package com.wishring.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wishring.app.core.util.Constants
import com.wishring.app.data.local.database.converter.DateConverter
import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.dao.WishCountDao
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.database.entity.WishCountEntity

/**
 * Room database for WISH RING app
 * Manages local data persistence
 */
@Database(
    entities = [
        WishCountEntity::class,
        ResetLogEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class WishRingDatabase : RoomDatabase() {
    
    /**
     * Get WishCountDao instance
     */
    abstract fun wishCountDao(): WishCountDao
    
    /**
     * Get ResetLogDao instance
     */
    abstract fun resetLogDao(): ResetLogDao
    
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
                    .addMigrations(*getAllMigrations())
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Database callback for onCreate and onOpen events
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initialize database with default data if needed
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Perform any cleanup or validation when database is opened
            }
        }
        
        /**
         * Get all database migrations
         * @return Array of Migration objects
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Add migrations here as database schema evolves
                // Example: MIGRATION_1_2
            )
        }
        
        /**
         * Example migration (commented out for initial version)
         * Uncomment and modify when adding first migration
         */
        /*
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Perform schema changes
                // Example: database.execSQL("ALTER TABLE wish_counts ADD COLUMN new_column TEXT")
            }
        }
        */
        
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