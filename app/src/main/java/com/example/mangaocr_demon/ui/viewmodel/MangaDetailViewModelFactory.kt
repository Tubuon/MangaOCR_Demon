package com.example.mangaocr_demon.ui.viewmodel // ⭐ ĐỔI PACKAGE

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mangaocr_demon.data.MangaRepository // ⭐ IMPORT REPO
import com.example.mangaocr_demon.data.AlbumRepository // ⭐ IMPORT REPO
import com.example.mangaocr_demon.data.ChapterEntity

class MangaDetailViewModelFactory(
    private val mangaRepo: MangaRepository, // ⭐ THAY THẾ DAO
    private val albumRepo: AlbumRepository, // ⭐ THÊM REPO
    private val mangaId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Truyền Repository vào ViewModel
            return MangaDetailViewModel(mangaRepo, albumRepo, mangaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}