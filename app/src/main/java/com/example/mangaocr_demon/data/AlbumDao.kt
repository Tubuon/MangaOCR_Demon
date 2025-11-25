package com.example.mangaocr_demon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity): Long

    @Update
    suspend fun update(album: AlbumEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Query("SELECT * FROM album ORDER BY created_at DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album ORDER BY created_at DESC")
    suspend fun getAllAlbumsSync(): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE id = :albumId LIMIT 1")
    suspend fun getAlbumById(albumId: Long): AlbumEntity?

    @Query("SELECT * FROM album WHERE id = :albumId LIMIT 1")
    fun getAlbumByIdFlow(albumId: Long): Flow<AlbumEntity?>

    @Query("SELECT COUNT(*) FROM album")
    suspend fun getAlbumCount(): Int
}
