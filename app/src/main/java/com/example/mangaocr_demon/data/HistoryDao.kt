// File: data/HistoryDao.kt
package com.example.mangaocr_demon.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HistoryDao {

    // Thêm 1 lịch sử OCR/Translate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    // Cập nhật 1 lịch sử
    @Update
    suspend fun update(history: HistoryEntity)

    // Xóa 1 lịch sử
    @Delete
    suspend fun delete(history: HistoryEntity)

    // Xóa tất cả lịch sử
    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    // Lấy tất cả lịch sử cho UI (LiveData)
    @Query("SELECT * FROM history ORDER BY id DESC")
    fun getAllHistory(): LiveData<List<HistoryEntity>>

    // Lấy tất cả lịch sử đồng bộ (suspend)
    @Query("SELECT * FROM history ORDER BY id DESC")
    suspend fun getAllHistorySync(): List<HistoryEntity>

    // Lấy lịch sử theo ID
    @Query("SELECT * FROM history WHERE id = :historyId LIMIT 1")
    suspend fun getHistoryById(historyId: Int): HistoryEntity?

    // Đếm tổng số lịch sử
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int

    // Lấy lịch sử mới nhất (limit)
    @Query("SELECT * FROM history ORDER BY id DESC LIMIT :limit")
    suspend fun getLatestHistory(limit: Int): List<HistoryEntity>

    // Tìm kiếm lịch sử theo OCR hoặc Text dịch
    @Query("""
        SELECT * FROM history
        WHERE ocrText LIKE '%' || :query || '%'
           OR translatedText LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    suspend fun searchHistory(query: String): List<HistoryEntity>
}
