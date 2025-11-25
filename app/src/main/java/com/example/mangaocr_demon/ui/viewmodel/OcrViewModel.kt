// File: ui/viewmodel/OcrViewModel.kt
package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.PageEntity
import com.example.mangaocr_demon.data.model.OcrData
import com.example.mangaocr_demon.ml.GeminiTranslator
import com.example.mangaocr_demon.ml.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrEngine = OcrEngine(application)
    private val translator = GeminiTranslator(application)
    private val pageDao = AppDatabase.getDatabase(application).pageDao()

    private val _ocrProgress = MutableLiveData<OcrProgress>()
    val ocrProgress: LiveData<OcrProgress> = _ocrProgress

    private val _translationProgress = MutableLiveData<TranslationProgress>()
    val translationProgress: LiveData<TranslationProgress> = _translationProgress

    private val _ocrError = MutableLiveData<String?>()
    val ocrError: LiveData<String?> = _ocrError

    /** Process OCR for a single page */
    fun processPage(page: PageEntity) {
        if (page.imageUri == null) {
            _ocrError.value = "No image URI found"
            return
        }

        viewModelScope.launch {
            try {
                _ocrProgress.value = OcrProgress.Processing

                val result = withContext(Dispatchers.IO) {
                    ocrEngine.processImage(page.imageUri)
                }

                val (textBlocks, dominantLanguage) = result

                if (textBlocks.isEmpty()) {
                    _ocrProgress.value = OcrProgress.NoTextFound
                    return@launch
                }

                val bitmap = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(
                        Uri.parse(page.imageUri)
                    )?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream)
                    }
                }

                val ocrData = OcrData(
                    textBlocks = textBlocks,
                    imageWidth = bitmap?.width ?: 0,
                    imageHeight = bitmap?.height ?: 0
                )
                bitmap?.recycle()

                val jsonData = Json.encodeToString(ocrData)

                withContext(Dispatchers.IO) {
                    pageDao.updateOcrData(
                        pageId = page.id,
                        ocrDataJson = jsonData,
                        isProcessed = true,
                        language = dominantLanguage,
                        timestamp = System.currentTimeMillis()
                    )
                }

                _ocrProgress.value = OcrProgress.Success(textBlocks.size, dominantLanguage)

            } catch (e: Exception) {
                Log.e("OcrViewModel", "OCR processing failed", e)
                _ocrError.value = "OCR failed: ${e.message}"
                _ocrProgress.value = OcrProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Translate page using Gemini */
    fun translatePage(page: PageEntity) {
        if (page.ocrDataJson.isNullOrEmpty()) {
            _ocrError.value = "No OCR data found. Please scan the page first."
            return
        }

        viewModelScope.launch {
            try {
                _translationProgress.value = TranslationProgress.Processing
                Log.d("OcrViewModel", "Starting translation for page ${page.id}")

                val ocrData = Json.decodeFromString<OcrData>(page.ocrDataJson)

                val result = translator.translateBlocks(ocrData.textBlocks)

                result.fold(
                    onSuccess = { translatedBlocks ->
                        Log.d("OcrViewModel", "Translation successful")

                        val updatedOcrData = ocrData.copy(textBlocks = translatedBlocks)
                        val jsonData = Json.encodeToString(updatedOcrData)

                        withContext(Dispatchers.IO) {
                            pageDao.updateOcrData(
                                pageId = page.id,
                                ocrDataJson = jsonData,
                                isProcessed = true,
                                language = ocrData.textBlocks.firstOrNull()?.language ?: "unknown",
                                timestamp = System.currentTimeMillis()
                            )
                        }

                        _translationProgress.value = TranslationProgress.Success(translatedBlocks.size)
                    },
                    onFailure = { error ->
                        Log.e("OcrViewModel", "Translation failed", error)
                        _translationProgress.value =
                            TranslationProgress.Error(error.message ?: "Unknown error")
                    }
                )

            } catch (e: Exception) {
                Log.e("OcrViewModel", "Translation error", e)
                _translationProgress.value = TranslationProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearError() {
        _ocrError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.cleanup()
        translator.cleanup()
    }

    // ------------------------ Sealed classes ------------------------ //
    sealed class OcrProgress {
        object Idle : OcrProgress()
        object Processing : OcrProgress()
        object NoTextFound : OcrProgress()
        data class Success(val textBlockCount: Int, val language: String) : OcrProgress()
        data class Error(val message: String) : OcrProgress()
    }

    sealed class TranslationProgress {
        object Idle : TranslationProgress()
        object Processing : TranslationProgress()
        data class Success(val translatedCount: Int) : TranslationProgress()
        data class Error(val message: String) : TranslationProgress()
    }
}
