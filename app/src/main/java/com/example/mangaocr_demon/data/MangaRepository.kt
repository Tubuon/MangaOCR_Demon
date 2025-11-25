// File: data/MangaRepository.kt
package com.example.mangaocr_demon.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository layer để xử lý logic data cho Manga/Chapter/Page
 */
class MangaRepository(
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val pageDao: PageDao
) {

    // =================== MANGA ===================
    suspend fun insertManga(manga: MangaEntity): Long {
        return withContext(Dispatchers.IO) {
            mangaDao.insert(manga)
        }
    }

    suspend fun updateManga(manga: MangaEntity) {
        withContext(Dispatchers.IO) {
            mangaDao.update(manga)
        }
    }

    suspend fun deleteManga(manga: MangaEntity) {
        withContext(Dispatchers.IO) {
            mangaDao.delete(manga)
        }
    }

    suspend fun getMangaByIdSync(id: Long): MangaEntity? {
        return withContext(Dispatchers.IO) {
            mangaDao.getAllMangaSync().firstOrNull { it.id == id }
        }
    }

    fun getAllMangaFlow() = mangaDao.getAllManga()
    suspend fun getAllMangaSync() = mangaDao.getAllMangaSync()
    suspend fun getTotalMangaCount() = mangaDao.getMangaCount()

    // =================== CHAPTER ===================
    suspend fun insertChapter(chapter: ChapterEntity): Long {
        return withContext(Dispatchers.IO) {
            chapterDao.insert(chapter)
        }
    }

    suspend fun updateChapter(chapter: ChapterEntity) {
        withContext(Dispatchers.IO) {
            chapterDao.update(chapter)
        }
    }

    suspend fun deleteChapter(chapter: ChapterEntity) {
        withContext(Dispatchers.IO) {
            chapterDao.delete(chapter)
        }
    }

    suspend fun getChapterById(chapterId: Long): ChapterEntity? {
        return withContext(Dispatchers.IO) {
            chapterDao.getChapterById(chapterId)
        }
    }

    // ✅ SỬA: đổi tên method cho khớp DAO
    suspend fun getChaptersByAlbumId(albumId: Long): List<ChapterEntity> {
        return withContext(Dispatchers.IO) {
            chapterDao.getChaptersByAlbumIdSync(albumId)
        }
    }

    fun getChaptersByAlbumIdFlow(albumId: Long) = chapterDao.getChaptersByAlbumIdFlow(albumId)

    suspend fun getLastChapter(albumId: Long) = withContext(Dispatchers.IO) {
        chapterDao.getLastChapter(albumId)
    }

    suspend fun getChapterCountForAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        chapterDao.getChapterCount(albumId)
    }

    suspend fun getTotalChapterCount() = withContext(Dispatchers.IO) {
        chapterDao.getTotalChapterCount()
    }

    // =================== PAGE ===================
    suspend fun insertPage(page: PageEntity): Long {
        return withContext(Dispatchers.IO) {
            pageDao.insert(page)
        }
    }

    suspend fun insertPages(pages: List<PageEntity>) {
        withContext(Dispatchers.IO) {
            pageDao.insertAll(pages)
        }
    }

    suspend fun updatePage(page: PageEntity) {
        withContext(Dispatchers.IO) {
            pageDao.update(page)
        }
    }

    suspend fun deletePage(page: PageEntity) {
        withContext(Dispatchers.IO) {
            pageDao.delete(page)
        }
    }

    suspend fun getPagesByChapterId(chapterId: Long): List<PageEntity> {
        return withContext(Dispatchers.IO) {
            pageDao.getPagesByChapterIdList(chapterId)
        }
    }

    suspend fun getPageById(pageId: Long): PageEntity? {
        return withContext(Dispatchers.IO) {
            pageDao.getPageById(pageId)
        }
    }

    // =================== OCR & TRANSLATION ===================
    suspend fun updatePageOcr(pageId: Long, ocrText: String) {
        withContext(Dispatchers.IO) {
            pageDao.updateOcrDataSimple(
                pageId = pageId,
                ocrDataJson = ocrText,
                isOcrProcessed = true
            )
        }
    }

    suspend fun updatePageTranslation(pageId: Long, ocrText: String, translatedText: String?) {
        withContext(Dispatchers.IO) {
            pageDao.updateTranslation(
                pageId = pageId,
                ocrText = ocrText,
                translatedText = translatedText ?: "",
                timestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun updatePageOcrData(
        pageId: Long,
        ocrDataJson: String?,
        isProcessed: Boolean,
        language: String
    ) {
        withContext(Dispatchers.IO) {
            pageDao.updateOcrData(
                pageId = pageId,
                ocrDataJson = ocrDataJson ?: "",
                isProcessed = isProcessed,
                language = language,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    // =================== STATISTICS ===================
    suspend fun getPageCountForChapter(chapterId: Long) = withContext(Dispatchers.IO) {
        pageDao.getPageCountByChapterId(chapterId)
    }

    suspend fun getTotalPageCount() = withContext(Dispatchers.IO) {
        pageDao.getTotalPageCount()
    }
}
