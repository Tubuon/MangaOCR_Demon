package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "name")
    val name: String = title, // Alias cho backward compatibility

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "color")
    val color: String = "#4CAF50", // Default green

    @ColumnInfo(name = "created_at")
    val created_at: Long = System.currentTimeMillis()
) {
    constructor(
        title: String,
        description: String? = null,
        color: String = "#4CAF50",
        created_at: Long = System.currentTimeMillis()
    ) : this(
        id = 0,
        title = title,
        name = title,
        description = description,
        color = color,
        created_at = created_at
    )
}
