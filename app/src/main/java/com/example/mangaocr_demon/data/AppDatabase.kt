// File: data/AppDatabase.kt
package com.example.mangaocr_demon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase - Room Database cho toàn bộ project MangaOCR
 * Version 6 - đã thêm SyncHistoryEntity và AlbumChapterEntity
 */
@Database(
    entities = [
        HistoryEntity::class,
        MangaEntity::class,
        ChapterEntity::class,
        PageEntity::class,
        AlbumEntity::class,
        AlbumChapterEntity::class,
        SyncHistoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // --------------------------
    // Dao interfaces
    // --------------------------
    abstract fun historyDao(): HistoryDao
    abstract fun mangaDao(): MangaDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageDao(): PageDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumChapterDao(): AlbumChapterDao
    abstract fun syncHistoryDao(): SyncHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Singleton instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manga_database"
                )
                    // Khi schema thay đổi, xóa data cũ để tránh crash
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
