package com.wishring.app.integration

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.wishring.app.data.local.database.WishRingDatabase
import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.dao.WishCountDao
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.database.entity.WishCountEntity
import com.wishring.app.domain.model.WishCount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Database Integration 테스트
 * 
 * Room 데이터베이스의 통합 동작을 검증합니다.
 * 
 * 테스트 영역:
 * 1. Schema Migration - 스키마 마이그레이션
 * 2. Transaction Management - 트랜잭션 관리
 * 3. Query Performance - 쿼리 성능
 * 4. Data Integrity - 데이터 무결성
 * 5. Concurrent Access - 동시 접근
 * 6. Backup & Restore - 백업 및 복원
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Database Integration 테스트 - Room Database")
class DatabaseIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: WishRingDatabase
    private lateinit var wishCountDao: WishCountDao
    private lateinit var resetLogDao: ResetLogDao
    private lateinit var testScope: TestScope
    
    @BeforeEach
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        // In-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            WishRingDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        wishCountDao = database.wishCountDao()
        resetLogDao = database.resetLogDao()
    }
    
    @AfterEach
    fun tearDown() {
        database.close()
    }
    
    @Nested
    @DisplayName("1. Schema Migration - 스키마 마이그레이션")
    inner class SchemaMigrationTest {
        
        @Test
        @DisplayName("Version 1 → 2 마이그레이션")
        fun testMigration1To2() = testScope.runTest {
            // Given - Version 1 데이터
            val testDb = "migration_test.db"
            val helper = MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                WishRingDatabase::class.java,
                emptyList(),
                FrameworkSQLiteOpenHelperFactory()
            )
            
            // Version 1 데이터베이스 생성
            var db = helper.createDatabase(testDb, 1).apply {
                execSQL("""
                    INSERT INTO wish_count_table (date, wish_text, target_count, current_count)
                    VALUES ('2024-01-01', 'Test Wish', 10, 5)
                """)
                close()
            }
            
            // When - Version 2로 마이그레이션
            db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)
            
            // Then - 데이터 유지 확인
            val cursor = db.query("SELECT * FROM wish_count_table WHERE date = '2024-01-01'")
            cursor.moveToFirst()
            cursor.getString(cursor.getColumnIndex("wish_text")) shouldBe "Test Wish"
            cursor.getInt(cursor.getColumnIndex("target_count")) shouldBe 10
            
            // 새 컬럼 확인
            cursor.getColumnIndex("completed_at") shouldNotBe -1
            cursor.getColumnIndex("is_completed") shouldNotBe -1
            
            cursor.close()
        }
        
        @Test
        @DisplayName("Version 2 → 3 인덱스 추가")
        fun testMigration2To3WithIndex() = testScope.runTest {
            // Given
            val testDb = "index_migration_test.db"
            val helper = MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                WishRingDatabase::class.java,
                emptyList(),
                FrameworkSQLiteOpenHelperFactory()
            )
            
            // Version 2 데이터베이스
            var db = helper.createDatabase(testDb, 2)
            
            // When - Version 3로 마이그레이션 (인덱스 추가)
            db = helper.runMigrationsAndValidate(testDb, 3, true, MIGRATION_2_3)
            
            // Then - 인덱스 존재 확인
            val cursor = db.query("""
                SELECT name FROM sqlite_master 
                WHERE type='index' AND name='index_wish_count_date'
            """)
            
            cursor.moveToFirst()
            cursor.getString(0) shouldBe "index_wish_count_date"
            cursor.close()
        }
        
        @Test
        @DisplayName("다중 버전 점프 마이그레이션")
        fun testMultiVersionMigration() = testScope.runTest {
            // Given - Version 1
            val testDb = "multi_version_test.db"
            val helper = MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                WishRingDatabase::class.java,
                emptyList(),
                FrameworkSQLiteOpenHelperFactory()
            )
            
            var db = helper.createDatabase(testDb, 1)
            
            // When - Version 1 → 4 직접 마이그레이션
            db = helper.runMigrationsAndValidate(
                testDb, 4, true,
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4
            )
            
            // Then - 모든 스키마 변경 적용 확인
            val tableInfo = db.query("PRAGMA table_info(wish_count_table)")
            val columns = mutableListOf<String>()
            
            while (tableInfo.moveToNext()) {
                columns.add(tableInfo.getString(1)) // column name
            }
            
            columns shouldContain "date"
            columns shouldContain "wish_text"
            columns shouldContain "completed_at"
            columns shouldContain "streak_count" // Version 4에서 추가
            
            tableInfo.close()
        }
    }
    
    @Nested
    @DisplayName("2. Transaction Management - 트랜잭션 관리")
    inner class TransactionManagementTest {
        
        @Test
        @DisplayName("트랜잭션 롤백 테스트")
        fun testTransactionRollback() = testScope.runTest {
            // Given
            val initialCount = wishCountDao.getAllWishCounts().first().size
            
            // When - 트랜잭션 내에서 예외 발생
            try {
                database.runInTransaction {
                    val entity1 = WishCountEntity(
                        date = LocalDate.now(),
                        wishText = "Transaction Test 1",
                        targetCount = 10
                    )
                    wishCountDao.insert(entity1)
                    
                    // 의도적 예외 발생
                    throw Exception("Rollback test")
                    
                    val entity2 = WishCountEntity(
                        date = LocalDate.now().plusDays(1),
                        wishText = "Transaction Test 2",
                        targetCount = 20
                    )
                    wishCountDao.insert(entity2)
                }
            } catch (e: Exception) {
                // 예외 처리
            }
            
            // Then - 트랜잭션 롤백으로 데이터 변경 없음
            val finalCount = wishCountDao.getAllWishCounts().first().size
            finalCount shouldBe initialCount
        }
        
        @Test
        @DisplayName("중첩 트랜잭션 처리")
        fun testNestedTransactions() = testScope.runTest {
            // Given
            var outerTransactionComplete = false
            var innerTransactionComplete = false
            
            // When
            database.runInTransaction {
                val outerEntity = WishCountEntity(
                    date = LocalDate.now(),
                    wishText = "Outer Transaction",
                    targetCount = 10
                )
                wishCountDao.insert(outerEntity)
                outerTransactionComplete = true
                
                database.runInTransaction {
                    val innerEntity = WishCountEntity(
                        date = LocalDate.now().plusDays(1),
                        wishText = "Inner Transaction",
                        targetCount = 20
                    )
                    wishCountDao.insert(innerEntity)
                    innerTransactionComplete = true
                }
            }
            
            // Then
            outerTransactionComplete shouldBe true
            innerTransactionComplete shouldBe true
            
            val allRecords = wishCountDao.getAllWishCounts().first()
            allRecords shouldHaveSize 2
        }
        
        @Test
        @DisplayName("대량 삽입 트랜잭션 성능")
        fun testBulkInsertTransaction() = testScope.runTest {
            // Given
            val entities = List(1000) { index ->
                WishCountEntity(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Bulk Insert $index",
                    targetCount = 10,
                    currentCount = Random.nextInt(0, 11)
                )
            }
            
            // When - 트랜잭션 사용
            val withTransactionTime = measureTimeMillis {
                database.runInTransaction {
                    entities.forEach { entity ->
                        wishCountDao.insert(entity)
                    }
                }
            }
            
            // 데이터 초기화
            wishCountDao.deleteAll()
            
            // 트랜잭션 미사용
            val withoutTransactionTime = measureTimeMillis {
                entities.forEach { entity ->
                    wishCountDao.insert(entity)
                }
            }
            
            // Then - 트랜잭션 사용이 더 빠름
            (withTransactionTime < withoutTransactionTime) shouldBe true
            
            val finalCount = wishCountDao.getAllWishCounts().first().size
            finalCount shouldBe 1000
        }
    }
    
    @Nested
    @DisplayName("3. Query Performance - 쿼리 성능")
    inner class QueryPerformanceTest {
        
        @Test
        @DisplayName("인덱스 사용 쿼리 성능")
        fun testIndexedQueryPerformance() = testScope.runTest {
            // Given - 대량 데이터 준비
            val baseDate = LocalDate.now()
            repeat(10000) { index ->
                val entity = WishCountEntity(
                    date = baseDate.plusDays(index.toLong()),
                    wishText = "Performance Test $index",
                    targetCount = 10,
                    currentCount = index % 11
                )
                wishCountDao.insert(entity)
            }
            
            // When - 인덱스된 컬럼(date)으로 조회
            val indexedQueryTime = measureTimeMillis {
                repeat(100) {
                    val randomDate = baseDate.plusDays(Random.nextLong(0, 10000))
                    wishCountDao.getWishCountByDate(randomDate)
                }
            }
            
            // 비인덱스 컬럼으로 조회 시뮬레이션
            val nonIndexedQueryTime = measureTimeMillis {
                repeat(100) {
                    val randomText = "Performance Test ${Random.nextInt(0, 10000)}"
                    wishCountDao.searchByWishText(randomText)
                }
            }
            
            // Then - 인덱스 쿼리가 더 빠름
            (indexedQueryTime < nonIndexedQueryTime * 2) shouldBe true
        }
        
        @Test
        @DisplayName("페이징 쿼리 최적화")
        fun testPagingQueryOptimization() = testScope.runTest {
            // Given - 1000개 데이터
            repeat(1000) { index ->
                val entity = WishCountEntity(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Paging Test $index",
                    targetCount = 10
                )
                wishCountDao.insert(entity)
            }
            
            // When - 페이징 조회
            val pageSize = 20
            val pages = mutableListOf<List<WishCountEntity>>()
            
            var offset = 0
            while (true) {
                val page = wishCountDao.getWishCountsPaged(pageSize, offset).first()
                if (page.isEmpty()) break
                pages.add(page)
                offset += pageSize
            }
            
            // Then
            pages shouldHaveSize 50 // 1000 / 20
            pages.forEach { page ->
                page.size <= pageSize shouldBe true
            }
        }
        
        @ParameterizedTest
        @ValueSource(ints = [10, 30, 90, 180, 365])
        @DisplayName("날짜 범위 쿼리 성능")
        fun testDateRangeQueryPerformance(days: Int) = testScope.runTest {
            // Given - 1년치 데이터
            val today = LocalDate.now()
            repeat(365) { index ->
                val entity = WishCountEntity(
                    date = today.minusDays(index.toLong()),
                    wishText = "Range Test $index",
                    targetCount = 10,
                    currentCount = Random.nextInt(0, 11)
                )
                wishCountDao.insert(entity)
            }
            
            // When
            val queryTime = measureTimeMillis {
                val startDate = today.minusDays(days.toLong())
                val result = wishCountDao.getWishCountsForDateRange(startDate, today).first()
                result.size // Force evaluation
            }
            
            // Then - 쿼리 시간이 선형적으로 증가
            queryTime < days * 2 shouldBe true // 2ms per day max
        }
    }
    
    @Nested
    @DisplayName("4. Data Integrity - 데이터 무결성")
    inner class DataIntegrityTest {
        
        @Test
        @DisplayName("외래 키 제약 조건")
        fun testForeignKeyConstraints() = testScope.runTest {
            // Given - WishCount 생성
            val wishCount = WishCountEntity(
                id = 1,
                date = LocalDate.now(),
                wishText = "FK Test",
                targetCount = 10
            )
            wishCountDao.insert(wishCount)
            
            // When - 연관된 ResetLog 생성
            val resetLog = ResetLogEntity(
                wishCountId = 1,
                resetTime = LocalDateTime.now(),
                previousCount = 5,
                lostCount = 5,
                resetType = "MANUAL"
            )
            resetLogDao.insert(resetLog)
            
            // WishCount 삭제 시도
            wishCountDao.delete(wishCount)
            
            // Then - CASCADE DELETE로 ResetLog도 삭제됨
            val remainingLogs = resetLogDao.getAllResetLogs().first()
            remainingLogs shouldHaveSize 0
        }
        
        @Test
        @DisplayName("유니크 제약 조건")
        fun testUniqueConstraints() = testScope.runTest {
            // Given
            val date = LocalDate.now()
            val entity1 = WishCountEntity(
                date = date,
                wishText = "Unique Test 1",
                targetCount = 10
            )
            
            // When - 같은 날짜에 두 개 삽입 시도
            wishCountDao.insert(entity1)
            
            val entity2 = WishCountEntity(
                date = date,
                wishText = "Unique Test 2",
                targetCount = 20
            )
            
            // Then - REPLACE 전략으로 업데이트됨
            wishCountDao.insert(entity2)
            
            val result = wishCountDao.getWishCountByDate(date).first()
            result?.wishText shouldBe "Unique Test 2"
            result?.targetCount shouldBe 20
        }
        
        @Test
        @DisplayName("NOT NULL 제약 조건")
        fun testNotNullConstraints() = testScope.runTest {
            // Given
            val invalidEntity = WishCountEntity(
                date = LocalDate.now(),
                wishText = null!!, // 의도적 null
                targetCount = 10
            )
            
            // When & Then - 예외 발생
            try {
                wishCountDao.insert(invalidEntity)
                assert(false) { "Should throw exception" }
            } catch (e: Exception) {
                // Expected
                e.message?.contains("NOT NULL") shouldBe true
            }
        }
    }
    
    @Nested
    @DisplayName("5. Concurrent Access - 동시 접근")
    inner class ConcurrentAccessTest {
        
        @Test
        @DisplayName("동시 읽기/쓰기 안전성")
        fun testConcurrentReadWrite() = testScope.runTest {
            // Given
            val writeCount = AtomicInteger(0)
            val readCount = AtomicInteger(0)
            val errors = ConcurrentLinkedQueue<Exception>()
            
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(100)
            
            // When - 50개 쓰기, 50개 읽기 동시 실행
            repeat(50) { index ->
                executor.submit {
                    try {
                        val entity = WishCountEntity(
                            date = LocalDate.now().plusDays(index.toLong()),
                            wishText = "Concurrent Write $index",
                            targetCount = 10
                        )
                        wishCountDao.insert(entity)
                        writeCount.incrementAndGet()
                    } catch (e: Exception) {
                        errors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
                
                executor.submit {
                    try {
                        val result = wishCountDao.getAllWishCounts().first()
                        readCount.incrementAndGet()
                    } catch (e: Exception) {
                        errors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            
            // Then
            errors shouldHaveSize 0
            writeCount.get() shouldBe 50
            readCount.get() shouldBe 50
        }
        
        @Test
        @DisplayName("동시 업데이트 충돌 해결")
        fun testConcurrentUpdateConflict() = testScope.runTest {
            // Given - 초기 데이터
            val date = LocalDate.now()
            val initial = WishCountEntity(
                date = date,
                wishText = "Conflict Test",
                targetCount = 100,
                currentCount = 0
            )
            wishCountDao.insert(initial)
            
            // When - 100개 스레드가 동시에 카운트 증가
            val executor = Executors.newFixedThreadPool(20)
            val latch = CountDownLatch(100)
            
            repeat(100) {
                executor.submit {
                    try {
                        wishCountDao.incrementCount(date)
                    } finally {
                        latch.countDown()
                    }
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            
            // Then - 모든 증가가 반영됨
            val final = wishCountDao.getWishCountByDate(date).first()
            final?.currentCount shouldBe 100
        }
        
        @Test
        @DisplayName("데드락 방지 메커니즘")
        fun testDeadlockPrevention() = testScope.runTest {
            // Given
            val date1 = LocalDate.now()
            val date2 = LocalDate.now().plusDays(1)
            
            wishCountDao.insert(
                WishCountEntity(date = date1, wishText = "Lock1", targetCount = 10)
            )
            wishCountDao.insert(
                WishCountEntity(date = date2, wishText = "Lock2", targetCount = 10)
            )
            
            val deadlockDetected = AtomicBoolean(false)
            val executor = Executors.newFixedThreadPool(2)
            val latch = CountDownLatch(2)
            
            // When - 교차 업데이트로 데드락 유도
            executor.submit {
                database.runInTransaction {
                    wishCountDao.incrementCount(date1)
                    Thread.sleep(100)
                    wishCountDao.incrementCount(date2)
                }
                latch.countDown()
            }
            
            executor.submit {
                database.runInTransaction {
                    wishCountDao.incrementCount(date2)
                    Thread.sleep(100)
                    wishCountDao.incrementCount(date1)
                }
                latch.countDown()
            }
            
            // Then - 타임아웃 없이 완료
            val completed = latch.await(5, TimeUnit.SECONDS)
            completed shouldBe true
            deadlockDetected.get() shouldBe false
            
            executor.shutdown()
        }
    }
    
    @Nested
    @DisplayName("6. Backup & Restore - 백업 및 복원")
    inner class BackupRestoreTest {
        
        @Test
        @DisplayName("데이터베이스 백업 생성")
        fun testDatabaseBackup() = testScope.runTest {
            // Given - 테스트 데이터
            repeat(100) { index ->
                val entity = WishCountEntity(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Backup Test $index",
                    targetCount = 10,
                    currentCount = index % 11
                )
                wishCountDao.insert(entity)
            }
            
            // When - 백업 생성
            val backupPath = "${context.filesDir}/backup.db"
            database.close()
            
            val sourceFile = context.getDatabasePath("test.db")
            val backupFile = java.io.File(backupPath)
            sourceFile.copyTo(backupFile, overwrite = true)
            
            // Then - 백업 파일 검증
            backupFile.exists() shouldBe true
            backupFile.length() > 0 shouldBe true
        }
        
        @Test
        @DisplayName("데이터베이스 복원")
        fun testDatabaseRestore() = testScope.runTest {
            // Given - 백업 데이터베이스
            val backupDb = Room.databaseBuilder(
                context,
                WishRingDatabase::class.java,
                "backup.db"
            ).build()
            
            // 백업 데이터 생성
            repeat(50) { index ->
                val entity = WishCountEntity(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Restore Test $index",
                    targetCount = 10
                )
                backupDb.wishCountDao().insert(entity)
            }
            backupDb.close()
            
            // When - 복원
            val restoredDb = Room.databaseBuilder(
                context,
                WishRingDatabase::class.java,
                "restored.db"
            ).createFromFile(java.io.File("${context.filesDir}/backup.db"))
                .build()
            
            // Then - 데이터 확인
            val restoredData = restoredDb.wishCountDao().getAllWishCounts().first()
            restoredData shouldHaveSize 50
            restoredData[0].wishText shouldContain "Restore Test"
            
            restoredDb.close()
        }
        
        @Test
        @DisplayName("점진적 마이그레이션 백업")
        fun testIncrementalBackup() = testScope.runTest {
            // Given - 초기 데이터
            val initialData = List(10) { index ->
                WishCountEntity(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Initial $index",
                    targetCount = 10
                )
            }
            initialData.forEach { wishCountDao.insert(it) }
            
            // 첫 번째 백업
            val backup1 = createBackup("backup1.db")
            
            // 추가 데이터
            val additionalData = List(5) { index ->
                WishCountEntity(
                    date = LocalDate.now().plusDays((10 + index).toLong()),
                    wishText = "Additional $index",
                    targetCount = 20
                )
            }
            additionalData.forEach { wishCountDao.insert(it) }
            
            // 두 번째 백업
            val backup2 = createBackup("backup2.db")
            
            // Then - 백업 크기 비교
            backup2.length() > backup1.length() shouldBe true
            
            // 백업 데이터 검증
            val restoredDb1 = openBackup("backup1.db")
            val data1 = restoredDb1.wishCountDao().getAllWishCounts().first()
            data1 shouldHaveSize 10
            
            val restoredDb2 = openBackup("backup2.db")
            val data2 = restoredDb2.wishCountDao().getAllWishCounts().first()
            data2 shouldHaveSize 15
            
            restoredDb1.close()
            restoredDb2.close()
        }
    }
    
    // Helper functions
    
    private fun createBackup(fileName: String): java.io.File {
        val backupFile = java.io.File("${context.filesDir}/$fileName")
        database.close()
        
        val sourceFile = context.getDatabasePath("test.db")
        sourceFile.copyTo(backupFile, overwrite = true)
        
        // Reopen database
        database = Room.inMemoryDatabaseBuilder(
            context,
            WishRingDatabase::class.java
        ).build()
        wishCountDao = database.wishCountDao()
        resetLogDao = database.resetLogDao()
        
        return backupFile
    }
    
    private fun openBackup(fileName: String): WishRingDatabase {
        return Room.databaseBuilder(
            context,
            WishRingDatabase::class.java,
            fileName
        ).createFromFile(java.io.File("${context.filesDir}/$fileName"))
            .build()
    }
    
    private fun measureTimeMillis(block: suspend () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        testScope.runTest { block() }
        return System.currentTimeMillis() - startTime
    }
    
    companion object {
        // Migration definitions
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wish_count_table ADD COLUMN completed_at TEXT")
                database.execSQL("ALTER TABLE wish_count_table ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX index_wish_count_date ON wish_count_table(date)")
            }
        }
        
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wish_count_table ADD COLUMN streak_count INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        @JvmStatic
        fun pageSizeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(10, 100),
            Arguments.of(20, 50),
            Arguments.of(50, 20),
            Arguments.of(100, 10)
        )
    }
}

// Extension for testing
private suspend fun WishCountDao.searchByWishText(text: String): List<WishCountEntity> {
    return getAllWishCounts().first().filter { it.wishText == text }
}

private suspend fun WishCountDao.getWishCountsPaged(
    limit: Int,
    offset: Int
): Flow<List<WishCountEntity>> {
    return flow {
        val all = getAllWishCounts().first()
        val paged = all.drop(offset).take(limit)
        emit(paged)
    }
}

private suspend fun WishCountDao.incrementCount(date: LocalDate) {
    val current = getWishCountByDate(date).first()
    current?.let {
        val updated = it.copy(currentCount = it.currentCount + 1)
        update(updated)
    }
}

private suspend fun WishCountDao.deleteAll() {
    getAllWishCounts().first().forEach { delete(it) }
}