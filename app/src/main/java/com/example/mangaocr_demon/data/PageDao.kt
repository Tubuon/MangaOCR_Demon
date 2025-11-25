package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)

    @Update
    suspend fun update(page: PageEntity)

    @Delete
    suspend fun delete(page: PageEntity)

    // Lấy tất cả pages của 1 chapter (Flow version)
    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getPagesByChapterIdFlow(chapterId: Long): Flow<List<PageEntity>>

    // Lấy tất cả pages của 1 chapter (suspend version)
    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    suspend fun getPagesByChapterIdList(chapterId: Long): List<PageEntity>

    @Query("SELECT * FROM page WHERE id = :pageId")
    suspend fun getPageById(pageId: Long): PageEntity?

    @Query("DELETE FROM page WHERE chapter_id = :chapterId")
    suspend fun deletePagesByChapterId(chapterId: Long)

    @Query("SELECT * FROM page")
    suspend fun getAllPages(): List<PageEntity>

    // Update bản dịch
    @Query("""
        UPDATE page 
        SET ocr_text = :ocrText, 
            translated_text = :translatedText,
            last_processed_at = :timestamp
        WHERE id = :pageId
    """)
    suspend fun updateTranslation(
        pageId: Long,
        ocrText: String?,
        translatedText: String?,
        timestamp: Long
    )

    // Update OCR JSON đơn giản
    @Query("""
        UPDATE page 
        SET ocr_data_json = :ocrDataJson, 
            is_ocr_processed = :isOcrProcessed
        WHERE id = :pageId
    """)
    suspend fun updateOcrDataSimple(
        pageId: Long,
        ocrDataJson: String?,
        isOcrProcessed: Boolean
    )

    // Method full cho OcrViewModel
    @Query("""
        UPDATE page 
        SET ocr_data_json = :ocrDataJson,
            is_ocr_processed = :isProcessed,
            ocr_language = :language,
            last_processed_at = :timestamp
        WHERE id = :pageId
    """)
    suspend fun updateOcrData(
        pageId: Long,
        ocrDataJson: String,
        isProcessed: Boolean,
        language: String,
        timestamp: Long
    )

    @Query("SELECT COUNT(*) FROM page WHERE chapter_id = :chapterId")
    suspend fun getPageCountByChapterId(chapterId: Long): Int

    @Query("SELECT COUNT(*) FROM page")
    suspend fun getTotalPageCount(): Int

    // ================= Backup / Restore =================
    @Query("SELECT * FROM page")
    suspend fun getAllPagesForBackup(): List<PageEntity>

    @Query("DELETE FROM page")
    suspend fun clearPages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPagesForRestore(pages: List<PageEntity>)
}
