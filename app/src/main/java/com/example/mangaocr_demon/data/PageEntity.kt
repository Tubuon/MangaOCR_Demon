package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapter_id"])]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "chapter_id") val chapterId: Long,
    // vị trí trang trong chapter, bắt đầu từ 0 hoặc 1 tùy bạn quy ước
    @ColumnInfo(name = "page_index") val pageIndex: Int,
    // uri tới file ảnh (content://... hoặc file://...)
    @ColumnInfo(name = "image_uri") val imageUri: String? = null,
    // text OCR gốc (nullable nếu chưa OCR)
    @ColumnInfo(name = "ocr_text") val ocrText: String? = null,
    @ColumnInfo(name = "translated_text") val translatedText: String? = null,
    @ColumnInfo(name = "pdf_uri") val pdfUri: String? = null,
    @ColumnInfo(name = "pdf_page_number") val pdfPageNumber: Int? = null, // For PDF pages
    @ColumnInfo(name = "page_type") val pageType: String = "IMAGE", // "IMAGE" or "PDF"
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
