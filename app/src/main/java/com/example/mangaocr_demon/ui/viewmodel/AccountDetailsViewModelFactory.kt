// File: ui/viewmodel/AccountDetailsViewModelFactory.kt
package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mangaocr_demon.data.AlbumDao
import com.example.mangaocr_demon.data.HistoryDao
import com.example.mangaocr_demon.data.MangaDao
import com.example.mangaocr_demon.data.SyncHistoryDao

class AccountDetailsViewModelFactory(
    private val mangaDao: MangaDao,
    private val historyDao: HistoryDao,
    private val albumDao: AlbumDao,
    private val syncHistoryDao: SyncHistoryDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountDetailsViewModel(
                mangaDao = mangaDao,
                historyDao = historyDao,
                albumDao = albumDao,
                syncHistoryDao = syncHistoryDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
