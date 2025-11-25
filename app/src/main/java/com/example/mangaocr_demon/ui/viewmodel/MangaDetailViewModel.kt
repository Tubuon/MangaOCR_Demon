package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.AlbumRepository
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.data.MangaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MangaDetailViewModel(
    private val mangaRepo: MangaRepository,
    private val albumRepo: AlbumRepository,
    private val albumId: Long
) : ViewModel() {

    val chapters: Flow<List<ChapterEntity>> = mangaRepo.getChaptersByAlbumIdFlow(albumId)

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            mangaRepo.deleteChapter(chapter)
        }
    }

    fun addChapterToAlbum(chapterId: Long, albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            albumRepo.addChapterToAlbum(albumId, chapterId)
        }
    }
}
