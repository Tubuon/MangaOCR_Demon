package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mangaocr_demon.data.MangaRepository
import com.example.mangaocr_demon.data.HistoryDao
import com.example.mangaocr_demon.ml.OcrEngine
import com.example.mangaocr_demon.ml.GeminiTranslator

class ChapterReaderViewModelFactory(
    private val mangaRepo: MangaRepository,
    private val historyDao: HistoryDao,
    private val ocrEngine: OcrEngine,
    private val translator: GeminiTranslator,
    private val chapterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapterReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChapterReaderViewModel(mangaRepo, historyDao, ocrEngine, translator, chapterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
