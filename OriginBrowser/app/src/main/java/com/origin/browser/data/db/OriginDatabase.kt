package com.origin.browser.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.origin.browser.data.db.dao.BookmarkDao
import com.origin.browser.data.db.dao.HistoryDao
import com.origin.browser.data.db.entity.BookmarkEntry
import com.origin.browser.data.db.entity.HistoryEntry

@Database(
    entities = [HistoryEntry::class, BookmarkEntry::class],
    version = 1,
    exportSchema = false
)
abstract class OriginDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: OriginDatabase? = null

        fun getDatabase(context: Context): OriginDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OriginDatabase::class.java,
                    "origin_browser_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
