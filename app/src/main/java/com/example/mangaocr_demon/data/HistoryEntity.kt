package com.example.mangaocr_demon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String?,         // ảnh OCR (URI)
    val ocrText: String,           // text OCR
    val translatedText: String,    // text dịch
    val timestamp: String          // thời gian
)
