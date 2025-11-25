// File: data/SyncRepository.kt
package com.example.mangaocr_demon.data

class SyncRepository(
    private val syncHistoryDao: SyncHistoryDao
) {
    // Ghi log một sự kiện đồng bộ
    suspend fun logSyncEvent(
        type: String,
        status: String,
        fileId: String? = null,
        message: String? = null
    ) {
        val entry = SyncHistoryEntity(
            type = type,
            status = status,
            fileId = fileId,
            message = message,
            lastSyncTime = System.currentTimeMillis(),
            successfulSyncs = 1
        )
        syncHistoryDao.insert(entry)
    }

    // Lấy số lần backup
    suspend fun getBackupCount(): Int = syncHistoryDao.getBackupCount()

    // Lấy thời gian đồng bộ cuối
    suspend fun getLastSyncTime(): Long {
        return syncHistoryDao.getLatestSuccessSync()?.lastSyncTime ?: 0
    }
}
