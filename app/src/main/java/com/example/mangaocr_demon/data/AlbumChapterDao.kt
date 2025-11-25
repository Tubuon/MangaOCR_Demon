package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChapterToAlbum(albumChapter: AlbumChapterEntity)

    @Delete
    suspend fun removeChapterFromAlbum(albumChapter: AlbumChapterEntity)

    @Query("DELETE FROM album_chapters WHERE albumId = :albumId AND chapterId = :chapterId")
    suspend fun removeChapterFromAlbumById(albumId: Long, chapterId: Long)

    @Query("SELECT * FROM album_chapters WHERE albumId = :albumId AND chapterId = :chapterId LIMIT 1")
    suspend fun isChapterInAlbum(albumId: Long, chapterId: Long): AlbumChapterEntity?

    @Query("""
        SELECT c.* FROM chapter c
        INNER JOIN album_chapters ac ON c.id = ac.chapterId
        WHERE ac.albumId = :albumId
        ORDER BY c.number ASC
    """)
    fun getChaptersByAlbumId(albumId: Long): Flow<List<ChapterEntity>>

    @Query("""
        SELECT c.* FROM chapter c
        INNER JOIN album_chapters ac ON c.id = ac.chapterId
        WHERE ac.albumId = :albumId
        ORDER BY c.number ASC
    """)
    suspend fun getChaptersByAlbumIdSync(albumId: Long): List<ChapterEntity>

    @Query("""
        SELECT a.* FROM album a
        INNER JOIN album_chapters ac ON a.id = ac.albumId
        WHERE ac.chapterId = :chapterId
        ORDER BY a.created_at DESC
    """)
    fun getAlbumsContainingChapter(chapterId: Long): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album_chapters")
    suspend fun getAllAlbumChaptersSync(): List<AlbumChapterEntity>

    @Query("SELECT COUNT(*) FROM album_chapters WHERE albumId = :albumId")
    suspend fun getChapterCountInAlbum(albumId: Long): Int

    @Query("DELETE FROM album_chapters WHERE albumId = :albumId")
    suspend fun clearAlbum(albumId: Long)
}
