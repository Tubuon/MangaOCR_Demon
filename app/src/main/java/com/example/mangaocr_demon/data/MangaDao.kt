package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manga: MangaEntity): Long

    @Update
    suspend fun update(manga: MangaEntity)

    @Delete
    suspend fun delete(manga: MangaEntity)

    @Query("SELECT * FROM manga ORDER BY created_at DESC")
    fun getAllManga(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id LIMIT 1")
    fun getMangaById(id: Long): Flow<MangaEntity?>

    @Query("SELECT * FROM manga ORDER BY created_at DESC")
    suspend fun getAllMangaSync(): List<MangaEntity>

    @Query("SELECT COUNT(*) FROM manga")
    suspend fun getMangaCount(): Int

    // ================= Backup / Restore =================

    @Query("SELECT * FROM manga")
    suspend fun getAllMangaForBackup(): List<MangaEntity>

    @Query("DELETE FROM manga")
    suspend fun clearManga()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllManga(list: List<MangaEntity>)
}
