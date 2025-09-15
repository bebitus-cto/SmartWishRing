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
import com.wishring.app.data.local.dao.BleEventDao
import com.wishring.app.data.local.entity.BleEventLogEntity

/**
 * Room database for WISH RING app
 * Manages local data persistence
 */
@Database(
    entities = [
        WishCountEntity::class,
        ResetLogEntity::class,
        BleEventLogEntity::class
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
    
    /**
     * Get BleEventDao instance
     */
    abstract fun bleEventDao(): BleEventDao
    
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
                MIGRATION_1_2,
                MIGRATION_2_3
            )
        }
        
        /**
         * Migration from version 1 to 2: Add BLE event logs table
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create ble_event_logs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ble_event_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        value INTEGER NOT NULL,
                        deviceAddress TEXT NOT NULL DEFAULT '',
                        additional TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add wishes_json column to store multiple wishes
                database.execSQL("""
                    ALTER TABLE wish_counts 
                    ADD COLUMN wishes_json TEXT NOT NULL DEFAULT '[]'
                """)
                
                // Add active_wish_index column to track which wish is currently active
                database.execSQL("""
                    ALTER TABLE wish_counts 
                    ADD COLUMN active_wish_index INTEGER NOT NULL DEFAULT 0
                """)
                
                // Migrate existing single-wish data to multi-wish format
                // We need to handle JSON escaping properly, so we'll do this in steps
                
                // First, get all existing records and update them individually
                val cursor = database.query("SELECT date, wish_text, target_count FROM wish_counts WHERE wishes_json = '[]'")
                
                val recordsToUpdate = mutableListOf<Triple<String, String, Int>>()
                
                while (cursor.moveToNext()) {
                    val date = cursor.getString(0)
                    val wishText = cursor.getString(1) ?: ""
                    val targetCount = cursor.getInt(2)
                    
                    if (wishText.isNotEmpty()) {
                        recordsToUpdate.add(Triple(date, wishText, targetCount))
                    }
                }
                cursor.close()
                
                // Update each record with properly escaped JSON
                recordsToUpdate.forEach { (date, wishText, targetCount) ->
                    // Escape JSON special characters
                    val escapedText = wishText
                        .replace("\\", "\\\\")  // Escape backslashes first
                        .replace("\"", "\\\"")  // Escape quotes
                        .replace("\n", "\\n")   // Escape newlines
                        .replace("\r", "\\r")   // Escape carriage returns
                        .replace("\t", "\\t")   // Escape tabs
                    
                    val wishesJson = """[{"text":"$escapedText","targetCount":$targetCount}]"""
                    
                    database.execSQL("""
                        UPDATE wish_counts 
                        SET wishes_json = ?, active_wish_index = 0
                        WHERE date = ?
                    """, arrayOf(wishesJson, date))
                }
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