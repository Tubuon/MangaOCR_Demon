package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.mangaocr_demon.data.ChapterDao
import com.example.mangaocr_demon.data.ChapterEntity
import com.example.mangaocr_demon.data.PageDao
import com.example.mangaocr_demon.data.PageEntity

class ChapterReaderViewModel(
    private val pageDao: PageDao,
    private val chapterDao: ChapterDao,
    private val chapterId: Long
) : ViewModel() {

    val chapter: LiveData<ChapterEntity?> = chapterDao.getChapterById(chapterId).asLiveData()
    val pages: LiveData<List<PageEntity>> = pageDao.getPagesByChapterId(chapterId).asLiveData()
}