package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.mangaocr_demon.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mangaDao = db.mangaDao()
    private val chapterDao = db.chapterDao()
    private val pageDao = db.pageDao()

    val mangaList: LiveData<List<MangaEntity>> = mangaDao.getAllManga().asLiveData()

    /**
     * Thêm manga từ danh sách ảnh
     * Tự động chia thành chapters (25 pages/chapter)
     */
    fun addMangaWithImages(manga: MangaEntity, imageUris: List<String>) {
        // ✅ FIXED: Dùng viewModelScope.launch đúng cách
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // ✅ Insert manga
                    val mangaId = mangaDao.insert(manga)

                    var chapterNumber = 1
                    var chapterId = chapterDao.insert(
                        ChapterEntity(
                            albumId = mangaId, // ✅ FIXED: dùng albumId như trong entity
                            number = chapterNumber,
                            title = "Chapter $chapterNumber"
                        )
                    )

                    var pageIndex = 0

                    imageUris.forEach { uri ->
                        // Mỗi 25 page tạo chapter mới
                        if (pageIndex >= 25) {
                            chapterNumber++
                            pageIndex = 0
                            chapterId = chapterDao.insert(
                                ChapterEntity(
                                    albumId = mangaId, // ✅ FIXED: dùng albumId
                                    number = chapterNumber,
                                    title = "Chapter $chapterNumber"
                                )
                            )
                        }

                        val page = PageEntity(
                            chapterId = chapterId,
                            pageIndex = pageIndex,
                            imageUri = uri,
                            pageType = "IMAGE"
                        )
                        pageDao.insert(page)
                        pageIndex++
                    }

                    android.util.Log.d("HomeViewModel", "✅ Added manga with ${imageUris.size} images")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error adding images", e)
            }
        }
    }

    /**
     * Thêm manga từ PDF
     * Tự động extract pages và chia chapters
     */
    fun addMangaFromPdf(manga: MangaEntity, pdfUri: String) {
        // ✅ FIXED: Dùng viewModelScope.launch đúng cách
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val totalPages = getPdfPageCountInternal(pdfUri)
                    if (totalPages <= 0) {
                        android.util.Log.e("HomeViewModel", "PDF has 0 pages")
                        return@withContext
                    }

                    val mangaId = mangaDao.insert(manga)
                    val pagesPerChapter = 25
                    val totalChapters = (totalPages + pagesPerChapter - 1) / pagesPerChapter

                    for (chapterIndex in 0 until totalChapters) {
                        val chapterEntity = ChapterEntity(
                            albumId = mangaId, // ✅ FIXED: dùng albumId
                            number = chapterIndex + 1,
                            title = "Chapter ${chapterIndex + 1}"
                        )
                        val chapterId = chapterDao.insert(chapterEntity)

                        val startPage = chapterIndex * pagesPerChapter
                        val endPage = minOf(startPage + pagesPerChapter, totalPages)

                        for (pdfPageIndex in startPage until endPage) {
                            val pageEntity = PageEntity(
                                chapterId = chapterId,
                                pageIndex = pdfPageIndex - startPage,
                                pdfUri = pdfUri,
                                pdfPageNumber = pdfPageIndex,
                                pageType = "PDF"
                            )
                            pageDao.insert(pageEntity)
                        }
                    }

                    android.util.Log.d("HomeViewModel", "✅ Added PDF with $totalPages pages, $totalChapters chapters")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error processing PDF", e)
            }
        }
    }

    /**
     * ✅ FIXED: Đổi tên để không conflict với suspend modifier
     * Internal helper function để đếm số page trong PDF
     */
    private fun getPdfPageCountInternal(pdfUri: String): Int {
        var pageCount = 0
        val uri = Uri.parse(pdfUri)

        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                    pageCount = renderer.pageCount
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error reading PDF page count", e)
            pageCount = 0
        }

        return pageCount
    }
}