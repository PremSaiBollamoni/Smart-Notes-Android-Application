package com.example.external.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.external.data.Note
import com.example.external.data.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    fun getAllNotes(): Flow<List<Note>> = repository.getAllNotes()

    fun getNotesByCategory(category: String): Flow<List<Note>> {
        return repository.getNotesByCategory(category)
    }

    fun getNotesBySubcategory(category: String, subcategory: String): Flow<List<Note>> {
        return repository.getNotesBySubcategory(category, subcategory)
    }

    fun getAllCategories(): Flow<List<String>> {
        return repository.getAllCategories()
    }

    fun getSubcategoriesForCategory(category: String): Flow<List<String>> {
        return repository.getSubcategoriesForCategory(category)
    }

    fun insertNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun deleteNoteById(noteId: Long) = viewModelScope.launch {
        repository.deleteNoteById(noteId)
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 