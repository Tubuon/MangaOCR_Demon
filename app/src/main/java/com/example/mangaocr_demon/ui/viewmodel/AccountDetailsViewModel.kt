// File: ui/viewmodel/AccountDetailsViewModel.kt
package com.example.mangaocr_demon.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangaocr_demon.data.AlbumDao
import com.example.mangaocr_demon.data.HistoryDao
import com.example.mangaocr_demon.data.MangaDao
import com.example.mangaocr_demon.data.SyncHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Data class giữ số liệu thống kê (dùng cho Fragment/Compose)
data class StatsData(
    val mangaCount: Int = 0,
    val chapterCount: Int = 0,
    val pageCount: Int = 0,
    val historyCount: Int = 0,
    val albumCount: Int = 0,
    val lastSyncTime: Long = 0,
    val successfulSyncs: Int = 0
)

class AccountDetailsViewModel(
    private val mangaDao: MangaDao,
    private val historyDao: HistoryDao,
    private val albumDao: AlbumDao,
    private val syncHistoryDao: SyncHistoryDao
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsData())
    val stats: StateFlow<StatsData> = _stats

    init {
        loadStats()
        observeSyncHistory()
    }

    /**
     * Tải tất cả số liệu thống kê
     */
    fun loadStats() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val mangaList = mangaDao.getAllMangaSync()
            var totalChapters = 0
            var totalPages = 0
            for (manga in mangaList) {
                val chapters = mangaDao.getAllMangaSync().map { it.id } // hoặc gọi ChapterDao để get
                totalChapters += chapters.size
                // TODO: tính totalPages nếu cần
            }

            val statsData = StatsData(
                mangaCount = mangaDao.getMangaCount(),
                chapterCount = totalChapters,
                pageCount = 0, // có thể tính từ PageDao nếu cần
                historyCount = historyDao.getHistoryCount(),
                albumCount = albumDao.getAlbumCount()
            )

            _stats.value = statsData
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lắng nghe trạng thái đồng bộ từ SyncHistoryDao
     */
    private fun observeSyncHistory() = viewModelScope.launch(Dispatchers.IO) {
        syncHistoryDao.getSyncHistoryFlow().collect { history ->
            val current = _stats.value
            _stats.value = current.copy(
                lastSyncTime = history?.lastSyncTime ?: 0,
                successfulSyncs = history?.successfulSyncs ?: 0
            )
        }
    }

    /**
     * Ghi lại Backup thành công
     */
    fun logBackupSuccess() = viewModelScope.launch(Dispatchers.IO) {
        syncHistoryDao.updateSyncSuccess(System.currentTimeMillis())
    }
}
