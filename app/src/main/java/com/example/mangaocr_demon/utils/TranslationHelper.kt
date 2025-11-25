package com.example.mangaocr_demon.ui.utils

import android.content.Context
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TranslationHelper(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val historyDao = db.historyDao()
    private val pageDao = db.pageDao()

    suspend fun saveTranslationToHistory(
        imageUri: String?,
        ocrText: String,
        translatedText: String
    ) = withContext(Dispatchers.IO) {
        try {
            val history = HistoryEntity(
                imageUri = imageUri,
                ocrText = ocrText,
                translatedText = translatedText,
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date())
            )

            historyDao.insert(history)
            android.util.Log.d("TranslationHelper", "Saved to history: $ocrText")
        } catch (e: Exception) {
            android.util.Log.e("TranslationHelper", "Error saving to history", e)
        }
    }

    suspend fun saveTranslationToPage(
        pageId: Long,
        ocrText: String?,
        translatedText: String?
    ) = withContext(Dispatchers.IO) {
        try {
            pageDao.updateTranslation(
                pageId = pageId,
                ocrText = ocrText,
                translatedText = translatedText,
                timestamp = System.currentTimeMillis()
            )

            android.util.Log.d("TranslationHelper", "Updated page $pageId with translation")
        } catch (e: Exception) {
            android.util.Log.e("TranslationHelper", "Error updating page translation", e)
        }
    }
}