package com.example.mangaocr_demon.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_chapters",
    primaryKeys = ["albumId", "chapterId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["chapterId"])
    ]
)
data class AlbumChapterEntity(
    val albumId: Long,
    val chapterId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
