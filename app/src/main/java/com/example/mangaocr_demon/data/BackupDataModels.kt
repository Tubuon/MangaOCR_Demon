package com.example.mangaocr_demon.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Model cho backup data
 */
data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("manga_list") val mangaList: List<MangaEntity>? = null,
    @SerializedName("chapters") val chapters: List<ChapterEntity>? = null,
    @SerializedName("pages") val pages: List<PageEntity>? = null,
    @SerializedName("history") val history: List<HistoryEntity>? = null,
    @SerializedName("albums") val albums: List<AlbumEntity>? = null,
    @SerializedName("album_chapters") val albumChapters: List<AlbumChapterEntity>? = null
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): BackupData? {
            return try {
                Gson().fromJson(json, BackupData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Thông tin về backup file
 */
data class BackupInfo(
    val fileName: String,
    val fileId: String,
    val size: Long,
    val modifiedTime: Long,
    val mangaCount: Int = 0,
    val chapterCount: Int = 0,
    val pageCount: Int = 0,
    val historyCount: Int = 0,
    val albumCount: Int = 0
)

/**
 * Selection state cho backup/restore
 */
data class BackupSelection(
    var includeManga: Boolean = true,
    var includeChapters: Boolean = true,
    var includePages: Boolean = true,
    var includeHistory: Boolean = true,
    var includeAlbums: Boolean = true
) {
    fun hasAnySelection(): Boolean {
        return includeManga || includeChapters || includePages ||
                includeHistory || includeAlbums
    }
}

/**
 * Progress callback cho sync operationsOcrEngine
 */
interface SyncProgressListener {
    fun onProgress(progress: Int, message: String)
    fun onSuccess(message: String)
    fun onError(error: String)
}