// File: ui/viewmodel/ChapterReaderViewModel.kt
package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.HistoryDao
import com.example.mangaocr_demon.data.HistoryEntity
import com.example.mangaocr_demon.data.MangaRepository
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.ml.GeminiTranslator
import com.example.mangaocr_demon.ml.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PageState(
    val pages: List<PageEntity> = emptyList(),
    val currentPage: PageEntity? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class ChapterReaderViewModel(
    private val mangaRepo: MangaRepository,
    private val historyDao: HistoryDao,
    private val ocrEngine: OcrEngine,
    private val translator: GeminiTranslator,
    private val chapterId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(PageState())
    val state: StateFlow<PageState> = _state

    init {
        loadPages()
    }

    /** Load tất cả pages của chapter hiện tại */
    private fun loadPages() = viewModelScope.launch(Dispatchers.IO) {
        val pages = mangaRepo.getPagesByChapterId(chapterId)
        _state.value = _state.value.copy(pages = pages)
    }

    /** Load 1 page cụ thể */
    fun loadPage(pageId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val page = mangaRepo.getPageById(pageId)
        _state.value = _state.value.copy(currentPage = page)
    }

    /** ✅ Xử lý OCR và Dịch cho 1 page */
    suspend fun processPageOcrAndTranslate(page: PageEntity) {
        _state.value = _state.value.copy(isProcessing = true, errorMessage = null)

        try {
            // Bước 1: OCR
            val ocrText = ocrEngine.recognize(page)

            if (ocrText.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    errorMessage = "Không tìm thấy text trong ảnh"
                )
                return
            }

            // Bước 2: Dịch
            val translatedText = translator.translate(ocrText, "vi")

            // Bước 3: Update database
            mangaRepo.updatePageTranslation(page.id, ocrText, translatedText)

            // Bước 4: Lưu vào History
            saveHistory(page.imageUri, ocrText, translatedText)

            // Bước 5: Refresh pages
            loadPages()

            _state.value = _state.value.copy(isProcessing = false)

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isProcessing = false,
                errorMessage = "Lỗi: ${e.message}"
            )
        }
    }

    /** Xử lý OCR cho 1 page (không dịch) */
    fun processOcr(pageId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val page = mangaRepo.getPageById(pageId) ?: return@launch
        val ocrText = ocrEngine.recognize(page)
        mangaRepo.updatePageOcr(pageId, ocrText ?: "")
        loadPages()
    }

    /** Dịch OCR Text sang ngôn ngữ khác */
    fun translatePage(pageId: Long, targetLang: String) = viewModelScope.launch(Dispatchers.IO) {
        val page = mangaRepo.getPageById(pageId) ?: return@launch
        val originalText = page.ocrText ?: ""
        val translatedText = translator.translate(originalText, targetLang)
        mangaRepo.updatePageTranslation(pageId, originalText, translatedText)
        loadPages()
    }

    /** Ghi lại lịch sử thao tác OCR/Translate */
    private suspend fun saveHistory(imageUri: String?, ocrText: String, translatedText: String?) {
        val history = HistoryEntity(
            imageUri = imageUri,
            ocrText = ocrText,
            translatedText = translatedText ?: "",
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        historyDao.insert(history)
    }

    /** Ghi lại lịch sử (public method) */
    fun logHistory(ocrText: String, translatedText: String) = viewModelScope.launch(Dispatchers.IO) {
        val history = HistoryEntity(
            imageUri = null,
            ocrText = ocrText,
            translatedText = translatedText,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        historyDao.insert(history)
    }
}