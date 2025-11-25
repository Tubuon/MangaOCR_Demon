// File: data/SyncHistoryDao.kt
package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SyncHistoryEntity): Long

    @Update
    suspend fun update(history: SyncHistoryEntity)

    @Query("SELECT * FROM sync_history LIMIT 1")
    fun getSyncHistoryFlow(): Flow<SyncHistoryEntity?>

    @Query("SELECT * FROM sync_history LIMIT 1")
    suspend fun getSyncHistory(): SyncHistoryEntity?

    @Query("SELECT COUNT(*) FROM sync_history")
    suspend fun getBackupCount(): Int

    @Query("SELECT * FROM sync_history ORDER BY last_sync_time DESC LIMIT 1")
    suspend fun getLatestSuccessSync(): SyncHistoryEntity?

    @Transaction
    suspend fun updateSyncSuccess(timestamp: Long) {
        val currentHistory = getSyncHistory()
        if (currentHistory == null) {
            val newHistory = SyncHistoryEntity(
                lastSyncTime = timestamp,
                successfulSyncs = 1
            )
            insert(newHistory)
        } else {
            val updatedHistory = currentHistory.copy(
                lastSyncTime = timestamp,
                successfulSyncs = currentHistory.successfulSyncs + 1
            )
            update(updatedHistory)
        }
    }

    @Query("DELETE FROM sync_history")
    suspend fun clearHistory()
}
