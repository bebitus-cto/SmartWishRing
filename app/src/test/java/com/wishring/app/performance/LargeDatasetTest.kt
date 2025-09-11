package com.wishring.app.performance

import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Large Dataset Performance 테스트
 * 
 * 대용량 데이터 처리 성능을 검증합니다.
 * 
 * 테스트 영역:
 * 1. Memory Efficiency - 메모리 효율성
 * 2. Query Performance - 쿼리 성능
 * 3. Batch Processing - 배치 처리
 * 4. Stream Processing - 스트림 처리
 * 5. Cache Performance - 캐시 성능
 * 6. Scalability Testing - 확장성 테스트
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Large Dataset Performance 테스트")
class LargeDatasetTest {
    
    private lateinit var testScope: TestScope
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @BeforeEach
    fun setup() {
        testScope = TestScope()
        performanceMonitor = PerformanceMonitor()
    }
    
    @Nested
    @DisplayName("1. Memory Efficiency - 메모리 효율성")
    inner class MemoryEfficiencyTest {
        
        @ParameterizedTest
        @ValueSource(ints = [1000, 10000, 100000, 1000000])
        @DisplayName("대용량 WishCount 객체 메모리 사용량")
        fun testLargeWishCountMemoryUsage(count: Int) = testScope.runTest {
            // Given
            val memoryBefore = getUsedMemory()
            
            // When - 대량 객체 생성
            val wishes = List(count) { index ->
                WishCount(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Wish $index",
                    targetCount = 100,
                    currentCount = Random.nextInt(0, 101)
                )
            }
            
            val memoryAfter = getUsedMemory()
            val memoryUsed = memoryAfter - memoryBefore
            
            // Then - 메모리 사용량 검증
            val bytesPerObject = memoryUsed / count
            println("Objects: $count, Memory: ${memoryUsed / 1024 / 1024}MB, Per object: ${bytesPerObject}B")
            
            // 객체당 1KB 이하 사용
            assert(bytesPerObject < 1024) { "Memory per object too high: ${bytesPerObject}B" }
            
            // Cleanup
            wishes.size // Keep reference to prevent GC
        }
        
        @Test
        @DisplayName("메모리 누수 감지")
        fun testMemoryLeakDetection() = testScope.runTest {
            // Given
            val iterations = 100
            val memorySnapshots = mutableListOf<Long>()
            
            // When - 반복적인 생성/삭제
            repeat(iterations) { iteration ->
                val tempList = List(10000) { index ->
                    WishCount(
                        date = LocalDate.now().plusDays(index.toLong()),
                        wishText = "Temp $index",
                        targetCount = 100
                    )
                }
                
                // 처리 시뮬레이션
                tempList.forEach { it.progress }
                
                // 명시적 참조 해제
                @Suppress("UNUSED_VALUE")
                var cleared = tempList
                cleared = emptyList()
                
                if (iteration % 10 == 0) {
                    System.gc()
                    delay(100)
                    memorySnapshots.add(getUsedMemory())
                }
            }
            
            // Then - 메모리 증가 추세 검증
            val memoryGrowth = memorySnapshots.last() - memorySnapshots.first()
            val growthPerIteration = memoryGrowth / iterations
            
            println("Memory growth: ${memoryGrowth / 1024}KB over $iterations iterations")
            assert(growthPerIteration < 1024) { "Potential memory leak detected" }
        }
        
        @Test
        @DisplayName("Flow 메모리 효율성")
        fun testFlowMemoryEfficiency() = testScope.runTest {
            // Given
            val itemCount = 1000000
            val memoryBefore = getUsedMemory()
            
            // When - Flow로 처리 (lazy evaluation)
            val processedCount = flow {
                repeat(itemCount) { index ->
                    emit(
                        WishCount(
                            date = LocalDate.now().plusDays(index.toLong()),
                            wishText = "Flow Item $index",
                            targetCount = 100,
                            currentCount = index % 101
                        )
                    )
                }
            }
                .filter { it.currentCount > 50 }
                .map { it.progress }
                .count()
            
            val memoryAfter = getUsedMemory()
            val memoryUsed = memoryAfter - memoryBefore
            
            // Then - Flow는 전체 데이터를 메모리에 유지하지 않음
            val expectedMaxMemory = 100 * 1024 * 1024 // 100MB
            assert(memoryUsed < expectedMaxMemory) {
                "Flow used too much memory: ${memoryUsed / 1024 / 1024}MB"
            }
            
            println("Processed $processedCount items using ${memoryUsed / 1024 / 1024}MB")
        }
    }
    
    @Nested
    @DisplayName("2. Query Performance - 쿼리 성능")
    inner class QueryPerformanceTest {
        
        @ParameterizedTest
        @CsvSource(
            "1000,10",
            "10000,50",
            "100000,200",
            "1000000,1000"
        )
        @DisplayName("날짜 범위 쿼리 성능")
        fun testDateRangeQueryPerformance(dataSize: Int, maxQueryTimeMs: Long) = testScope.runTest {
            // Given - 대량 데이터 준비
            val wishes = List(dataSize) { index ->
                WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Query Test $index",
                    targetCount = 100,
                    currentCount = Random.nextInt(0, 101)
                )
            }.sortedBy { it.date }
            
            // When - 다양한 범위 쿼리
            val queryTimes = mutableListOf<Long>()
            val ranges = listOf(7, 30, 90, 365)
            
            ranges.forEach { days ->
                val queryTime = measureTimeMillis {
                    val startDate = LocalDate.now().minusDays(days.toLong())
                    val endDate = LocalDate.now()
                    
                    wishes.filter { wish ->
                        wish.date in startDate..endDate
                    }
                }
                queryTimes.add(queryTime)
            }
            
            // Then
            val avgQueryTime = queryTimes.average()
            println("Data size: $dataSize, Avg query time: ${avgQueryTime}ms")
            
            assert(avgQueryTime < maxQueryTimeMs) {
                "Query too slow: ${avgQueryTime}ms > ${maxQueryTimeMs}ms"
            }
        }
        
        @Test
        @DisplayName("인덱스 활용 검색 성능")
        fun testIndexedSearchPerformance() = testScope.runTest {
            // Given - 인덱스 시뮬레이션
            val dataSize = 100000
            val indexedData = mutableMapOf<LocalDate, WishCount>()
            val listData = mutableListOf<WishCount>()
            
            repeat(dataSize) { index ->
                val wish = WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Indexed $index",
                    targetCount = 100
                )
                indexedData[wish.date] = wish
                listData.add(wish)
            }
            
            // When - 랜덤 검색 1000회
            val searchCount = 1000
            val randomDates = List(searchCount) {
                LocalDate.now().minusDays(Random.nextLong(0, dataSize.toLong()))
            }
            
            val indexedSearchTime = measureNanoTime {
                randomDates.forEach { date ->
                    indexedData[date]
                }
            }
            
            val linearSearchTime = measureNanoTime {
                randomDates.forEach { date ->
                    listData.find { it.date == date }
                }
            }
            
            // Then - 인덱스 검색이 훨씬 빠름
            val speedup = linearSearchTime.toDouble() / indexedSearchTime
            println("Indexed: ${indexedSearchTime / 1_000_000}ms, Linear: ${linearSearchTime / 1_000_000}ms")
            println("Speedup: ${speedup}x")
            
            assert(speedup > 10) { "Index not effective enough: ${speedup}x" }
        }
        
        @Test
        @DisplayName("집계 쿼리 최적화")
        fun testAggregationQueryOptimization() = testScope.runTest {
            // Given
            val dataSize = 100000
            val wishes = List(dataSize) { index ->
                WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Aggregate $index",
                    targetCount = 100,
                    currentCount = Random.nextInt(0, 101)
                )
            }
            
            // When - 다양한 집계 연산
            val aggregationTime = measureTimeMillis {
                val stats = calculateStatistics(wishes)
                
                // 통계 계산
                stats.totalCount
                stats.averageCount
                stats.completionRate
                stats.maxStreak
                stats.percentiles
            }
            
            // Then - 대용량 데이터에서도 빠른 집계
            println("Aggregation time for $dataSize items: ${aggregationTime}ms")
            assert(aggregationTime < 1000) { "Aggregation too slow: ${aggregationTime}ms" }
        }
    }
    
    @Nested
    @DisplayName("3. Batch Processing - 배치 처리")
    inner class BatchProcessingTest {
        
        @ParameterizedTest
        @ValueSource(ints = [100, 500, 1000, 5000])
        @DisplayName("최적 배치 크기 찾기")
        fun testOptimalBatchSize(batchSize: Int) = testScope.runTest {
            // Given
            val totalItems = 100000
            val items = List(totalItems) { index ->
                WishCount(
                    date = LocalDate.now().plusDays(index.toLong()),
                    wishText = "Batch $index",
                    targetCount = 100,
                    currentCount = index % 101
                )
            }
            
            // When - 배치 처리
            val processingTime = measureTimeMillis {
                items.chunked(batchSize).forEach { batch ->
                    processBatch(batch)
                }
            }
            
            val throughput = totalItems.toDouble() / processingTime * 1000
            
            // Then
            println("Batch size: $batchSize, Time: ${processingTime}ms, Throughput: ${throughput.toInt()} items/sec")
            
            performanceMonitor.recordBatchPerformance(batchSize, throughput)
        }
        
        @Test
        @DisplayName("병렬 배치 처리")
        fun testParallelBatchProcessing() = testScope.runTest {
            // Given
            val totalItems = 100000
            val batchSize = 1000
            val items = List(totalItems) { index ->
                DailyRecord(
                    date = LocalDate.now().plusDays(index.toLong()),
                    completedCount = Random.nextInt(0, 101),
                    targetCount = 100
                )
            }
            
            // When - 순차 처리
            val sequentialTime = measureTimeMillis {
                items.chunked(batchSize).forEach { batch ->
                    processDailyRecords(batch)
                }
            }
            
            // 병렬 처리
            val parallelTime = measureTimeMillis {
                coroutineScope {
                    items.chunked(batchSize).map { batch ->
                        async {
                            processDailyRecords(batch)
                        }
                    }.awaitAll()
                }
            }
            
            // Then
            val speedup = sequentialTime.toDouble() / parallelTime
            println("Sequential: ${sequentialTime}ms, Parallel: ${parallelTime}ms, Speedup: ${speedup}x")
            
            assert(speedup > 1.5) { "Parallel processing not effective: ${speedup}x" }
        }
        
        @Test
        @DisplayName("백프레셔 처리")
        fun testBackpressureHandling() = testScope.runTest {
            // Given
            val producer = Channel<WishCount>(capacity = 10)
            val processedCount = AtomicInteger(0)
            val droppedCount = AtomicInteger(0)
            
            // When - 빠른 생산자, 느린 소비자
            val producerJob = launch {
                repeat(10000) { index ->
                    val result = producer.trySend(
                        WishCount(
                            date = LocalDate.now(),
                            wishText = "Item $index",
                            targetCount = 100
                        )
                    )
                    if (!result.isSuccess) {
                        droppedCount.incrementAndGet()
                    }
                }
                producer.close()
            }
            
            val consumerJob = launch {
                for (item in producer) {
                    delay(1) // 느린 처리 시뮬레이션
                    processedCount.incrementAndGet()
                }
            }
            
            producerJob.join()
            consumerJob.join()
            
            // Then
            println("Processed: ${processedCount.get()}, Dropped: ${droppedCount.get()}")
            
            // 백프레셔로 인한 드롭 발생
            assert(droppedCount.get() > 0) { "No backpressure detected" }
        }
    }
    
    @Nested
    @DisplayName("4. Stream Processing - 스트림 처리")
    inner class StreamProcessingTest {
        
        @Test
        @DisplayName("실시간 스트림 처리 성능")
        fun testRealtimeStreamProcessing() = testScope.runTest {
            // Given
            val eventRate = 1000 // events per second
            val duration = 5 // seconds
            val totalEvents = eventRate * duration
            
            val eventStream = flow {
                repeat(totalEvents) { index ->
                    emit(
                        Event(
                            timestamp = System.currentTimeMillis(),
                            type = EventType.COUNT_INCREMENT,
                            data = index
                        )
                    )
                    delay(1000L / eventRate)
                }
            }
            
            val processedEvents = AtomicInteger(0)
            val latencies = ConcurrentLinkedQueue<Long>()
            
            // When
            val processingTime = measureTimeMillis {
                eventStream
                    .buffer(100) // 버퍼링으로 처리 효율 향상
                    .collect { event ->
                        val latency = System.currentTimeMillis() - event.timestamp
                        latencies.add(latency)
                        processedEvents.incrementAndGet()
                    }
            }
            
            // Then
            val avgLatency = latencies.average()
            val maxLatency = latencies.maxOrNull() ?: 0
            val throughput = processedEvents.get().toDouble() / processingTime * 1000
            
            println("Processed: ${processedEvents.get()}, Throughput: ${throughput.toInt()} events/sec")
            println("Avg latency: ${avgLatency}ms, Max latency: ${maxLatency}ms")
            
            assert(avgLatency < 100) { "Latency too high: ${avgLatency}ms" }
        }
        
        @Test
        @DisplayName("윈도우 집계 성능")
        fun testWindowAggregation() = testScope.runTest {
            // Given
            val windowSize = 1000
            val slideSize = 100
            val dataSize = 100000
            
            val dataStream = flow {
                repeat(dataSize) { index ->
                    emit(
                        DataPoint(
                            timestamp = index.toLong(),
                            value = Random.nextInt(0, 100)
                        )
                    )
                }
            }
            
            val windows = mutableListOf<WindowResult>()
            
            // When
            val processingTime = measureTimeMillis {
                dataStream
                    .windowed(windowSize, slideSize) { window ->
                        WindowResult(
                            startTime = window.first().timestamp,
                            endTime = window.last().timestamp,
                            count = window.size,
                            sum = window.sumOf { it.value },
                            average = window.map { it.value }.average()
                        )
                    }
                    .collect { windows.add(it) }
            }
            
            // Then
            val expectedWindows = (dataSize - windowSize) / slideSize + 1
            println("Windows created: ${windows.size}, Time: ${processingTime}ms")
            
            assert(windows.size >= expectedWindows - 1) { 
                "Not enough windows: ${windows.size} < $expectedWindows" 
            }
        }
        
        @Test
        @DisplayName("복잡한 스트림 파이프라인")
        fun testComplexStreamPipeline() = testScope.runTest {
            // Given
            val dataSize = 50000
            val source = flow {
                repeat(dataSize) { index ->
                    emit(
                        UserAction(
                            userId = index % 100,
                            action = if (index % 2 == 0) "increment" else "view",
                            timestamp = System.currentTimeMillis(),
                            value = Random.nextInt(1, 10)
                        )
                    )
                }
            }
            
            // When - 복잡한 변환 파이프라인
            val result = source
                .filter { it.action == "increment" }
                .map { action ->
                    ProcessedAction(
                        userId = action.userId,
                        score = action.value * 2,
                        category = when (action.value) {
                            in 1..3 -> "low"
                            in 4..7 -> "medium"
                            else -> "high"
                        }
                    )
                }
                .groupBy { it.userId }
                .aggregate { userId, actions ->
                    UserSummary(
                        userId = userId,
                        totalScore = actions.sumOf { it.score },
                        actionCount = actions.size,
                        dominantCategory = actions
                            .groupingBy { it.category }
                            .eachCount()
                            .maxByOrNull { it.value }?.key ?: "unknown"
                    )
                }
                .toList()
            
            // Then
            println("Processed ${result.size} user summaries from $dataSize actions")
            assert(result.size <= 100) { "Too many summaries: ${result.size}" }
            
            result.forEach { summary ->
                assert(summary.totalScore > 0) { "Invalid total score" }
                assert(summary.actionCount > 0) { "Invalid action count" }
            }
        }
    }
    
    @Nested
    @DisplayName("5. Cache Performance - 캐시 성능")
    inner class CachePerformanceTest {
        
        @Test
        @DisplayName("LRU 캐시 효율성")
        fun testLRUCacheEfficiency() = testScope.runTest {
            // Given
            val cacheSize = 1000
            val cache = LRUCache<LocalDate, WishCount>(cacheSize)
            val dataSize = 10000
            
            // 접근 패턴 생성 (80/20 rule)
            val hotKeys = List(200) { 
                LocalDate.now().minusDays(it.toLong()) 
            }
            val coldKeys = List(9800) { 
                LocalDate.now().minusDays((200 + it).toLong()) 
            }
            
            var hits = 0
            var misses = 0
            
            // When - 캐시 사용 시뮬레이션
            repeat(100000) { iteration ->
                val key = if (Random.nextFloat() < 0.8) {
                    hotKeys.random()
                } else {
                    coldKeys.random()
                }
                
                val cached = cache.get(key)
                if (cached != null) {
                    hits++
                } else {
                    misses++
                    cache.put(key, WishCount(
                        date = key,
                        wishText = "Cached",
                        targetCount = 100
                    ))
                }
            }
            
            // Then
            val hitRate = hits.toDouble() / (hits + misses)
            println("Cache hit rate: ${(hitRate * 100).toInt()}%")
            
            assert(hitRate > 0.7) { "Cache hit rate too low: ${hitRate}" }
        }
        
        @Test
        @DisplayName("캐시 워밍 성능")
        fun testCacheWarmingPerformance() = testScope.runTest {
            // Given
            val cache = ConcurrentHashMap<LocalDate, WishCount>()
            val dataSize = 10000
            
            val data = List(dataSize) { index ->
                WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Warm $index",
                    targetCount = 100
                )
            }
            
            // When - 캐시 워밍
            val warmingTime = measureTimeMillis {
                coroutineScope {
                    data.chunked(100).map { chunk ->
                        async {
                            chunk.forEach { wish ->
                                cache[wish.date] = wish
                            }
                        }
                    }.awaitAll()
                }
            }
            
            // Then
            println("Cache warming time for $dataSize items: ${warmingTime}ms")
            assert(cache.size == dataSize) { "Cache not fully warmed" }
            assert(warmingTime < 1000) { "Cache warming too slow: ${warmingTime}ms" }
        }
    }
    
    @Nested
    @DisplayName("6. Scalability Testing - 확장성 테스트")
    inner class ScalabilityTest {
        
        @ParameterizedTest
        @ValueSource(ints = [1, 2, 4, 8, 16])
        @DisplayName("동시성 레벨별 확장성")
        fun testConcurrencyScalability(concurrency: Int) = testScope.runTest {
            // Given
            val workPerThread = 10000
            val totalWork = workPerThread * concurrency
            
            // When
            val processingTime = measureTimeMillis {
                coroutineScope {
                    List(concurrency) { threadId ->
                        async(Dispatchers.Default) {
                            repeat(workPerThread) { index ->
                                // CPU 집약적 작업 시뮬레이션
                                val wish = WishCount(
                                    date = LocalDate.now(),
                                    wishText = "Thread $threadId Item $index",
                                    targetCount = 100,
                                    currentCount = index % 101
                                )
                                // 복잡한 계산
                                calculateComplexMetric(wish)
                            }
                        }
                    }.awaitAll()
                }
            }
            
            val throughput = totalWork.toDouble() / processingTime * 1000
            
            // Then
            println("Concurrency: $concurrency, Time: ${processingTime}ms, Throughput: ${throughput.toInt()} ops/sec")
            
            performanceMonitor.recordScalability(concurrency, throughput)
        }
        
        @Test
        @DisplayName("선형 확장성 검증")
        fun testLinearScalability() = testScope.runTest {
            // Given
            val baseConcurrency = 1
            val baseWorkload = 10000
            
            // Single thread baseline
            val baselineTime = measureTimeMillis {
                processWorkload(baseWorkload, baseConcurrency)
            }
            
            // When - 2x, 4x, 8x 확장
            val scalingFactors = listOf(2, 4, 8)
            val scalingResults = mutableMapOf<Int, Double>()
            
            scalingFactors.forEach { factor ->
                val scaledTime = measureTimeMillis {
                    processWorkload(baseWorkload * factor, factor)
                }
                
                val efficiency = (baselineTime.toDouble() * factor) / scaledTime
                scalingResults[factor] = efficiency
                
                println("Scaling factor: $factor, Efficiency: ${(efficiency * 100).toInt()}%")
            }
            
            // Then - 최소 70% 효율성
            scalingResults.values.forEach { efficiency ->
                assert(efficiency > 0.7) { "Poor scaling efficiency: ${efficiency}" }
            }
        }
    }
    
    // Helper classes and functions
    
    private fun getUsedMemory(): Long {
        System.gc()
        Thread.sleep(100)
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
    
    private suspend fun processBatch(batch: List<WishCount>) {
        delay(batch.size.toLong() / 100) // 시뮬레이션
        batch.forEach { it.progress }
    }
    
    private suspend fun processDailyRecords(records: List<DailyRecord>) {
        delay(records.size.toLong() / 100)
        records.forEach { it.completionRate }
    }
    
    private fun calculateStatistics(wishes: List<WishCount>): WishStatistics {
        return WishStatistics(
            totalCount = wishes.sumOf { it.currentCount },
            averageCount = wishes.map { it.currentCount }.average(),
            completionRate = wishes.count { it.isCompleted } * 100.0 / wishes.size,
            maxStreak = calculateMaxStreak(wishes),
            percentiles = calculatePercentiles(wishes.map { it.currentCount })
        )
    }
    
    private fun calculateMaxStreak(wishes: List<WishCount>): Int {
        var maxStreak = 0
        var currentStreak = 0
        
        wishes.sortedBy { it.date }.forEach { wish ->
            if (wish.isCompleted) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }
        
        return maxStreak
    }
    
    private fun calculatePercentiles(values: List<Int>): Map<Int, Double> {
        val sorted = values.sorted()
        return mapOf(
            25 to sorted[sorted.size / 4].toDouble(),
            50 to sorted[sorted.size / 2].toDouble(),
            75 to sorted[sorted.size * 3 / 4].toDouble(),
            95 to sorted[sorted.size * 95 / 100].toDouble()
        )
    }
    
    private fun calculateComplexMetric(wish: WishCount): Double {
        // CPU 집약적 계산 시뮬레이션
        var result = 0.0
        repeat(100) {
            result += Math.sqrt(wish.currentCount.toDouble()) * Math.log(wish.targetCount.toDouble() + 1)
        }
        return result
    }
    
    private suspend fun processWorkload(workload: Int, concurrency: Int) {
        coroutineScope {
            List(concurrency) { 
                async {
                    repeat(workload / concurrency) {
                        delay(1)
                    }
                }
            }.awaitAll()
        }
    }
    
    // Helper classes
    
    data class Event(
        val timestamp: Long,
        val type: EventType,
        val data: Int
    )
    
    enum class EventType {
        COUNT_INCREMENT, COUNT_RESET, WISH_COMPLETE
    }
    
    data class DataPoint(
        val timestamp: Long,
        val value: Int
    )
    
    data class WindowResult(
        val startTime: Long,
        val endTime: Long,
        val count: Int,
        val sum: Int,
        val average: Double
    )
    
    data class UserAction(
        val userId: Int,
        val action: String,
        val timestamp: Long,
        val value: Int
    )
    
    data class ProcessedAction(
        val userId: Int,
        val score: Int,
        val category: String
    )
    
    data class UserSummary(
        val userId: Int,
        val totalScore: Int,
        val actionCount: Int,
        val dominantCategory: String
    )
    
    data class WishStatistics(
        val totalCount: Int,
        val averageCount: Double,
        val completionRate: Double,
        val maxStreak: Int,
        val percentiles: Map<Int, Double>
    )
    
    class LRUCache<K, V>(private val maxSize: Int) {
        private val cache = LinkedHashMap<K, V>(maxSize, 0.75f, true)
        
        fun get(key: K): V? = cache[key]
        
        fun put(key: K, value: V) {
            cache[key] = value
            if (cache.size > maxSize) {
                cache.remove(cache.keys.first())
            }
        }
    }
    
    class PerformanceMonitor {
        private val batchPerformance = mutableMapOf<Int, Double>()
        private val scalabilityMetrics = mutableMapOf<Int, Double>()
        
        fun recordBatchPerformance(batchSize: Int, throughput: Double) {
            batchPerformance[batchSize] = throughput
        }
        
        fun recordScalability(concurrency: Int, throughput: Double) {
            scalabilityMetrics[concurrency] = throughput
        }
        
        fun getOptimalBatchSize(): Int {
            return batchPerformance.maxByOrNull { it.value }?.key ?: 1000
        }
        
        fun getScalabilityReport(): String {
            return scalabilityMetrics.entries.joinToString("\n") { (concurrency, throughput) ->
                "Concurrency: $concurrency, Throughput: ${throughput.toInt()} ops/sec"
            }
        }
    }
}

// Extension functions for Flow

private fun <T> Flow<T>.windowed(size: Int, step: Int, transform: (List<T>) -> T): Flow<T> = flow {
    val buffer = mutableListOf<T>()
    var emitCount = 0
    
    collect { value ->
        buffer.add(value)
        
        if (buffer.size == size) {
            emit(transform(buffer.toList()))
            emitCount++
            
            repeat(step) {
                if (buffer.isNotEmpty()) {
                    buffer.removeAt(0)
                }
            }
        }
    }
}

private fun <T, K> Flow<T>.groupBy(keySelector: (T) -> K): Flow<Pair<K, List<T>>> = flow {
    val groups = mutableMapOf<K, MutableList<T>>()
    
    collect { value ->
        val key = keySelector(value)
        groups.getOrPut(key) { mutableListOf() }.add(value)
    }
    
    groups.forEach { (key, values) ->
        emit(key to values)
    }
}

private fun <K, T, R> Flow<Pair<K, List<T>>>.aggregate(
    aggregator: (K, List<T>) -> R
): Flow<R> = flow {
    collect { (key, values) ->
        emit(aggregator(key, values))
    }
}

private val DailyRecord.completionRate: Double
    get() = if (targetCount > 0) (completedCount.toDouble() / targetCount) * 100 else 0.0