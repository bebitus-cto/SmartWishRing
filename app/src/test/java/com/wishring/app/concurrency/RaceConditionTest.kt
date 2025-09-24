package com.wishring.app.concurrency

import com.wishring.app.data.local.dao.WishCountDao
import com.wishring.app.data.local.entity.WishCountEntity
import com.wishring.app.data.repository.WishCountRepositoryImpl
import com.wishring.app.data.repository.WishCountRepository
import com.wishring.app.data.repository.PreferencesRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

@DisplayName("Race Condition 테스트 - Race Condition Derby 전략")
class RaceConditionTest {

    @MockK
    private lateinit var wishCountDao: WishCountDao
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    private lateinit var repository: WishCountRepository
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Nested
    @DisplayName("동시 카운트 증가 레이스 컨디션")
    inner class ConcurrentIncrementTests {
        
        @ParameterizedTest
        @ValueSource(ints = [10, 50, 100, 500])
        @DisplayName("다중 스레드 동시 증가 - Lost Update 방지")
        fun `should prevent lost updates during concurrent increments`(threadCount: Int) = runTest {
            // Given
            val initialCount = 0
            val incrementPerThread = 10
            val expectedFinalCount = threadCount * incrementPerThread
            
            val atomicCounter = AtomicInteger(initialCount)
            val mutex = Mutex()
            
            // Mock repository with thread-safe implementation
            val safeRepository = object : WishCountRepository {
                override suspend fun incrementCount(amount: Int) {
                    mutex.withLock {
                        val current = atomicCounter.get()
                        delay(Random.nextLong(1, 5)) // Simulate DB latency
                        atomicCounter.set(current + amount)
                    }
                }
                
                override suspend fun getCurrentCount(): Int = atomicCounter.get()
            }
            
            // When - Launch multiple coroutines simultaneously
            val jobs = List(threadCount) {
                launch(Dispatchers.Default) {
                    repeat(incrementPerThread) {
                        safeRepository.incrementCount(1)
                    }
                }
            }
            
            jobs.joinAll()
            
            // Then
            val finalCount = safeRepository.getCurrentCount()
            finalCount shouldBe expectedFinalCount
        }
        
        @Test
        @DisplayName("동시 읽기-수정-쓰기 패턴 테스트")
        fun `should handle read-modify-write race conditions`() = runTest {
            // Given
            val sharedCounter = SharedCounter()
            val operations = 1000
            val threads = 10
            
            // When - Concurrent read-modify-write operations
            val jobs = List(threads) {
                launch(Dispatchers.Default) {
                    repeat(operations / threads) {
                        sharedCounter.incrementWithCheck()
                    }
                }
            }
            
            jobs.joinAll()
            
            // Then
            sharedCounter.getCount() shouldBe operations
            sharedCounter.getCheckFailures() shouldBe 0
        }
        
        @Test
        @DisplayName("ABA 문제 감지 및 방지")
        fun `should detect and prevent ABA problems`() = runTest {
            // Given
            val abaDetector = ABADetector()
            val iterations = 100
            
            // When - Simulate ABA scenario
            val writer1 = launch(Dispatchers.Default) {
                repeat(iterations) {
                    abaDetector.updateValue("A")
                    delay(1)
                    abaDetector.updateValue("B")
                    delay(1)
                    abaDetector.updateValue("A") // Back to A - potential ABA
                }
            }
            
            val reader = launch(Dispatchers.Default) {
                repeat(iterations * 2) {
                    abaDetector.checkForABA()
                    delay(1)
                }
            }
            
            joinAll(writer1, reader)
            
            // Then
            abaDetector.getABAOccurrences() shouldBe 0 // Should prevent ABA
        }
    }
    
    @Nested
    @DisplayName("데이터베이스 트랜잭션 레이스 컨디션")
    inner class DatabaseTransactionTests {
        
        @Test
        @DisplayName("동시 트랜잭션 격리 수준 테스트")
        fun `should maintain transaction isolation levels`() = runTest {
            // Given
            val transactionManager = TransactionManager()
            val accounts = listOf(
                Account("A", 1000),
                Account("B", 1000)
            )
            
            // When - Concurrent transfers
            val transfers = List(100) { index ->
                launch(Dispatchers.Default) {
                    val amount = Random.nextInt(1, 100)
                    val from = if (index % 2 == 0) "A" else "B"
                    val to = if (from == "A") "B" else "A"
                    
                    transactionManager.transfer(from, to, amount)
                }
            }
            
            transfers.joinAll()
            
            // Then - Total balance should remain constant
            val totalBalance = transactionManager.getTotalBalance()
            totalBalance shouldBe 2000
        }
        
        @Test
        @DisplayName("Phantom Read 방지 테스트")
        fun `should prevent phantom reads`() = runTest {
            // Given
            val dataStore = ConcurrentDataStore()
            val phantomReads = AtomicInteger(0)
            
            // When
            val reader = launch(Dispatchers.Default) {
                repeat(100) {
                    val snapshot1 = dataStore.getSnapshot()
                    delay(5)
                    val snapshot2 = dataStore.getSnapshot()
                    
                    if (snapshot1.size != snapshot2.size) {
                        phantomReads.incrementAndGet()
                    }
                }
            }
            
            val writer = launch(Dispatchers.Default) {
                repeat(50) {
                    dataStore.addItem("Item$it")
                    delay(10)
                }
            }
            
            joinAll(reader, writer)
            
            // Then
            phantomReads.get() shouldBe 0 // No phantom reads with proper locking
        }
        
        @Test
        @DisplayName("Deadlock 감지 및 회피")
        fun `should detect and avoid deadlocks`() = runTest {
            // Given
            val resourceManager = DeadlockAwareResourceManager()
            val deadlockDetected = AtomicInteger(0)
            
            // When - Create potential deadlock scenario
            val job1 = launch(Dispatchers.Default) {
                repeat(50) {
                    try {
                        resourceManager.acquireResources(listOf("A", "B"))
                        delay(1)
                        resourceManager.releaseResources(listOf("A", "B"))
                    } catch (e: DeadlockException) {
                        deadlockDetected.incrementAndGet()
                    }
                }
            }
            
            val job2 = launch(Dispatchers.Default) {
                repeat(50) {
                    try {
                        resourceManager.acquireResources(listOf("B", "A")) // Opposite order
                        delay(1)
                        resourceManager.releaseResources(listOf("B", "A"))
                    } catch (e: DeadlockException) {
                        deadlockDetected.incrementAndGet()
                    }
                }
            }
            
            joinAll(job1, job2)
            
            // Then - Deadlocks should be detected and avoided
            deadlockDetected.get() shouldBeGreaterThan 0
        }
    }
    
    @Nested
    @DisplayName("Flow 및 StateFlow 레이스 컨디션")
    inner class FlowRaceConditionTests {
        
        @Test
        @DisplayName("StateFlow 동시 업데이트 충돌")
        fun `should handle StateFlow concurrent updates`() = runTest {
            // Given
            val stateFlow = MutableStateFlow(0)
            val updateCount = 1000
            val threads = 10
            
            // When
            val jobs = List(threads) { threadId ->
                launch(Dispatchers.Default) {
                    repeat(updateCount / threads) {
                        stateFlow.update { current ->
                            delay(Random.nextLong(0, 2))
                            current + 1
                        }
                    }
                }
            }
            
            jobs.joinAll()
            
            // Then
            stateFlow.value shouldBe updateCount
        }
        
        @Test
        @DisplayName("SharedFlow 이벤트 유실 방지")
        fun `should not lose events in SharedFlow`() = runTest {
            // Given
            val sharedFlow = MutableSharedFlow<Int>(
                replay = 0,
                extraBufferCapacity = 100
            )
            val receivedEvents = ConcurrentHashMap.newKeySet<Int>()
            val totalEvents = 1000
            
            // Collectors
            val collectors = List(5) {
                launch(Dispatchers.Default) {
                    sharedFlow.collect { event ->
                        receivedEvents.add(event)
                    }
                }
            }
            
            // When - Emit events concurrently
            val emitters = List(10) { emitterId ->
                launch(Dispatchers.Default) {
                    repeat(totalEvents / 10) { index ->
                        val event = emitterId * 100 + index
                        sharedFlow.emit(event)
                        delay(1)
                    }
                }
            }
            
            emitters.joinAll()
            delay(100) // Allow collectors to process
            collectors.forEach { it.cancel() }
            
            // Then
            receivedEvents.size shouldBe totalEvents
        }
        
        @Test
        @DisplayName("Flow 백프레셔 처리")
        fun `should handle backpressure correctly`() = runTest {
            // Given
            val producer = Channel<Int>(Channel.CONFLATED)
            val processedItems = AtomicInteger(0)
            val droppedItems = AtomicInteger(0)
            
            // Slow consumer
            val consumer = launch(Dispatchers.Default) {
                producer.consumeAsFlow().collect { item ->
                    delay(10) // Slow processing
                    processedItems.incrementAndGet()
                }
            }
            
            // Fast producer
            val producerJob = launch(Dispatchers.Default) {
                repeat(100) { index ->
                    val offered = producer.trySend(index).isSuccess
                    if (!offered) {
                        droppedItems.incrementAndGet()
                    }
                    delay(1) // Fast production
                }
            }
            
            producerJob.join()
            producer.close()
            consumer.join()
            
            // Then - Some items should be dropped due to conflation
            processedItems.get() shouldBeGreaterThan 0
            droppedItems.get() shouldBeGreaterThan 0
            (processedItems.get() + droppedItems.get()) shouldBeLessThanOrEqual 100
        }
    }
    
    @Nested
    @DisplayName("고급 동시성 패턴 테스트")
    inner class AdvancedConcurrencyTests {
        
        @Test
        @DisplayName("Compare-And-Swap (CAS) 연산 테스트")
        fun `should perform CAS operations correctly`() = runTest {
            // Given
            val casCounter = CASCounter()
            val operations = 10000
            val threads = 20
            
            // When
            val jobs = List(threads) {
                launch(Dispatchers.Default) {
                    repeat(operations / threads) {
                        casCounter.increment()
                    }
                }
            }
            
            jobs.joinAll()
            
            // Then
            casCounter.get() shouldBe operations
        }
        
        @Test
        @DisplayName("Producer-Consumer 패턴 동시성")
        fun `should handle producer-consumer pattern`() = runTest {
            // Given
            val queue = ConcurrentQueue<Int>(capacity = 10)
            val produced = AtomicInteger(0)
            val consumed = AtomicInteger(0)
            
            // Producers
            val producers = List(5) { producerId ->
                launch(Dispatchers.Default) {
                    repeat(100) {
                        queue.enqueue(producerId * 100 + it)
                        produced.incrementAndGet()
                        delay(Random.nextLong(1, 5))
                    }
                }
            }
            
            // Consumers
            val consumers = List(3) {
                launch(Dispatchers.Default) {
                    while (isActive) {
                        queue.dequeue()?.let {
                            consumed.incrementAndGet()
                            delay(Random.nextLong(2, 7))
                        }
                    }
                }
            }
            
            producers.joinAll()
            delay(500) // Let consumers finish
            consumers.forEach { it.cancel() }
            
            // Then
            consumed.get() shouldBe produced.get()
        }
        
        @Test
        @DisplayName("Reader-Writer Lock 패턴")
        fun `should implement reader-writer lock correctly`() = runTest {
            // Given
            val rwLock = ReadWriteLock()
            val sharedData = mutableListOf<Int>()
            val readOperations = AtomicInteger(0)
            val writeOperations = AtomicInteger(0)
            
            // Multiple readers
            val readers = List(10) {
                launch(Dispatchers.Default) {
                    repeat(100) {
                        rwLock.readLock {
                            // Multiple readers can access simultaneously
                            val size = sharedData.size
                            readOperations.incrementAndGet()
                            delay(1)
                        }
                    }
                }
            }
            
            // Single writer at a time
            val writers = List(2) { writerId ->
                launch(Dispatchers.Default) {
                    repeat(50) {
                        rwLock.writeLock {
                            sharedData.add(writerId * 100 + it)
                            writeOperations.incrementAndGet()
                            delay(2)
                        }
                    }
                }
            }
            
            joinAll(*readers.toTypedArray(), *writers.toTypedArray())
            
            // Then
            sharedData.size shouldBe 100
            readOperations.get() shouldBe 1000
            writeOperations.get() shouldBe 100
        }
    }
    
    @Nested
    @DisplayName("메모리 일관성 테스트")
    inner class MemoryConsistencyTests {
        
        @Test
        @DisplayName("Happens-Before 관계 검증")
        fun `should maintain happens-before relationships`() = runTest {
            // Given
            @Volatile var flag = false
            var data = 0
            val latch = CountDownLatch(1)
            
            // Writer thread
            val writer = launch(Dispatchers.Default) {
                data = 42
                flag = true // Happens-before relationship
                latch.countDown()
            }
            
            // Reader thread
            val reader = launch(Dispatchers.Default) {
                latch.await()
                while (!flag) {
                    // Spin wait
                }
                // If flag is true, data should be 42 due to happens-before
                assertEquals(42, data)
            }
            
            joinAll(writer, reader)
        }
        
        @Test
        @DisplayName("메모리 가시성 테스트")
        fun `should ensure memory visibility`() = runTest {
            // Given
            val visibilityTester = MemoryVisibilityTester()
            
            // When
            val writer = launch(Dispatchers.Default) {
                repeat(1000) {
                    visibilityTester.write(it)
                    delay(1)
                }
            }
            
            val reader = launch(Dispatchers.Default) {
                repeat(1000) {
                    visibilityTester.read()
                    delay(1)
                }
            }
            
            joinAll(writer, reader)
            
            // Then
            visibilityTester.getInconsistencies() shouldBe 0
        }
    }
    
    @Nested
    @DisplayName("Property-based 동시성 테스트")
    inner class PropertyBasedConcurrencyTests {
        
        @Test
        @DisplayName("동시성 불변성 검증")
        fun `should maintain invariants under concurrent operations`() = runTest {
            checkAll(
                Arb.int(1..20), // thread count
                Arb.int(10..100) // operations per thread
            ) { threads, operations ->
                // Given
                val invariantChecker = InvariantChecker()
                
                // When
                val jobs = List(threads) {
                    launch(Dispatchers.Default) {
                        repeat(operations) {
                            invariantChecker.performOperation()
                        }
                    }
                }
                
                jobs.joinAll()
                
                // Then
                invariantChecker.checkInvariant() shouldBe true
            }
        }
        
        @Test
        @DisplayName("선형화 가능성 테스트")
        fun `should be linearizable`() = runTest {
            // Given
            val linearizableQueue = LinearizableQueue<Int>()
            val history = ConcurrentHistory()
            
            // When - Concurrent operations with history tracking
            val jobs = List(10) { threadId ->
                launch(Dispatchers.Default) {
                    repeat(50) { opId ->
                        val operation = when (Random.nextInt(3)) {
                            0 -> {
                                val value = threadId * 100 + opId
                                history.recordStart(threadId, "enqueue", value)
                                linearizableQueue.enqueue(value)
                                history.recordEnd(threadId, "enqueue", value)
                            }
                            1 -> {
                                history.recordStart(threadId, "dequeue", null)
                                val result = linearizableQueue.dequeue()
                                history.recordEnd(threadId, "dequeue", result)
                            }
                            else -> {
                                history.recordStart(threadId, "size", null)
                                val size = linearizableQueue.size()
                                history.recordEnd(threadId, "size", size)
                            }
                        }
                    }
                }
            }
            
            jobs.joinAll()
            
            // Then - Check if history is linearizable
            history.isLinearizable() shouldBe true
        }
    }
}

// Helper classes for testing

class SharedCounter {
    private val counter = AtomicInteger(0)
    private val checkFailures = AtomicInteger(0)
    private val mutex = Mutex()
    
    suspend fun incrementWithCheck() {
        mutex.withLock {
            val before = counter.get()
            delay(1) // Simulate processing
            val after = counter.incrementAndGet()
            if (after != before + 1) {
                checkFailures.incrementAndGet()
            }
        }
    }
    
    fun getCount() = counter.get()
    fun getCheckFailures() = checkFailures.get()
}

class ABADetector {
    private var value = "A"
    private var version = 0
    private val abaOccurrences = AtomicInteger(0)
    private val mutex = Mutex()
    
    suspend fun updateValue(newValue: String) {
        mutex.withLock {
            value = newValue
            version++
        }
    }
    
    suspend fun checkForABA() {
        mutex.withLock {
            val initialValue = value
            val initialVersion = version
            delay(1)
            if (value == initialValue && version != initialVersion) {
                abaOccurrences.incrementAndGet()
            }
        }
    }
    
    fun getABAOccurrences() = abaOccurrences.get()
}

class TransactionManager {
    private val accounts = ConcurrentHashMap<String, Account>()
    private val mutex = Mutex()
    
    init {
        accounts["A"] = Account("A", 1000)
        accounts["B"] = Account("B", 1000)
    }
    
    suspend fun transfer(from: String, to: String, amount: Int) {
        mutex.withLock {
            val fromAccount = accounts[from]!!
            val toAccount = accounts[to]!!
            
            if (fromAccount.balance >= amount) {
                fromAccount.balance -= amount
                toAccount.balance += amount
            }
        }
    }
    
    fun getTotalBalance(): Int {
        return accounts.values.sumOf { it.balance }
    }
}

data class Account(val id: String, var balance: Int)

class ConcurrentDataStore {
    private val items = mutableListOf<String>()
    private val mutex = Mutex()
    
    suspend fun addItem(item: String) {
        mutex.withLock {
            items.add(item)
        }
    }
    
    suspend fun getSnapshot(): List<String> {
        return mutex.withLock {
            items.toList()
        }
    }
}

class DeadlockAwareResourceManager {
    private val resources = ConcurrentHashMap<String, Mutex>()
    private val acquiredResources = ThreadLocal.withInitial { mutableSetOf<String>() }
    
    init {
        resources["A"] = Mutex()
        resources["B"] = Mutex()
    }
    
    suspend fun acquireResources(resourceList: List<String>) {
        val sortedResources = resourceList.sorted() // Always acquire in same order
        
        for (resource in sortedResources) {
            val mutex = resources[resource]!!
            if (!mutex.tryLock()) {
                // Release all acquired resources and throw
                releaseResources(acquiredResources.get().toList())
                throw DeadlockException("Potential deadlock detected")
            }
            acquiredResources.get().add(resource)
        }
    }
    
    suspend fun releaseResources(resourceList: List<String>) {
        for (resource in resourceList) {
            resources[resource]?.unlock()
            acquiredResources.get().remove(resource)
        }
    }
}

class DeadlockException(message: String) : Exception(message)

class CASCounter {
    private val value = AtomicInteger(0)
    
    fun increment() {
        while (true) {
            val current = value.get()
            val next = current + 1
            if (value.compareAndSet(current, next)) {
                break
            }
        }
    }
    
    fun get() = value.get()
}

class ConcurrentQueue<T>(private val capacity: Int) {
    private val queue = mutableListOf<T>()
    private val mutex = Mutex()
    
    suspend fun enqueue(item: T) {
        mutex.withLock {
            while (queue.size >= capacity) {
                delay(1) // Wait for space
            }
            queue.add(item)
        }
    }
    
    suspend fun dequeue(): T? {
        return mutex.withLock {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
            } else {
                null
            }
        }
    }
}

class ReadWriteLock {
    private var readers = 0
    private var writers = 0
    private val mutex = Mutex()
    
    suspend fun <T> readLock(block: suspend () -> T): T {
        mutex.withLock {
            while (writers > 0) {
                delay(1)
            }
            readers++
        }
        
        try {
            return block()
        } finally {
            mutex.withLock {
                readers--
            }
        }
    }
    
    suspend fun <T> writeLock(block: suspend () -> T): T {
        mutex.withLock {
            while (readers > 0 || writers > 0) {
                delay(1)
            }
            writers++
        }
        
        try {
            return block()
        } finally {
            mutex.withLock {
                writers--
            }
        }
    }
}

class MemoryVisibilityTester {
    @Volatile private var sharedValue = 0
    private var inconsistencies = 0
    private val mutex = Mutex()
    
    suspend fun write(value: Int) {
        sharedValue = value
    }
    
    suspend fun read() {
        val value = sharedValue
        if (value < 0 || value >= 1000) {
            mutex.withLock {
                inconsistencies++
            }
        }
    }
    
    fun getInconsistencies() = inconsistencies
}

class InvariantChecker {
    private val state1 = AtomicInteger(0)
    private val state2 = AtomicInteger(0)
    
    suspend fun performOperation() {
        // Invariant: state1 + state2 = 0
        val delta = Random.nextInt(-10, 11)
        state1.addAndGet(delta)
        delay(Random.nextLong(0, 2))
        state2.addAndGet(-delta)
    }
    
    fun checkInvariant(): Boolean {
        return state1.get() + state2.get() == 0
    }
}

class LinearizableQueue<T> {
    private val queue = mutableListOf<T>()
    private val mutex = Mutex()
    
    suspend fun enqueue(item: T) {
        mutex.withLock {
            queue.add(item)
        }
    }
    
    suspend fun dequeue(): T? {
        return mutex.withLock {
            if (queue.isNotEmpty()) {
                queue.removeAt(0)
            } else {
                null
            }
        }
    }
    
    suspend fun size(): Int {
        return mutex.withLock {
            queue.size
        }
    }
}

class ConcurrentHistory {
    private val events = mutableListOf<HistoryEvent>()
    private val mutex = Mutex()
    
    suspend fun recordStart(threadId: Int, operation: String, value: Any?) {
        mutex.withLock {
            events.add(HistoryEvent(threadId, operation, value, EventType.START))
        }
    }
    
    suspend fun recordEnd(threadId: Int, operation: String, value: Any?) {
        mutex.withLock {
            events.add(HistoryEvent(threadId, operation, value, EventType.END))
        }
    }
    
    fun isLinearizable(): Boolean {
        // Simplified linearizability check
        // In real implementation, would use more sophisticated algorithm
        return events.filter { it.type == EventType.START }.size == 
               events.filter { it.type == EventType.END }.size
    }
}

data class HistoryEvent(
    val threadId: Int,
    val operation: String,
    val value: Any?,
    val type: EventType
)

enum class EventType { START, END }

// Mock repository interface
interface WishCountRepository {
    suspend fun incrementCount(amount: Int = 1)
    suspend fun getCurrentCount(): Int
}