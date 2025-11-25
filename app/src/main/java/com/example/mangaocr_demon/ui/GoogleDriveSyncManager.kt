package com.example.mangaocr_demon.ui

import android.content.Context
import com.example.mangaocr_demon.data.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class GoogleDriveSyncManager(private val context: Context) {

    private val appFolderName = "MangaOCR_Demon_Backup"

    /**
     * Lấy Drive service instance
     */
    private suspend fun getDriveService(): Drive? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("MangaOCR Demon")
                .build()
        } catch (e: Exception) {
            android.util.Log.e("DriveSyncManager", "Error creating Drive service", e)
            null
        }
    }

    /**
     * Tìm hoặc tạo folder backup trên Drive
     */
    private suspend fun getOrCreateBackupFolder(driveService: Drive): String? =
        withContext(Dispatchers.IO) {
            try {
                val query = "mimeType='application/vnd.google-apps.folder' and " +
                        "name='$appFolderName' and trashed=false"

                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                if (result.files.isNotEmpty()) {
                    return@withContext result.files[0].id
                }

                val folderMetadata = File().apply {
                    name = appFolderName
                    mimeType = "application/vnd.google-apps.folder"
                }

                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()

                folder.id
            } catch (e: Exception) {
                android.util.Log.e("DriveSyncManager", "Error getting/creating folder", e)
                null
            }
        }


    suspend fun backupToGoogleDrive(
        selection: BackupSelection,
        listener: SyncProgressListener
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // callback trên Main thread
            withContext(Dispatchers.Main) {
                listener.onProgress(10, "Đang kết nối Drive...")
            }

            val driveService = getDriveService() ?: run {
                withContext(Dispatchers.Main) {
                    listener.onError("Không thể kết nối Drive")
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                listener.onProgress(20, "Đang thu thập dữ liệu...")
            }

            val db = AppDatabase.getDatabase(context)

            val backupData = BackupData(
                mangaList = if (selection.includeManga) {
                    db.mangaDao().getAllManga().first()
                } else null,

                chapters = if (selection.includeChapters) {
                    db.chapterDao().getAllChapters()
                } else null,

                pages = if (selection.includePages) {
                    db.pageDao().getAllPages()
                } else null,

                history = if (selection.includeHistory) {
                    db.historyDao().getAllHistorySync()
                } else null,

                albums = if (selection.includeAlbums) {
                    db.albumDao().getAllAlbumsSync()
                } else null,

                albumChapters = if (selection.includeAlbums) {
                    db.albumChapterDao().getAllAlbumChaptersSync()
                } else null
            )

            withContext(Dispatchers.Main) {
                listener.onProgress(50, "Đang nén dữ liệu...")
            }

            val jsonData = backupData.toJson()
            val content = ByteArrayContent.fromString("application/json", jsonData)

            withContext(Dispatchers.Main) {
                listener.onProgress(70, "Đang upload lên Drive...")
            }

            val folderId = getOrCreateBackupFolder(driveService) ?: run {
                withContext(Dispatchers.Main) {
                    listener.onError("Không thể tạo folder backup")
                }
                return@withContext false
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "backup_$timestamp.json"

            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
                mimeType = "application/json"
            }

            driveService.files().create(fileMetadata, content)
                .setFields("id, name, size")
                .execute()

            withContext(Dispatchers.Main) {
                listener.onProgress(90, "Đang hoàn tất...")
            }

            saveLastSyncTime()

            withContext(Dispatchers.Main) {
                listener.onProgress(100, "Hoàn tất!")
                listener.onSuccess("Backup thành công: $fileName")
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("DriveSyncManager", "Backup error", e)
            withContext(Dispatchers.Main) {
                listener.onError("Lỗi backup: ${e.message}")
            }
            false
        }
    }

    /**
     * Lấy danh sách backup files từ Drive
     */
    suspend fun getBackupFiles(): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext emptyList()
            val folderId = getOrCreateBackupFolder(driveService) ?: return@withContext emptyList()

            val query = "'$folderId' in parents and trashed=false and " +
                    "mimeType='application/json'"

            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files.map { file ->
                BackupInfo(
                    fileName = file.name,
                    fileId = file.id,
                    size = file.getSize() ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DriveSyncManager", "Error listing backups", e)
            emptyList()
        }
    }
    /**
     * Restore dữ liệu từ Google Drive
     */
    suspend fun restoreFromGoogleDrive(
        fileId: String,
        selection: BackupSelection,
        listener: SyncProgressListener
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                listener.onProgress(10, "Đang kết nối Drive...")
            }

            val driveService = getDriveService() ?: run {
                withContext(Dispatchers.Main) {
                    listener.onError("Không thể kết nối Drive")
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                listener.onProgress(30, "Đang tải file backup...")
            }

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val jsonData = outputStream.toString("UTF-8")

            withContext(Dispatchers.Main) {
                listener.onProgress(50, "Đang giải nén dữ liệu...")
            }

            val backupData = BackupData.fromJson(jsonData) ?: run {
                withContext(Dispatchers.Main) {
                    listener.onError("File backup không hợp lệ")
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                listener.onProgress(60, "Đang khôi phục dữ liệu...")
            }

            val db = AppDatabase.getDatabase(context)

            if (selection.includeManga && backupData.mangaList != null) {
                withContext(Dispatchers.Main) {
                    listener.onProgress(65, "Đang khôi phục manga...")
                }
                backupData.mangaList.forEach { manga ->
                    db.mangaDao().insert(manga.copy(id = 0))
                }
            }

            if (selection.includeChapters && backupData.chapters != null) {
                withContext(Dispatchers.Main) {
                    listener.onProgress(70, "Đang khôi phục chapters...")
                }
                backupData.chapters.forEach { chapter ->
                    db.chapterDao().insert(chapter.copy(id = 0))
                }
            }

            if (selection.includePages && backupData.pages != null) {
                withContext(Dispatchers.Main) {
                    listener.onProgress(75, "Đang khôi phục pages...")
                }
                backupData.pages.forEach { page ->
                    db.pageDao().insert(page.copy(id = 0))
                }
            }

            if (selection.includeHistory && backupData.history != null) {
                withContext(Dispatchers.Main) {
                    listener.onProgress(80, "Đang khôi phục lịch sử...")
                }
                backupData.history.forEach { history ->
                    db.historyDao().insert(history.copy(id = 0))
                }
            }

            if (selection.includeAlbums) {
                if (backupData.albums != null) {
                    withContext(Dispatchers.Main) {
                        listener.onProgress(85, "Đang khôi phục albums...")
                    }
                    backupData.albums.forEach { album ->
                        db.albumDao().insert(album.copy(id = 0))
                    }
                }

                if (backupData.albumChapters != null) {
                    withContext(Dispatchers.Main) {
                        listener.onProgress(90, "Đang khôi phục album chapters...")
                    }
                    backupData.albumChapters.forEach { ac ->
                        db.albumChapterDao().addChapterToAlbum(ac)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                listener.onProgress(95, "Đang hoàn tất...")
            }

            saveLastSyncTime()

            withContext(Dispatchers.Main) {
                listener.onProgress(100, "Hoàn tất!")
                listener.onSuccess("Khôi phục thành công!")
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("DriveSyncManager", "Restore error", e)
            withContext(Dispatchers.Main) {
                listener.onError("Lỗi khôi phục: ${e.message}")
            }
            false
        }
    }

    private fun saveLastSyncTime() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date())
        prefs.edit().putString("last_sync_time", timestamp).apply()
    }

    fun getLastSyncTime(): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("last_sync_time", "Chưa đồng bộ") ?: "Chưa đồng bộ"
    }
}