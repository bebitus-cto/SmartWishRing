package com.wishring.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wishring.app.core.util.Constants
import com.wishring.app.data.local.database.dao.WishDao
import com.wishring.app.data.local.database.entity.WishEntity

/**
 * Room database for WISH RING app
 * Manages local data persistence
 */
@Database(
    entities = [WishEntity::class],
    version = 1,
    exportSchema = true
)
abstract class WishRingDatabase : RoomDatabase() {

    /**
     * Get WishCountDao instance
     */
    abstract fun wishDao(): WishDao

    companion object {
        @Volatile
        private var INSTANCE: WishRingDatabase? = null

        /**
         * Get database instance (Singleton)
         * @param context Application context
         * @return WishRingDatabase instance
         */
        fun getInstance(context: Context): WishRingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WishRingDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 마이그레이션 실패 시 DB 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear all tables (for testing purposes)
         * WARNING: This will delete all data
         */
        suspend fun clearAllTables() {
            INSTANCE?.clearAllTables()
        }

        /**
         * Close database connection
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}