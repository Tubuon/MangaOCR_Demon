// File: data/HistoryEntity.kt
package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String?,
    val ocrText: String,
    val translatedText: String,
    val timestamp: String
)
