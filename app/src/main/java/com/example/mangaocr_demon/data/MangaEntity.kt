// File: data/MangaEntity.kt
package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val title: String,

    val author: String? = null,

    val description: String? = null, // ✅ Thêm trường description

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // ✅ Thống nhất camelCase
)
