// File: data/SyncHistoryEntity.kt
package com.example.mangaocr_demon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long = 0,
    @ColumnInfo(name = "successful_syncs") val successfulSyncs: Int = 0,

    // Các field bổ sung
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "status") val status: String? = null,
    @ColumnInfo(name = "file_id") val fileId: String? = null,
    @ColumnInfo(name = "message") val message: String? = null
)
