// File: data/AlbumRepository.kt
package com.example.mangaocr_demon.data

import kotlinx.coroutines.flow.Flow

class AlbumRepository(
    private val albumDao: AlbumDao,
    private val albumChapterDao: AlbumChapterDao
) {

    // Lấy tất cả album (Flow)
    fun getAllAlbums(): Flow<List<AlbumEntity>> = albumDao.getAllAlbums()

    // Thêm 1 chapter vào album
    suspend fun addChapterToAlbum(albumId: Long, chapterId: Long) {
        val crossRef = AlbumChapterEntity(albumId = albumId, chapterId = chapterId)
        albumChapterDao.addChapterToAlbum(crossRef)
    }

    // Xóa chapter khỏi album
    suspend fun removeChapterFromAlbum(albumId: Long, chapterId: Long) {
        albumChapterDao.removeChapterFromAlbumById(albumId, chapterId)
    }

    // Lấy danh sách album chứa chapter (dùng cho dialog)
    fun getAlbumsForChapter(chapterId: Long): Flow<List<AlbumEntity>> =
        albumChapterDao.getAlbumsContainingChapter(chapterId)

    // Lấy danh sách chapter trong album (Flow)
    fun getChaptersInAlbum(albumId: Long): Flow<List<ChapterEntity>> =
        albumChapterDao.getChaptersByAlbumId(albumId)

    // Lấy danh sách chapter trong album (Sync)
    suspend fun getChaptersInAlbumSync(albumId: Long): List<ChapterEntity> =
        albumChapterDao.getChaptersByAlbumIdSync(albumId)

    // Đếm tổng chapter trong album
    suspend fun getChapterCountInAlbum(albumId: Long): Int =
        albumChapterDao.getChapterCountInAlbum(albumId)

    // Xóa toàn bộ chapter trong album
    suspend fun clearAlbum(albumId: Long) = albumChapterDao.clearAlbum(albumId)

    // Thống kê tổng album
    suspend fun getAlbumCount(): Int = albumDao.getAlbumCount()
}
