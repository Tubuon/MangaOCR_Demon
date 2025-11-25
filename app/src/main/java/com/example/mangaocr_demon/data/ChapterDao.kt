package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Delete
    suspend fun delete(chapter: ChapterEntity)

    // Lấy chapter theo albumId
    @Query("SELECT * FROM chapter WHERE album_id = :albumId ORDER BY number ASC")
    fun getChaptersByAlbumIdFlow(albumId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapter WHERE album_id = :albumId ORDER BY number ASC")
    suspend fun getChaptersByAlbumIdSync(albumId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapter WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): ChapterEntity?

    @Query("SELECT * FROM chapter WHERE id = :chapterId")
    fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?>

    @Query("DELETE FROM chapter WHERE album_id = :albumId")
    suspend fun deleteChaptersByAlbumId(albumId: Long)

    @Query("SELECT * FROM chapter")
    suspend fun getAllChapters(): List<ChapterEntity>

    @Query("SELECT COUNT(*) FROM chapter WHERE album_id = :albumId")
    suspend fun getChapterCount(albumId: Long): Int

    @Query("SELECT * FROM chapter WHERE album_id = :albumId ORDER BY number DESC LIMIT 1")
    suspend fun getLastChapter(albumId: Long): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapter")
    suspend fun getTotalChapterCount(): Int

    // ================= Backup / Restore =================
    @Query("SELECT * FROM chapter")
    suspend fun getAllChaptersForBackup(): List<ChapterEntity>

    @Query("DELETE FROM chapter")
    suspend fun clearChapters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChaptersForRestore(chapters: List<ChapterEntity>)
}
