package com.example.external

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.external.data.Note
import com.example.external.data.NoteRepository
import com.example.external.databinding.ActivityMainBinding
import com.example.external.ui.CategoryAdapter
import com.example.external.ui.NoteAdapter
import com.example.external.ui.NoteViewModel
import com.example.external.ui.NoteViewModelFactory
import com.example.external.ui.SubcategoryAdapter
import com.example.external.utils.AIUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var subcategoryAdapter: SubcategoryAdapter
    private lateinit var aiUtils: AIUtils

    private val currentCategoryFlow = MutableStateFlow("All")
    private val currentSubcategoryFlow = MutableStateFlow("")
    private val allCategories = mutableSetOf<String>()
    private val allSubcategories = mutableSetOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { processImage(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val repository = NoteRepository(this)
        viewModel = ViewModelProvider(this, NoteViewModelFactory(repository))
            .get(NoteViewModel::class.java)
        aiUtils = AIUtils(this)

        setupCategoryList()
        setupSubcategoryList()
        setupNoteList()
        setupClickListeners()
        observeData()
    }

    private fun setupCategoryList() {
        categoryAdapter = CategoryAdapter { category ->
            currentCategoryFlow.value = category
            currentSubcategoryFlow.value = "" // Reset subcategory when changing category
            binding.selectedCategoryText.text = if (category == "All") {
                "All Notes"
            } else {
                "Category: $category"
            }
            
            // Update subcategory visibility
            if (category != "All") {
                loadSubcategories(category)
            } else {
                binding.subcategoriesRecyclerView.isVisible = false
            }
        }

        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Start with "All" category
        allCategories.add("All")
        categoryAdapter.submitList(allCategories.toList())
    }

    private fun setupSubcategoryList() {
        subcategoryAdapter = SubcategoryAdapter { subcategory ->
            currentSubcategoryFlow.value = subcategory
        }

        binding.subcategoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = subcategoryAdapter
        }
    }

    private fun loadSubcategories(category: String) {
        lifecycleScope.launch {
            viewModel.getSubcategoriesForCategory(category).collect { subcategories ->
                if (subcategories.isEmpty()) {
                    binding.subcategoriesRecyclerView.isVisible = false
                } else {
                    binding.subcategoriesRecyclerView.isVisible = true
                    subcategoryAdapter.submitList(subcategories)
                }
            }
        }
    }

    private fun setupNoteList() {
        noteAdapter = NoteAdapter(
            onNoteClick = { note ->
                openNoteDetail(note)
            },
            onNoteLongClick = { note ->
                showDeleteDialog(note)
            }
        )

        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNote.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                viewModel.getAllNotes(),
                currentCategoryFlow,
                currentSubcategoryFlow
            ) { notes, currentCategory, currentSubcategory ->
                // Extract all categories from notes
                allCategories.clear()
                allCategories.add("All") // Always include "All" option
                notes.forEach { note ->
                    if (note.category.isNotBlank()) {
                        allCategories.add(note.category)
                    }
                }
                categoryAdapter.submitList(allCategories.toList().sorted())

                // Filter notes by selected category and subcategory
                when {
                    currentCategory == "All" -> notes
                    currentSubcategory.isBlank() -> notes.filter { it.category == currentCategory }
                    else -> notes.filter { it.category == currentCategory && it.subcategory == currentSubcategory }
                }
            }.collect { filteredNotes ->
                noteAdapter.submitList(filteredNotes)
                
                // Show/hide empty view
                binding.emptyView.visibility = if (filteredNotes.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun processImage(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                // Show processing message
                Toast.makeText(
                    this@MainActivity,
                    "Analyzing image...",
                    Toast.LENGTH_SHORT
                ).show()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val imagePath = aiUtils.saveImageToInternalStorage(bitmap, "note_$timestamp.jpg")
                
                // Analyze image with Gemini
                val analysis = aiUtils.analyzeContent(bitmap)

                // Extract a subcategory if possible (from tags or content)
                val subcategory = if (analysis.suggestedTags.isNotEmpty()) {
                    analysis.suggestedTags.firstOrNull() ?: ""
                } else {
                    ""
                }

                // Create note with enhanced information
                val note = Note(
                    title = "Note from ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date())}",
                    content = buildString {
                        append(analysis.imageDescription)
                        append("\n\n")
                        
                        if (analysis.textContent.isNotBlank()) {
                            append("Detected Text:\n")
                            append(analysis.textContent)
                            append("\n\n")
                        }
                        
                        if (analysis.detectedObjects.isNotEmpty()) {
                            append("Detected Objects:\n")
                            append(analysis.detectedObjects.joinToString(", "))
                            append("\n\n")
                        }
                        
                        append("Summary:\n")
                        append(analysis.summary)
                    },
                    category = analysis.category,
                    subcategory = subcategory,
                    imagePath = imagePath,
                    summary = analysis.summary,
                    tags = analysis.suggestedTags
                )

                viewModel.insertNote(note)

                // Show success message with category and confidence
                val confidencePercentage = (analysis.confidence * 100).toInt()
                Toast.makeText(
                    this@MainActivity,
                    "Note created and categorized as: ${analysis.category} ($confidencePercentage% confidence)",
                    Toast.LENGTH_LONG
                ).show()
                
                // Switch to the new note's category
                currentCategoryFlow.value = analysis.category
                currentSubcategoryFlow.value = subcategory
                loadSubcategories(analysis.category)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error processing image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openNoteDetail(note: Note) {
        val intent = Intent(this, NoteDetailActivity::class.java)
        intent.putExtra(NoteDetailActivity.EXTRA_NOTE, note)
        startActivity(intent)
    }

    private fun showDeleteDialog(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}