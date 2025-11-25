package com.example.mangaocr_demon.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.AppDatabase
import com.example.mangaocr_demon.data.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).historyDao()

    val allHistory: LiveData<List<HistoryEntity>> = dao.getAllHistory()

    fun insertHistory(history: HistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(history)
        }
    }

    // ✅ FIXED: clearAll() -> clearAllHistory()
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllHistory()
        }
    }
}