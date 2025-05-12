package com.example.external.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class NoteDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val observers = mutableListOf<Any>()

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_NOTES = "notes"

        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_SUBCATEGORY = "subcategory"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_IMAGE_PATH = "image_path"
        private const val COLUMN_SUMMARY = "summary"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_SUBCATEGORY TEXT DEFAULT '',
                $COLUMN_TAGS TEXT DEFAULT '[]',
                $COLUMN_IMAGE_PATH TEXT,
                $COLUMN_SUMMARY TEXT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Migrating from STRING to INTEGER for IDs
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            onCreate(db)
        } else if (oldVersion < 3) {
            // Adding subcategory and tags columns
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_SUBCATEGORY TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_TAGS TEXT DEFAULT '[]'")
        }
    }

    fun registerObserver(observer: Any) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    fun unregisterObserver(observer: Any) {
        observers.remove(observer)
    }

    fun notifyDatabaseChanged() {
        observers.forEach { observer ->
            try {
                val method = observer.javaClass.getMethod("onDatabaseChanged")
                method.invoke(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun tagsToJson(tags: List<String>): String {
        val jsonArray = JSONArray()
        tags.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    private fun jsonToTags(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val note = Note(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
                    content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)),
                    category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    subcategory = getString(getColumnIndexOrThrow(COLUMN_SUBCATEGORY)),
                    imagePath = getString(getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                    summary = getString(getColumnIndexOrThrow(COLUMN_SUMMARY)),
                    timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    tags = jsonToTags(getString(getColumnIndexOrThrow(COLUMN_TAGS)))
                )
                notes.add(note)
            }
        }
        cursor.close()
        return notes
    }

    fun getNotesByCategory(category: String): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val selection = "$COLUMN_CATEGORY = ?"
        val selectionArgs = arrayOf(category)
        val cursor = db.query(
            TABLE_NOTES,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val note = Note(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
                    content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)),
                    category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    subcategory = getString(getColumnIndexOrThrow(COLUMN_SUBCATEGORY)),
                    imagePath = getString(getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                    summary = getString(getColumnIndexOrThrow(COLUMN_SUMMARY)),
                    timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    tags = jsonToTags(getString(getColumnIndexOrThrow(COLUMN_TAGS)))
                )
                notes.add(note)
            }
        }
        cursor.close()
        return notes
    }

    fun getNotesBySubcategory(category: String, subcategory: String): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val selection = "$COLUMN_CATEGORY = ? AND $COLUMN_SUBCATEGORY = ?"
        val selectionArgs = arrayOf(category, subcategory)
        val cursor = db.query(
            TABLE_NOTES,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val note = Note(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
                    content = getString(getColumnIndexOrThrow(COLUMN_CONTENT)),
                    category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    subcategory = getString(getColumnIndexOrThrow(COLUMN_SUBCATEGORY)),
                    imagePath = getString(getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                    summary = getString(getColumnIndexOrThrow(COLUMN_SUMMARY)),
                    timestamp = getLong(getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    tags = jsonToTags(getString(getColumnIndexOrThrow(COLUMN_TAGS)))
                )
                notes.add(note)
            }
        }
        cursor.close()
        return notes
    }

    fun getAllCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.query(
            true,
            TABLE_NOTES,
            arrayOf(COLUMN_CATEGORY),
            null,
            null,
            null,
            null,
            "$COLUMN_CATEGORY ASC",
            null
        )

        with(cursor) {
            while (moveToNext()) {
                categories.add(getString(getColumnIndexOrThrow(COLUMN_CATEGORY)))
            }
        }
        cursor.close()
        return categories
    }

    fun getSubcategoriesForCategory(category: String): List<String> {
        val subcategories = mutableListOf<String>()
        val db = this.readableDatabase
        val selection = "$COLUMN_CATEGORY = ?"
        val selectionArgs = arrayOf(category)
        val cursor = db.query(
            true,
            TABLE_NOTES,
            arrayOf(COLUMN_SUBCATEGORY),
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_SUBCATEGORY ASC",
            null
        )

        with(cursor) {
            while (moveToNext()) {
                val subcategory = getString(getColumnIndexOrThrow(COLUMN_SUBCATEGORY))
                if (subcategory.isNotBlank()) {
                    subcategories.add(subcategory)
                }
            }
        }
        cursor.close()
        return subcategories
    }

    fun insertNote(note: Note): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            // Don't include ID for AUTOINCREMENT
            if (note.id > 0) {
                put(COLUMN_ID, note.id)
            }
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_CATEGORY, note.category)
            put(COLUMN_SUBCATEGORY, note.subcategory)
            put(COLUMN_TAGS, tagsToJson(note.tags))
            put(COLUMN_IMAGE_PATH, note.imagePath)
            put(COLUMN_SUMMARY, note.summary)
            put(COLUMN_TIMESTAMP, note.timestamp)
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    fun updateNote(note: Note): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_CATEGORY, note.category)
            put(COLUMN_SUBCATEGORY, note.subcategory)
            put(COLUMN_TAGS, tagsToJson(note.tags))
            put(COLUMN_IMAGE_PATH, note.imagePath)
            put(COLUMN_SUMMARY, note.summary)
            put(COLUMN_TIMESTAMP, note.timestamp)
        }
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(note.id.toString())
        return db.update(TABLE_NOTES, values, selection, selectionArgs)
    }

    fun deleteNote(noteId: Long): Int {
        val db = this.writableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(noteId.toString())
        return db.delete(TABLE_NOTES, selection, selectionArgs)
    }
} 