package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Insert
    suspend fun insert(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)

    @Update
    suspend fun update(page: PageEntity)

    @Delete
    suspend fun delete(page: PageEntity)

    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getPagesForChapter(chapterId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM page WHERE id = :id LIMIT 1")
    fun getPageById(id: Long): Flow<PageEntity?>

    @Query("DELETE FROM page WHERE chapter_id = :chapterId")
    suspend fun deletePagesForChapter(chapterId: Long)

    @Query("SELECT * FROM page WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getPagesByChapterId(chapterId: Long): Flow<List<PageEntity>>
}
