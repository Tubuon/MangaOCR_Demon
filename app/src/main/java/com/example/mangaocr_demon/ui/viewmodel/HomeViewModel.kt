package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.mangaocr_demon.data.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlinx.coroutines.withContext
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------- OCR + Translate (giữ nguyên) ------------------- //
    private val _ocrText = MutableLiveData<String>()
    val ocrText: LiveData<String> get() = _ocrText

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> get() = _translatedText

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> get() = _imageUri

    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.historyDao()

    fun setImageUri(uri: Uri) {
        _imageUri.postValue(uri)
    }

    fun runOCRAndTranslate(image: InputImage, imageUri: Uri?) {
        val recognizer = TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                _ocrText.postValue(text)

                if (text.isNotEmpty()) {
                    detectLanguageAndTranslate(text, imageUri)
                }
            }
            .addOnFailureListener { e ->
                _ocrText.postValue("OCR lỗi: ${e.message}")
            }
    }

    private fun detectLanguageAndTranslate(text: String, imageUri: Uri?) {
        val languageIdentifier = LanguageIdentification.getClient()

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { langCode ->
                val sourceLang = when (langCode) {
                    "ja" -> TranslateLanguage.JAPANESE
                    "zh" -> TranslateLanguage.CHINESE
                    "en" -> TranslateLanguage.ENGLISH
                    else -> TranslateLanguage.ENGLISH
                }
                translateText(text, sourceLang, TranslateLanguage.VIETNAMESE, imageUri)
            }
            .addOnFailureListener {
                _translatedText.postValue("Không xác định được ngôn ngữ.")
            }
    }

    private fun translateText(text: String, sourceLang: String, targetLang: String, imageUri: Uri?) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        _translatedText.postValue(translated)

                        // Lưu lịch sử vào DB
                        val history = HistoryEntity(
                            imageUri = imageUri?.toString(),
                            ocrText = text,
                            translatedText = translated,
                            timestamp = System.currentTimeMillis()
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            historyDao.insert(history)
                        }
                    }
                    .addOnFailureListener { e ->
                        _translatedText.postValue("Dịch lỗi: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _translatedText.postValue("Không tải được model dịch: ${e.message}")
            }
    }

    // ------------------- Manga / Chapter / Page ------------------- //
    private val mangaDao = db.mangaDao()
    private val chapterDao = db.chapterDao()
    private val pageDao = db.pageDao()

    // Chuyển Flow -> LiveData để fragment dễ quan sát
    val mangaList: LiveData<List<MangaEntity>> = mangaDao.getAllManga().asLiveData()

    /**
     * Thêm Manga từ danh sách URI (String).
     * Chia chapter nếu cần (tại đây đơn giản tạo 1 chapter và thêm tất cả ảnh;
     * nếu muốn auto-split 25 ảnh/chap hãy tinh chỉnh bên dưới).
     */
    fun addMangaWithImages(manga: MangaEntity, imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val mangaId = mangaDao.insert(manga) // trả về id (Long)
            // Tạo chapter đầu tiên
            var chapterNumber = 1
            var chapterId = chapterDao.insert(ChapterEntity(mangaId = mangaId, number = chapterNumber, title = "Chapter $chapterNumber"))

            var pageIndex = 0
            imageUris.forEach { uri ->
                // nếu >=25 thì tạo chap mới
                if (pageIndex >= 25) {
                    chapterNumber++
                    pageIndex = 0
                    chapterId = chapterDao.insert(ChapterEntity(mangaId = mangaId, number = chapterNumber, title = "Chapter $chapterNumber"))
                }

                val page = PageEntity(
                    chapterId = chapterId,
                    pageIndex = pageIndex,
                    imageUri = uri
                )
                pageDao.insert(page)
                pageIndex++
            }
        }
    }


    private suspend fun getPdfPageCount(pdfUri: String): Int {
        return withContext(Dispatchers.IO) {
            var pageCount = 0
            val uri = Uri.parse(pdfUri)

            try {
                getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                        android.util.Log.d("HomeViewModel", "PDF has $pageCount pages")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error reading PDF page count", e)
                pageCount = 1 // Default fallback
            }

            pageCount
        }
    }
    /**
     * Thêm Manga từ PDF — hiện tại chỉ lưu URI của PDF làm 1 page.
     * Bạn có thể mở rộng: render PDF -> tạo nhiều PageEntity từ mỗi trang.
     */
    fun addMangaFromPdf(manga: MangaEntity, pdfUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("HomeViewModel", "Starting PDF import: $pdfUri")

                // Get total pages in PDF
                val totalPages = getPdfPageCount(pdfUri)

                if (totalPages <= 0) {
                    android.util.Log.w("HomeViewModel", "PDF has no readable pages")
                    return@launch
                }

                // Insert manga
                val mangaId = mangaDao.insert(manga)
                android.util.Log.d("HomeViewModel", "Created manga with ID: $mangaId")

                // Create chapters (25 PDF pages per chapter)
                val pagesPerChapter = 25
                val totalChapters = (totalPages + pagesPerChapter - 1) / pagesPerChapter

                android.util.Log.d("HomeViewModel", "Will create $totalChapters chapters from $totalPages pages")

                for (chapterIndex in 0 until totalChapters) {
                    val chapterEntity = ChapterEntity(
                        mangaId = mangaId,
                        number = chapterIndex + 1,
                        title = "Chapter ${chapterIndex + 1}"
                    )
                    val chapterId = chapterDao.insert(chapterEntity)

                    // Calculate page range for this chapter
                    val startPage = chapterIndex * pagesPerChapter
                    val endPage = minOf(startPage + pagesPerChapter, totalPages)

                    android.util.Log.d("HomeViewModel", "Chapter ${chapterIndex + 1}: pages $startPage to ${endPage - 1}")

                    // Create PageEntity for each PDF page in this chapter
                    for (pdfPageIndex in startPage until endPage) {
                        val pageEntity = PageEntity(
                            chapterId = chapterId,
                            pageIndex = pdfPageIndex - startPage, // Index within chapter (0-24)
                            pdfUri = pdfUri,
                            pdfPageNumber = pdfPageIndex, // Which page in PDF (0-based)
                            pageType = "PDF"
                        )
                        pageDao.insert(pageEntity)
                    }
                }

                android.util.Log.d("HomeViewModel", "Successfully created manga with $totalChapters chapters")

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error processing PDF: ${e.message}", e)
            }
        }
    }
}
