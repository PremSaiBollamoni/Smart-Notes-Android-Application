package com.example.external.data

import android.os.Parcel
import android.os.Parcelable

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Uncategorized",
    val subcategory: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "Uncategorized",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeString(imagePath)
        parcel.writeLong(timestamp)
        parcel.writeString(category)
        parcel.writeString(subcategory)
        parcel.writeString(summary)
        parcel.writeStringList(tags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note {
            return Note(parcel)
        }

        override fun newArray(size: Int): Array<Note?> {
            return arrayOfNulls(size)
        }
    }
} 