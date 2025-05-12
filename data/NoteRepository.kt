package com.example.external.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class NoteRepository(context: Context) {
    private val databaseHelper = NoteDatabaseHelper(context)

    fun getAllNotes(): Flow<List<Note>> = callbackFlow {
        // Initial emission
        trySend(databaseHelper.getAllNotes())

        // Create a database observer
        val observer = object {
            fun onDatabaseChanged() {
                trySend(databaseHelper.getAllNotes())
            }
        }

        // Register observer (dummy implementation since SQLite doesn't have built-in observers)
        databaseHelper.registerObserver(observer)

        awaitClose {
            // Cleanup when the Flow is cancelled
            databaseHelper.unregisterObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    fun getNotesByCategory(category: String): Flow<List<Note>> = callbackFlow {
        trySend(databaseHelper.getNotesByCategory(category))
        
        val observer = object {
            fun onDatabaseChanged() {
                trySend(databaseHelper.getNotesByCategory(category))
            }
        }
        
        databaseHelper.registerObserver(observer)
        
        awaitClose {
            databaseHelper.unregisterObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    fun getNotesBySubcategory(category: String, subcategory: String): Flow<List<Note>> = callbackFlow {
        trySend(databaseHelper.getNotesBySubcategory(category, subcategory))
        
        val observer = object {
            fun onDatabaseChanged() {
                trySend(databaseHelper.getNotesBySubcategory(category, subcategory))
            }
        }
        
        databaseHelper.registerObserver(observer)
        
        awaitClose {
            databaseHelper.unregisterObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    fun getAllCategories(): Flow<List<String>> = callbackFlow {
        trySend(databaseHelper.getAllCategories())
        
        val observer = object {
            fun onDatabaseChanged() {
                trySend(databaseHelper.getAllCategories())
            }
        }
        
        databaseHelper.registerObserver(observer)
        
        awaitClose {
            databaseHelper.unregisterObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    fun getSubcategoriesForCategory(category: String): Flow<List<String>> = callbackFlow {
        trySend(databaseHelper.getSubcategoriesForCategory(category))
        
        val observer = object {
            fun onDatabaseChanged() {
                trySend(databaseHelper.getSubcategoriesForCategory(category))
            }
        }
        
        databaseHelper.registerObserver(observer)
        
        awaitClose {
            databaseHelper.unregisterObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertNote(note: Note) = withContext(Dispatchers.IO) {
        databaseHelper.insertNote(note)
        databaseHelper.notifyDatabaseChanged()
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        databaseHelper.updateNote(note)
        databaseHelper.notifyDatabaseChanged()
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        databaseHelper.deleteNote(note.id)
        databaseHelper.notifyDatabaseChanged()
    }

    suspend fun deleteNoteById(noteId: Long) = withContext(Dispatchers.IO) {
        databaseHelper.deleteNote(noteId)
        databaseHelper.notifyDatabaseChanged()
    }
} 