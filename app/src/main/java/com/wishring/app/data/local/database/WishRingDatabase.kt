package com.wishring.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wishring.app.core.util.Constants
import com.wishring.app.data.local.database.dao.WishCountDao
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
    abstract fun wishDao(): WishCountDao
    
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