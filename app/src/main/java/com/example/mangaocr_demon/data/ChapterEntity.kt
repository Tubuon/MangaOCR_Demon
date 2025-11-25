package com.example.mangaocr_demon.data

import androidx.room.*

@Entity(
    tableName = "chapter",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class, // ✅ FIXED: Reference Manga, not Album
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["album_id"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "album_id") val albumId: Long, // Column name là album_id nhưng reference đến Manga
    val number: Int,
    val title: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)