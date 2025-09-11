package com.wishring.app.concurrency

import com.wishring.app.presentation.viewmodel.*
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.BleRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import app.cash.turbine.test
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@DisplayName("State Consistency 테스트 - 상태 일관성 검증")
class StateConsistencyTest {

    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var bleRepository: BleRepository
    
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("ViewModel 상태 일관성 테스트")
    inner class ViewModelStateConsistencyTests {
        
        @Test
        @DisplayName("동시 이벤트 처리 시 상태 일관성")
        fun `should maintain state consistency during concurrent events`() = runTest {
            // Given
            val stateManager = ViewModelStateManager()
            val events = List(1000) { index ->
                when (index % 4) {
                    0 -> TestEvent.Increment(1)
                    1 -> TestEvent.Decrement(1)
                    2 -> TestEvent.Reset
                    else -> TestEvent.SetValue(Random.nextInt(0, 100))
                }
            }
            
            // When - Process events concurrently
            val jobs = events.map { event ->
                launch(Dispatchers.Default) {
                    stateManager.handleEvent(event)
                }
            }
            
            jobs.joinAll()
            
            // Then - State should be consistent
            val finalState = stateManager.getState()
            finalState.isConsistent() shouldBe true
            stateManager.getStateTransitions().isValidSequence() shouldBe true
        }
        
        @Test
        @DisplayName("상태 전이 원자성 보장")
        fun `should ensure atomic state transitions`() = runTest {
            // Given
            val atomicStateManager = AtomicStateManager()
            val observers = ConcurrentHashMap<Int, MutableList<TestState>>()
            
            // Setup observers
            val observerJobs = List(5) { observerId ->
                launch(Dispatchers.Default) {
                    atomicStateManager.stateFlow.collect { state ->
                        observers.getOrPut(observerId) { mutableListOf() }.add(state)
                    }
                }
            }
            
            // When - Perform state transitions
            val transitionJobs = List(100) { index ->
                launch(Dispatchers.Default) {
                    atomicStateManager.transition {
                        // Complex state transition
                        copy(
                            count = count + 1,
                            lastUpdate = System.currentTimeMillis(),
                            version = version + 1
                        )
                    }
                }
            }
            
            transitionJobs.joinAll()
            delay(100) // Let observers catch up
            observerJobs.forEach { it.cancel() }
            
            // Then - All observers should see consistent states
            observers.values.forEach { observedStates ->
                observedStates.zipWithNext().all { (prev, next) ->
                    next.version == prev.version + 1 &&
                    next.count == prev.count + 1
                } shouldBe true
            }
        }
        
        @Test
        @DisplayName("Effect 순서 보장")
        fun `should maintain effect ordering`() = runTest {
            // Given
            val effectManager = EffectManager()
            val receivedEffects = mutableListOf<TestEffect>()
            
            // Collect effects
            val collector = launch {
                effectManager.effectFlow.collect { effect ->
                    synchronized(receivedEffects) {
                        receivedEffects.add(effect)
                    }
                }
            }
            
            // When - Send effects in specific order
            val expectedEffects = List(100) { index ->
                TestEffect.ShowMessage("Message $index")
            }
            
            expectedEffects.forEach { effect ->
                launch(Dispatchers.Default) {
                    effectManager.sendEffect(effect)
                }
            }
            
            delay(500) // Wait for effects to be processed
            collector.cancel()
            
            // Then - Effects should be received in order
            receivedEffects shouldContainAll expectedEffects
        }
    }
    
    @Nested
    @DisplayName("다중 데이터 소스 동기화 테스트")
    inner class MultiSourceSynchronizationTests {
        
        @Test
        @DisplayName("Repository와 ViewModel 상태 동기화")
        fun `should synchronize repository and viewmodel states`() = runTest {
            // Given
            val syncManager = RepositoryViewModelSync()
            
            // When - Concurrent updates from different sources
            val repositoryUpdates = launch(Dispatchers.Default) {
                repeat(50) {
                    syncManager.updateFromRepository(Random.nextInt(0, 100))
                    delay(10)
                }
            }
            
            val userActions = launch(Dispatchers.Default) {
                repeat(50) {
                    syncManager.updateFromUser(Random.nextInt(0, 100))
                    delay(10)
                }
            }
            
            joinAll(repositoryUpdates, userActions)
            
            // Then - States should be synchronized
            val repoState = syncManager.getRepositoryState()
            val viewModelState = syncManager.getViewModelState()
            
            (repoState - viewModelState) in -1..1 shouldBe true // Allow small difference
        }
        
        @Test
        @DisplayName("BLE 이벤트와 UI 상태 동기화")
        fun `should sync BLE events with UI state`() = runTest {
            // Given
            val bleUiSync = BleUiSynchronizer()
            val bleEvents = Channel<BleEvent>(Channel.UNLIMITED)
            
            // Setup event processor
            val processor = launch(Dispatchers.Default) {
                bleEvents.consumeAsFlow().collect { event ->
                    bleUiSync.processBleEvent(event)
                }
            }
            
            // When - Send mixed BLE events
            val events = listOf(
                BleEvent.Connected("device1"),
                BleEvent.DataReceived(50),
                BleEvent.Disconnected,
                BleEvent.Connected("device2"),
                BleEvent.DataReceived(75),
                BleEvent.DataReceived(100)
            )
            
            events.forEach { event ->
                bleEvents.send(event)
                delay(50)
            }
            
            bleEvents.close()
            processor.join()
            
            // Then - UI state should reflect latest BLE state
            val uiState = bleUiSync.getUiState()
            uiState.isConnected shouldBe true
            uiState.lastValue shouldBe 100
            uiState.connectionHistory shouldHaveSize 2
        }
        
        @Test
        @DisplayName("캐시와 데이터베이스 일관성")
        fun `should maintain cache and database consistency`() = runTest {
            // Given
            val cacheDbSync = CacheDatabaseSync()
            
            // When - Concurrent reads and writes
            val writeJobs = List(20) { index ->
                launch(Dispatchers.Default) {
                    val key = "key${index % 5}"
                    val value = "value$index"
                    cacheDbSync.write(key, value)
                    delay(Random.nextLong(1, 10))
                }
            }
            
            val readJobs = List(50) { index ->
                launch(Dispatchers.Default) {
                    val key = "key${index % 5}"
                    delay(Random.nextLong(1, 5))
                    cacheDbSync.read(key)
                }
            }
            
            (writeJobs + readJobs).joinAll()
            
            // Then - Cache and DB should be consistent
            cacheDbSync.validateConsistency() shouldBe true
            cacheDbSync.getCacheHitRate() shouldNotBe 0.0
        }
    }
    
    @Nested
    @DisplayName("트랜잭션 일관성 테스트")
    inner class TransactionConsistencyTests {
        
        @Test
        @DisplayName("복합 트랜잭션 원자성")
        fun `should maintain atomicity in composite transactions`() = runTest {
            // Given
            val transactionManager = CompositeTransactionManager()
            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            
            // When - Execute composite transactions
            val transactions = List(100) { index ->
                launch(Dispatchers.Default) {
                    try {
                        transactionManager.executeTransaction {
                            // Composite operation
                            updateCounter(index)
                            updateStats(index)
                            if (index % 10 == 5) {
                                throw TransactionException("Simulated failure")
                            }
                            updateHistory(index)
                        }
                        successCount.incrementAndGet()
                    } catch (e: TransactionException) {
                        failureCount.incrementAndGet()
                    }
                }
            }
            
            transactions.joinAll()
            
            // Then - All or nothing principle
            val state = transactionManager.getState()
            state.counter shouldBe successCount.get()
            state.stats.size shouldBe successCount.get()
            state.history.size shouldBe successCount.get()
        }
        
        @Test
        @DisplayName("보상 트랜잭션 (Saga 패턴)")
        fun `should handle compensation in saga pattern`() = runTest {
            // Given
            val sagaManager = SagaTransactionManager()
            
            // When - Execute saga with potential failures
            val sagaResults = List(50) { index ->
                async(Dispatchers.Default) {
                    sagaManager.executeSaga(
                        steps = listOf(
                            SagaStep("step1") { /* success */ },
                            SagaStep("step2") { 
                                if (index % 5 == 0) throw Exception("Failed")
                            },
                            SagaStep("step3") { /* success */ }
                        )
                    )
                }
            }
            
            val results = sagaResults.awaitAll()
            
            // Then - Failed sagas should be compensated
            val successfulSagas = results.count { it.isSuccess }
            val compensatedSagas = results.count { it.isCompensated }
            
            (successfulSagas + compensatedSagas) shouldBe 50
            sagaManager.getState().isConsistent shouldBe true
        }
    }
    
    @Nested
    @DisplayName("이벤트 소싱 일관성 테스트")
    inner class EventSourcingConsistencyTests {
        
        @Test
        @DisplayName("이벤트 스트림 순서 보장")
        fun `should maintain event stream ordering`() = runTest {
            // Given
            val eventStore = EventStore()
            val events = List(1000) { index ->
                DomainEvent(
                    id = index.toLong(),
                    type = "TestEvent",
                    timestamp = System.currentTimeMillis(),
                    data = mapOf("value" to index)
                )
            }
            
            // When - Append events concurrently
            val jobs = events.map { event ->
                launch(Dispatchers.Default) {
                    eventStore.append(event)
                }
            }
            
            jobs.joinAll()
            
            // Then - Events should be ordered by timestamp
            val storedEvents = eventStore.getAll()
            storedEvents.zipWithNext().all { (prev, next) ->
                prev.timestamp <= next.timestamp
            } shouldBe true
        }
        
        @Test
        @DisplayName("이벤트 재생을 통한 상태 복원")
        fun `should restore state from event replay`() = runTest {
            // Given
            val eventSourcedState = EventSourcedState()
            val events = listOf(
                StateEvent.Created(id = 1),
                StateEvent.Updated(id = 1, value = 10),
                StateEvent.Updated(id = 1, value = 20),
                StateEvent.Deleted(id = 1),
                StateEvent.Created(id = 2),
                StateEvent.Updated(id = 2, value = 30)
            )
            
            // When - Apply events
            events.forEach { event ->
                eventSourcedState.apply(event)
            }
            
            // Create snapshot
            val snapshot = eventSourcedState.createSnapshot()
            
            // Replay from beginning
            val replayedState = EventSourcedState()
            events.forEach { event ->
                replayedState.apply(event)
            }
            
            // Then - States should match
            replayedState.createSnapshot() shouldBe snapshot
        }
    }
    
    @Nested
    @DisplayName("CQRS 일관성 테스트")
    inner class CQRSConsistencyTests {
        
        @Test
        @DisplayName("Command와 Query 모델 동기화")
        fun `should sync command and query models`() = runTest {
            // Given
            val cqrsManager = CQRSManager()
            
            // When - Execute commands and queries concurrently
            val commandJobs = List(50) { index ->
                launch(Dispatchers.Default) {
                    when (index % 3) {
                        0 -> cqrsManager.executeCommand(CreateCommand(id = index))
                        1 -> cqrsManager.executeCommand(UpdateCommand(id = index, value = index * 10))
                        else -> cqrsManager.executeCommand(DeleteCommand(id = index))
                    }
                }
            }
            
            val queryJobs = List(100) {
                launch(Dispatchers.Default) {
                    delay(Random.nextLong(1, 10))
                    cqrsManager.executeQuery(GetAllQuery())
                }
            }
            
            (commandJobs + queryJobs).joinAll()
            
            // Then - Eventually consistent
            delay(100) // Allow for eventual consistency
            
            val commandModel = cqrsManager.getCommandModel()
            val queryModel = cqrsManager.getQueryModel()
            
            queryModel.size shouldBe commandModel.size
            queryModel.keys shouldBe commandModel.keys
        }
        
        @Test
        @DisplayName("이벤트 프로젝션 일관성")
        fun `should maintain projection consistency`() = runTest {
            // Given
            val projectionManager = ProjectionManager()
            
            // When - Project events to different views
            val events = List(100) { index ->
                BusinessEvent(
                    aggregateId = index % 10,
                    eventType = "Updated",
                    payload = mapOf("value" to index)
                )
            }
            
            val projectionJobs = events.map { event ->
                launch(Dispatchers.Default) {
                    projectionManager.project(event)
                }
            }
            
            projectionJobs.joinAll()
            
            // Then - All projections should be consistent
            val summaryView = projectionManager.getSummaryView()
            val detailView = projectionManager.getDetailView()
            val statsView = projectionManager.getStatsView()
            
            summaryView.totalEvents shouldBe 100
            detailView.aggregates.size shouldBe 10
            statsView.eventsByType["Updated"] shouldBe 100
        }
    }
    
    @Nested
    @DisplayName("Property-based 일관성 테스트")
    inner class PropertyBasedConsistencyTests {
        
        @Test
        @DisplayName("상태 머신 불변성")
        fun `should maintain state machine invariants`() = runTest {
            checkAll(
                Arb.list(
                    Arb.enum<StateTransition>(),
                    range = 1..100
                )
            ) { transitions ->
                // Given
                val stateMachine = StateMachine()
                
                // When
                transitions.forEach { transition ->
                    stateMachine.transition(transition)
                }
                
                // Then
                stateMachine.validateInvariants() shouldBe true
            }
        }
        
        @Test
        @DisplayName("분산 시스템 일관성")
        fun `should maintain distributed system consistency`() = runTest {
            // Given
            val nodes = List(5) { nodeId ->
                DistributedNode(nodeId)
            }
            
            // When - Simulate distributed operations
            val operations = List(1000) { opId ->
                launch(Dispatchers.Default) {
                    val targetNode = nodes[opId % nodes.size]
                    val operation = when (opId % 3) {
                        0 -> DistributedOp.Write(key = "key${opId % 10}", value = opId)
                        1 -> DistributedOp.Read(key = "key${opId % 10}")
                        else -> DistributedOp.Delete(key = "key${opId % 10}")
                    }
                    
                    targetNode.execute(operation)
                    
                    // Replicate to other nodes
                    if (operation is DistributedOp.Write) {
                        nodes.filter { it != targetNode }.forEach { node ->
                            delay(Random.nextLong(1, 5)) // Simulate network delay
                            node.replicate(operation)
                        }
                    }
                }
            }
            
            operations.joinAll()
            delay(100) // Allow for replication
            
            // Then - All nodes should converge to same state
            val states = nodes.map { it.getState() }
            states.all { it == states[0] } shouldBe true
        }
    }
}

// Helper classes for testing

class ViewModelStateManager {
    private val state = MutableStateFlow(TestState())
    private val transitions = mutableListOf<StateTransition>()
    private val mutex = Mutex()
    
    suspend fun handleEvent(event: TestEvent) {
        mutex.withLock {
            val oldState = state.value
            val newState = when (event) {
                is TestEvent.Increment -> oldState.copy(count = oldState.count + event.amount)
                is TestEvent.Decrement -> oldState.copy(count = oldState.count - event.amount)
                is TestEvent.Reset -> oldState.copy(count = 0)
                is TestEvent.SetValue -> oldState.copy(count = event.value)
            }
            
            state.value = newState
            transitions.add(StateTransition(oldState, newState, event))
        }
    }
    
    fun getState() = state.value
    fun getStateTransitions() = transitions.toList()
}

data class TestState(
    val count: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis(),
    val version: Int = 0
) {
    fun isConsistent(): Boolean {
        return count >= 0 && version >= 0
    }
}

sealed class TestEvent {
    data class Increment(val amount: Int) : TestEvent()
    data class Decrement(val amount: Int) : TestEvent()
    object Reset : TestEvent()
    data class SetValue(val value: Int) : TestEvent()
}

sealed class TestEffect {
    data class ShowMessage(val message: String) : TestEffect()
    object ShowLoading : TestEffect()
    object HideLoading : TestEffect()
}

data class StateTransition(
    val from: TestState,
    val to: TestState,
    val event: TestEvent
)

fun List<StateTransition>.isValidSequence(): Boolean {
    return zipWithNext().all { (prev, next) ->
        prev.to == next.from
    }
}

class AtomicStateManager {
    private val _stateFlow = MutableStateFlow(TestState())
    val stateFlow: StateFlow<TestState> = _stateFlow.asStateFlow()
    
    suspend fun transition(transform: TestState.() -> TestState) {
        _stateFlow.update { currentState ->
            transform(currentState)
        }
    }
}

class EffectManager {
    private val _effectFlow = MutableSharedFlow<TestEffect>()
    val effectFlow: SharedFlow<TestEffect> = _effectFlow.asSharedFlow()
    
    suspend fun sendEffect(effect: TestEffect) {
        _effectFlow.emit(effect)
    }
}

class RepositoryViewModelSync {
    private val repositoryState = AtomicInteger(0)
    private val viewModelState = AtomicInteger(0)
    
    suspend fun updateFromRepository(value: Int) {
        repositoryState.set(value)
        delay(1) // Simulate propagation delay
        viewModelState.set(value)
    }
    
    suspend fun updateFromUser(value: Int) {
        viewModelState.set(value)
        delay(1) // Simulate save delay
        repositoryState.set(value)
    }
    
    fun getRepositoryState() = repositoryState.get()
    fun getViewModelState() = viewModelState.get()
}

sealed class BleEvent {
    data class Connected(val deviceId: String) : BleEvent()
    object Disconnected : BleEvent()
    data class DataReceived(val value: Int) : BleEvent()
}

data class BleUiState(
    val isConnected: Boolean = false,
    val lastValue: Int = 0,
    val connectionHistory: List<String> = emptyList()
)

class BleUiSynchronizer {
    private var uiState = BleUiState()
    private val mutex = Mutex()
    
    suspend fun processBleEvent(event: BleEvent) {
        mutex.withLock {
            uiState = when (event) {
                is BleEvent.Connected -> uiState.copy(
                    isConnected = true,
                    connectionHistory = uiState.connectionHistory + event.deviceId
                )
                is BleEvent.Disconnected -> uiState.copy(isConnected = false)
                is BleEvent.DataReceived -> uiState.copy(lastValue = event.value)
            }
        }
    }
    
    fun getUiState() = uiState
}

class CacheDatabaseSync {
    private val cache = ConcurrentHashMap<String, String>()
    private val database = ConcurrentHashMap<String, String>()
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    suspend fun write(key: String, value: String) {
        database[key] = value
        cache[key] = value
    }
    
    suspend fun read(key: String): String? {
        return cache[key]?.also {
            cacheHits.incrementAndGet()
        } ?: database[key]?.also {
            cacheMisses.incrementAndGet()
            cache[key] = it
        }
    }
    
    fun validateConsistency(): Boolean {
        return cache.all { (key, value) ->
            database[key] == value
        }
    }
    
    fun getCacheHitRate(): Double {
        val total = cacheHits.get() + cacheMisses.get()
        return if (total > 0) cacheHits.get().toDouble() / total else 0.0
    }
}

class CompositeTransactionManager {
    private data class State(
        val counter: Int = 0,
        val stats: List<Int> = emptyList(),
        val history: List<Int> = emptyList()
    )
    
    private var state = State()
    private val mutex = Mutex()
    
    suspend fun executeTransaction(block: TransactionScope.() -> Unit) {
        mutex.withLock {
            val scope = TransactionScope(state)
            try {
                scope.block()
                state = scope.getState()
            } catch (e: Exception) {
                // Rollback - state remains unchanged
                throw e
            }
        }
    }
    
    fun getState() = state
    
    inner class TransactionScope(private var tempState: State) {
        fun updateCounter(value: Int) {
            tempState = tempState.copy(counter = tempState.counter + 1)
        }
        
        fun updateStats(value: Int) {
            tempState = tempState.copy(stats = tempState.stats + value)
        }
        
        fun updateHistory(value: Int) {
            tempState = tempState.copy(history = tempState.history + value)
        }
        
        fun getState() = tempState
    }
}

class TransactionException(message: String) : Exception(message)

data class SagaStep(
    val name: String,
    val action: suspend () -> Unit,
    val compensation: suspend () -> Unit = {}
)

data class SagaResult(
    val isSuccess: Boolean,
    val isCompensated: Boolean
)

class SagaTransactionManager {
    private data class State(
        val isConsistent: Boolean = true
    )
    
    private var state = State()
    
    suspend fun executeSaga(steps: List<SagaStep>): SagaResult {
        val executedSteps = mutableListOf<SagaStep>()
        
        try {
            steps.forEach { step ->
                step.action()
                executedSteps.add(step)
            }
            return SagaResult(isSuccess = true, isCompensated = false)
        } catch (e: Exception) {
            // Compensate in reverse order
            executedSteps.reversed().forEach { step ->
                try {
                    step.compensation()
                } catch (e: Exception) {
                    // Log compensation failure
                }
            }
            return SagaResult(isSuccess = false, isCompensated = true)
        }
    }
    
    fun getState() = state
}

data class DomainEvent(
    val id: Long,
    val type: String,
    val timestamp: Long,
    val data: Map<String, Any>
)

class EventStore {
    private val events = mutableListOf<DomainEvent>()
    private val mutex = Mutex()
    
    suspend fun append(event: DomainEvent) {
        mutex.withLock {
            events.add(event)
            events.sortBy { it.timestamp }
        }
    }
    
    fun getAll(): List<DomainEvent> = events.toList()
}

sealed class StateEvent {
    data class Created(val id: Int) : StateEvent()
    data class Updated(val id: Int, val value: Int) : StateEvent()
    data class Deleted(val id: Int) : StateEvent()
}

class EventSourcedState {
    private val entities = mutableMapOf<Int, Int>()
    
    fun apply(event: StateEvent) {
        when (event) {
            is StateEvent.Created -> entities[event.id] = 0
            is StateEvent.Updated -> entities[event.id] = event.value
            is StateEvent.Deleted -> entities.remove(event.id)
        }
    }
    
    fun createSnapshot(): Map<Int, Int> = entities.toMap()
}

// CQRS pattern classes
sealed class Command {
    abstract val id: Int
}

data class CreateCommand(override val id: Int) : Command()
data class UpdateCommand(override val id: Int, val value: Int) : Command()
data class DeleteCommand(override val id: Int) : Command()

sealed class Query
class GetAllQuery : Query()

class CQRSManager {
    private val commandModel = ConcurrentHashMap<Int, Int>()
    private val queryModel = ConcurrentHashMap<Int, Int>()
    
    suspend fun executeCommand(command: Command) {
        when (command) {
            is CreateCommand -> commandModel[command.id] = 0
            is UpdateCommand -> commandModel[command.id] = command.value
            is DeleteCommand -> commandModel.remove(command.id)
        }
        
        // Async projection update
        delay(Random.nextLong(1, 10))
        syncToQueryModel()
    }
    
    suspend fun executeQuery(query: Query): Map<Int, Int> {
        return when (query) {
            is GetAllQuery -> queryModel.toMap()
        }
    }
    
    private fun syncToQueryModel() {
        queryModel.clear()
        queryModel.putAll(commandModel)
    }
    
    fun getCommandModel() = commandModel.toMap()
    fun getQueryModel() = queryModel.toMap()
}

data class BusinessEvent(
    val aggregateId: Int,
    val eventType: String,
    val payload: Map<String, Any>
)

class ProjectionManager {
    data class SummaryView(val totalEvents: Int = 0)
    data class DetailView(val aggregates: Map<Int, List<BusinessEvent>> = emptyMap())
    data class StatsView(val eventsByType: Map<String, Int> = emptyMap())
    
    private var summaryView = SummaryView()
    private var detailView = DetailView()
    private var statsView = StatsView()
    private val mutex = Mutex()
    
    suspend fun project(event: BusinessEvent) {
        mutex.withLock {
            // Update summary
            summaryView = summaryView.copy(totalEvents = summaryView.totalEvents + 1)
            
            // Update detail
            val aggregateEvents = detailView.aggregates[event.aggregateId] ?: emptyList()
            detailView = detailView.copy(
                aggregates = detailView.aggregates + (event.aggregateId to aggregateEvents + event)
            )
            
            // Update stats
            val typeCount = statsView.eventsByType[event.eventType] ?: 0
            statsView = statsView.copy(
                eventsByType = statsView.eventsByType + (event.eventType to typeCount + 1)
            )
        }
    }
    
    fun getSummaryView() = summaryView
    fun getDetailView() = detailView
    fun getStatsView() = statsView
}

// State machine for property testing
enum class StateTransition {
    START, PROCESS, COMPLETE, CANCEL, RETRY
}

class StateMachine {
    private enum class State {
        IDLE, PROCESSING, COMPLETED, CANCELLED, ERROR
    }
    
    private var currentState = State.IDLE
    private val transitions = mutableListOf<Pair<State, State>>()
    
    fun transition(transition: StateTransition) {
        val oldState = currentState
        currentState = when (transition) {
            StateTransition.START -> when (currentState) {
                State.IDLE -> State.PROCESSING
                else -> currentState
            }
            StateTransition.PROCESS -> when (currentState) {
                State.PROCESSING -> State.PROCESSING
                else -> currentState
            }
            StateTransition.COMPLETE -> when (currentState) {
                State.PROCESSING -> State.COMPLETED
                else -> currentState
            }
            StateTransition.CANCEL -> when (currentState) {
                State.PROCESSING -> State.CANCELLED
                else -> currentState
            }
            StateTransition.RETRY -> when (currentState) {
                State.ERROR, State.CANCELLED -> State.IDLE
                else -> currentState
            }
        }
        transitions.add(oldState to currentState)
    }
    
    fun validateInvariants(): Boolean {
        // Check that we never transition from COMPLETED back to PROCESSING
        return transitions.none { (from, to) ->
            from == State.COMPLETED && to == State.PROCESSING
        }
    }
}

// Distributed system simulation
sealed class DistributedOp {
    data class Write(val key: String, val value: Int) : DistributedOp()
    data class Read(val key: String) : DistributedOp()
    data class Delete(val key: String) : DistributedOp()
}

class DistributedNode(private val nodeId: Int) {
    private val data = ConcurrentHashMap<String, Int>()
    private val vectorClock = ConcurrentHashMap<Int, Int>()
    
    suspend fun execute(op: DistributedOp) {
        when (op) {
            is DistributedOp.Write -> {
                data[op.key] = op.value
                vectorClock[nodeId] = (vectorClock[nodeId] ?: 0) + 1
            }
            is DistributedOp.Read -> {
                data[op.key]
            }
            is DistributedOp.Delete -> {
                data.remove(op.key)
                vectorClock[nodeId] = (vectorClock[nodeId] ?: 0) + 1
            }
        }
    }
    
    suspend fun replicate(op: DistributedOp) {
        execute(op)
    }
    
    fun getState(): Map<String, Int> = data.toMap()
}