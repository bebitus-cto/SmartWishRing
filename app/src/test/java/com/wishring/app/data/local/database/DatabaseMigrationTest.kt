package com.wishring.app.data.local.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wishring.app.core.util.Constants
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test database migrations to ensure data integrity
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext(),
        WishRingDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_WithExistingWishData_ShouldPreserveDataAndConvertToMultiWish() {
        // Create database with version 2
        var db = helper.createDatabase(TEST_DB, 2).apply {
            // Insert test data in version 2 format
            execSQL("""
                INSERT INTO wish_counts (date, total_count, wish_text, target_count, is_completed, created_at, updated_at)
                VALUES ('2024-01-15', 150, 'My first wish', 1000, 0, 1642204800000, 1642204800000)
            """)
            
            execSQL("""
                INSERT INTO wish_counts (date, total_count, wish_text, target_count, is_completed, created_at, updated_at)
                VALUES ('2024-01-16', 500, 'Wish with "quotes" and \backslash', 2000, 0, 1642291200000, 1642291200000)
            """)
            
            execSQL("""
                INSERT INTO wish_counts (date, total_count, wish_text, target_count, is_completed, created_at, updated_at)
                VALUES ('2024-01-17', 0, '', 1000, 0, 1642377600000, 1642377600000)
            """)
            
            close()
        }

        // Run the migration to version 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        // Verify migration results
        val cursor = db.query("SELECT date, total_count, wish_text, target_count, wishes_json, active_wish_index FROM wish_counts ORDER BY date")
        
        // Test first record - normal wish text
        assertTrue(cursor.moveToNext())
        val date1 = cursor.getString(0)
        val totalCount1 = cursor.getInt(1)
        val wishText1 = cursor.getString(2)
        val targetCount1 = cursor.getInt(3)
        val wishesJson1 = cursor.getString(4)
        val activeWishIndex1 = cursor.getInt(5)
        
        assertEquals("2024-01-15", date1)
        assertEquals(150, totalCount1) // Preserved original count
        assertEquals("My first wish", wishText1)
        assertEquals(1000, targetCount1)
        assertEquals(0, activeWishIndex1)
        
        // Parse and verify wishes JSON
        val wishes1 = Json.decodeFromString<List<Map<String, Any>>>(wishesJson1)
        assertEquals(1, wishes1.size)
        assertEquals("My first wish", wishes1[0]["text"])
        assertEquals(1000.0, wishes1[0]["targetCount"]) // JSON numbers are doubles
        
        // Test second record - wish with special characters
        assertTrue(cursor.moveToNext())
        val date2 = cursor.getString(0)
        val totalCount2 = cursor.getInt(1)
        val wishesJson2 = cursor.getString(4)
        
        assertEquals("2024-01-16", date2)
        assertEquals(500, totalCount2) // Preserved original count
        
        val wishes2 = Json.decodeFromString<List<Map<String, Any>>>(wishesJson2)
        assertEquals(1, wishes2.size)
        assertEquals("Wish with \"quotes\" and \\backslash", wishes2[0]["text"])
        assertEquals(2000.0, wishes2[0]["targetCount"])
        
        // Test third record - empty wish text
        assertTrue(cursor.moveToNext())
        val date3 = cursor.getString(0)
        val totalCount3 = cursor.getInt(1)
        val wishesJson3 = cursor.getString(4)
        
        assertEquals("2024-01-17", date3)
        assertEquals(0, totalCount3)
        assertEquals("[]", wishesJson3) // Empty wish should result in empty array
        
        cursor.close()
        db.close()
    }

    @Test
    fun migrate2To3_WithEmptyDatabase_ShouldAddColumnsWithDefaults() {
        // Create database with version 2
        var db = helper.createDatabase(TEST_DB, 2).apply {
            close()
        }

        // Run the migration to version 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        // Verify new columns exist with correct defaults
        val cursor = db.query("PRAGMA table_info(wish_counts)")
        val columnNames = mutableListOf<String>()
        
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(1)) // Column name is at index 1
        }
        
        assertTrue(columnNames.contains("wishes_json"))
        assertTrue(columnNames.contains("active_wish_index"))
        
        cursor.close()
        db.close()
    }

    @Test
    fun fullMigrationPath_Version1To3_ShouldWorkCorrectly() {
        // Test the full migration path from version 1 to 3
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data in version 1 format
            execSQL("""
                INSERT INTO wish_counts (date, total_count, wish_text, target_count, is_completed, created_at, updated_at)
                VALUES ('2024-01-15', 100, 'Test wish', 1000, 0, 1642204800000, 1642204800000)
            """)
            close()
        }

        // Run all migrations
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        // Verify the final state includes both BLE event logs table and wishes JSON
        val tablesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        
        while (tablesCursor.moveToNext()) {
            tableNames.add(tablesCursor.getString(0))
        }
        
        assertTrue(tableNames.contains("wish_counts"))
        assertTrue(tableNames.contains("ble_event_logs"))
        
        // Verify wish data was migrated correctly
        val wishCursor = db.query("SELECT wishes_json FROM wish_counts WHERE date = '2024-01-15'")
        assertTrue(wishCursor.moveToNext())
        val wishesJson = wishCursor.getString(0)
        
        val wishes = Json.decodeFromString<List<Map<String, Any>>>(wishesJson)
        assertEquals(1, wishes.size)
        assertEquals("Test wish", wishes[0]["text"])
        
        tablesCursor.close()
        wishCursor.close()
        db.close()
    }
}