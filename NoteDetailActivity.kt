package com.example.external

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.external.data.Note
import com.example.external.databinding.ActivityNoteDetailBinding
import java.util.Random

class NoteDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailBinding
    private val random = Random(System.currentTimeMillis())

    companion object {
        const val EXTRA_NOTE = "extra_note"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // For API level 33+, use getParcelableExtra(String, Class)
        // For compatibility with older APIs, use the deprecated method
        val note = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_NOTE, Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Note>(EXTRA_NOTE)
        }

        if (note == null) {
            finish()
            return
        }

        displayNote(note)
    }

    private fun displayNote(note: Note) {
        // Set the title
        supportActionBar?.title = note.title

        // Display category
        binding.categoryChip.text = note.category
        binding.categoryChip.setChipBackgroundColorResource(getCategoryColor(note.category))
        binding.categoryChip.setTextColor(Color.WHITE)

        // Display subcategory if available
        if (note.subcategory.isNotBlank()) {
            binding.subcategoryChip.visibility = android.view.View.VISIBLE
            binding.subcategoryChip.text = note.subcategory
        } else {
            binding.subcategoryChip.visibility = android.view.View.GONE
        }

        // Display summary
        if (note.summary.isNotBlank()) {
            binding.summaryText.text = note.summary
        } else {
            binding.summaryTitle.visibility = android.view.View.GONE
            binding.summaryText.visibility = android.view.View.GONE
        }

        // Display content
        binding.contentText.text = note.content

        // Display image if available
        if (note.imagePath.isNotBlank()) {
            try {
                val bitmap = BitmapFactory.decodeFile(note.imagePath)
                if (bitmap != null) {
                    binding.noteImage.setImageBitmap(bitmap)
                    binding.noteImage.visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                // If there's an error loading the image, just hide the ImageView
                binding.noteImage.visibility = android.view.View.GONE
            }
        } else {
            binding.noteImage.visibility = android.view.View.GONE
        }
    }

    private fun getCategoryColor(category: String): Int {
        // Return a consistent color for each category
        return when (category) {
            "Work" -> R.color.work_category
            "Personal" -> R.color.personal_category
            "Study" -> R.color.study_category
            "Meeting" -> R.color.meeting_category
            "Task" -> R.color.task_category
            "Research" -> R.color.research_category
            "Creative" -> R.color.creative_category
            "Finance" -> R.color.finance_category
            "Health" -> R.color.health_category
            "Travel" -> R.color.travel_category
            "Recipe" -> R.color.recipe_category
            "Technical" -> R.color.technical_category
            else -> R.color.other_category
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
} 