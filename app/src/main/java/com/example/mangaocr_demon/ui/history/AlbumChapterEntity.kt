package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_chapter",
    primaryKeys = ["album_id", "chapter_id"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["album_id"]), Index(value = ["chapter_id"])]
)
data class AlbumChapterEntity(
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "chapter_id") val chapterId: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)