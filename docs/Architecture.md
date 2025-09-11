# 🏗️ WISH RING Android 앱 아키텍처 문서

## 📚 목차
1. [아키텍처 개요](#아키텍처-개요)
2. [Clean Architecture 원칙](#clean-architecture-원칙)
3. [MVVM + MVI 패턴](#mvvm--mvi-패턴)
4. [레이어 상세 설명](#레이어-상세-설명)
5. [데이터 플로우](#데이터-플로우)
6. [의존성 규칙](#의존성-규칙)
7. [상태 관리](#상태-관리)
8. [BLE 통신 아키텍처](#ble-통신-아키텍처)
9. [에러 처리 전략](#에러-처리-전략)
10. [테스트 전략](#테스트-전략)

---

## 🎯 아키텍처 개요

WISH RING 앱은 **Clean Architecture**와 **MVVM (Model-View-ViewModel)** 패턴을 결합하여 설계되었습니다. 추가로 **MVI (Model-View-Intent)** 패턴의 단방향 데이터 플로우를 적용하여 예측 가능한 상태 관리를 구현합니다.

### 핵심 원칙
- **관심사의 분리 (Separation of Concerns)**
- **의존성 역전 (Dependency Inversion)**
- **단일 책임 원칙 (Single Responsibility)**
- **단방향 데이터 플로우 (Unidirectional Data Flow)**
- **불변 상태 (Immutable State)**

---

## 🧱 Clean Architecture 원칙

### 레이어 구조

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  (UI Components, ViewModels, States)        │
│         [Feature-based Structure]           │
├─────────────────────────────────────────────┤
│             Domain Layer                    │
│  (Use Cases, Repository Interfaces,         │
│   Business Models)                          │
├─────────────────────────────────────────────┤
│              Data Layer                     │
│  (Repository Implementations, Data Sources, │
│   API, Database, BLE)                       │
└─────────────────────────────────────────────┘
```

### 레이어별 책임

#### 1. Presentation Layer (Feature-based Architecture)
- **책임**: UI 렌더링, 사용자 입력 처리
- **구조**: Feature별로 ViewModel과 UI를 함께 관리
- **컴포넌트**: 
  - Jetpack Compose UI (Screen, Components)
  - ViewModels
  - ViewStates
  - Events & Effects
  - Theme System (Material3)
- **의존성**: Domain Layer만 참조
- **특징**: 
  - 각 Feature 폴더가 독립적인 모듈처럼 동작
  - Feature 내부에서 ViewModel과 UI가 긴밀하게 연결
  - 공통 컴포넌트는 별도 폴더에서 관리

#### 2. Domain Layer  
- **책임**: 비즈니스 로직, 비즈니스 규칙
- **컴포넌트**:
  - Use Cases
  - Repository Interfaces
  - Domain Models
- **의존성**: 없음 (완전 독립적)

#### 3. Data Layer
- **책임**: 데이터 제공, 외부 시스템 통신
- **컴포넌트**:
  - Repository Implementations
  - Local Database (Room)
  - Remote API (BLE)
  - Data Mappers
- **의존성**: Domain Layer 참조

---

## 🔄 MVVM + MVI 패턴

### MVVM 구조

```kotlin
// View (Compose UI)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // UI rendering based on state
    HomeContent(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

// ViewModel
class HomeViewModel @Inject constructor(
    private val wishCountRepository: WishCountRepository,
    private val bleRepository: BleRepository
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {
    
    // State management
    private val _state = MutableStateFlow(HomeViewState())
    val state = _state.asStateFlow()
    
    // Event handling
    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.IncrementCount -> incrementWishCount()
            is HomeEvent.ConnectDevice -> connectToDevice()
        }
    }
}
```

### MVI 단방향 데이터 플로우

```
     ┌──────────┐
     │   User   │
     └─────┬────┘
           │ Interaction
           ▼
     ┌──────────┐
     │  Event   │──────┐
     └──────────┘      │
                       ▼
               ┌──────────────┐
               │  ViewModel   │
               │   (Reducer)  │
               └──────┬───────┘
                      │ New State
                      ▼
               ┌──────────────┐
               │   ViewState  │
               └──────┬───────┘
                      │
                      ▼
               ┌──────────────┐
               │      UI      │
               └──────────────┘
```

### 핵심 컴포넌트

#### ViewState
```kotlin
data class HomeViewState(
    val isLoading: Boolean = false,
    val wishCount: Int = 0,
    val targetCount: Int = 1000,
    val wishText: String = "",
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val error: String? = null
)
```

#### Event
```kotlin
sealed interface HomeEvent {
    object LoadData : HomeEvent
    object IncrementCount : HomeEvent
    data class UpdateWishText(val text: String) : HomeEvent
    data class ConnectDevice(val deviceAddress: String) : HomeEvent
    object ResetCount : HomeEvent
}
```

#### Effect
```kotlin
sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
    data class NavigateToDetail(val wishId: Long) : HomeEffect
    object PlaySuccessAnimation : HomeEffect
    data class ShowError(val error: Throwable) : HomeEffect
}
```

---

## 📊 데이터 플로우

### 1. 일반 데이터 플로우

```
UI Event → ViewModel → UseCase → Repository → DataSource
    ↑                                              ↓
    └──────── StateFlow ← Flow ← Flow ← ──────────┘
```

### 2. BLE 실시간 데이터 플로우

```
BLE Device → MRD SDK → BleRepository → ViewModel → UI
     ↓                        ↓              ↓
  Notification            Flow/Channel   StateFlow
```

### 3. 데이터베이스 플로우

```kotlin
// Repository Implementation
class WishCountRepositoryImpl @Inject constructor(
    private val wishCountDao: WishCountDao
) : WishCountRepository {
    
    override fun getWishCountFlow(): Flow<WishCount?> {
        return wishCountDao.getLatestWishCount()
            .map { entity -> entity?.toDomainModel() }
    }
    
    override suspend fun updateWishCount(count: Int) {
        withContext(Dispatchers.IO) {
            val current = wishCountDao.getLatest() 
                ?: WishCountEntity(count = 0)
            wishCountDao.update(current.copy(count = count))
        }
    }
}
```

---

## 🔗 의존성 규칙

### Dependency Inversion Principle

```kotlin
// Domain Layer - Interface
interface WishCountRepository {
    fun getWishCountFlow(): Flow<WishCount?>
    suspend fun updateWishCount(count: Int)
    suspend fun resetCount(): Boolean
}

// Data Layer - Implementation
@Singleton
class WishCountRepositoryImpl @Inject constructor(
    private val dao: WishCountDao,
    private val bleRepository: BleRepository
) : WishCountRepository {
    // Implementation details
}

// DI Module - Binding
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindWishCountRepository(
        impl: WishCountRepositoryImpl
    ): WishCountRepository
}
```

### 의존성 그래프

```
Presentation → Domain ← Data
      ↓           ↑        ↓
   ViewModel   UseCase  Repository
      ↓                    ↓
   UI State            DataSource
```

---

## 📦 Feature-based Architecture

### 폴더 구조

```
presentation/
├── theme/                 # 공통 테마 시스템
│   ├── Color.kt
│   ├── Theme.kt
│   ├── Type.kt
│   └── Dimension.kt
├── component/            # 공통 컴포넌트
│   ├── CircularProgress.kt
│   └── BatteryIndicator.kt
├── home/                 # Home Feature
│   ├── HomeViewModel.kt
│   ├── HomeViewState.kt
│   ├── HomeEvent.kt
│   ├── HomeEffect.kt
│   ├── HomeScreen.kt
│   └── component/        # Feature 전용 컴포넌트
│       ├── WishCountCard.kt
│       └── WishReportItem.kt
├── detail/               # Detail Feature
│   ├── DetailViewModel.kt
│   ├── DetailScreen.kt
│   └── component/
└── wishinput/           # WishInput Feature
    ├── WishInputViewModel.kt
    ├── WishInputScreen.kt
    └── component/
```

### Feature Module 장점

1. **높은 응집도**: 관련 코드들이 한 곳에 모여있음
2. **낮은 결합도**: Feature 간 독립성 유지
3. **병렬 개발**: 팀원들이 독립적으로 Feature 개발 가능
4. **테스트 용이성**: Feature 단위로 테스트 가능
5. **리팩토링 편의성**: Feature 단위로 수정/제거 용이

### Feature 간 통신

```kotlin
// Navigation을 통한 Feature 간 통신
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Loading.route
    ) {
        composable(Screen.Loading.route) {
            LoadingScreen(
                onLoadingComplete = {
                    navController.navigate(Screen.Home.route)
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { date ->
                    navController.navigate("detail/$date")
                }
            )
        }
    }
}
```

---

## 🎮 상태 관리

### StateFlow를 통한 상태 관리

```kotlin
class HomeViewModel : BaseViewModel() {
    // Immutable state exposure
    private val _uiState = MutableStateFlow(HomeViewState())
    val uiState: StateFlow<HomeViewState> = _uiState.asStateFlow()
    
    // State updates
    private fun updateState(update: HomeViewState.() -> HomeViewState) {
        _uiState.update { currentState ->
            currentState.update()
        }
    }
    
    // Example usage
    fun incrementCount() {
        updateState { 
            copy(wishCount = wishCount + 1) 
        }
    }
}
```

### Effect 처리

```kotlin
class BaseViewModel<State, Event, Effect> : ViewModel() {
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

// UI에서 Effect 수집
LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is HomeEffect.ShowToast -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is HomeEffect.NavigateToDetail -> {
                navController.navigate("detail/${effect.wishId}")
            }
        }
    }
}
```

---

## 📡 BLE 통신 아키텍처

### BLE 레이어 구조

```
┌──────────────────────────┐
│    Presentation Layer    │
│   (BLE Status UI)        │
├──────────────────────────┤
│     Domain Layer         │
│  (BleRepository)         │
├──────────────────────────┤
│      Data Layer          │
│  ┌────────────────────┐  │
│  │ BleRepositoryImpl  │  │
│  ├────────────────────┤  │
│  │ MrdProtocolAdapter │  │
│  ├────────────────────┤  │
│  │    Nordic BLE      │  │
│  ├────────────────────┤  │
│  │     MRD SDK        │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

### BLE 연결 상태 관리

```kotlin
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning : BleConnectionState()
    object Connecting : BleConnectionState()
    data class Connected(val device: BleDevice) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

// StateFlow로 연결 상태 관리
class BleRepositoryImpl : BleRepository {
    private val _connectionState = MutableStateFlow<BleConnectionState>(
        BleConnectionState.Disconnected
    )
    override val connectionState = _connectionState.asStateFlow()
}
```

### 커맨드 큐 시스템

```kotlin
class BleCommandQueue {
    private val commandQueue = Channel<BleCommand>(Channel.UNLIMITED)
    
    suspend fun enqueue(command: BleCommand) {
        commandQueue.send(command)
    }
    
    suspend fun processCommands() {
        commandQueue.receiveAsFlow().collect { command ->
            executeCommand(command)
            delay(100) // 커맨드 간 딜레이
        }
    }
}
```

---

## ⚠️ 에러 처리 전략

### Result 패턴

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Repository에서 사용
suspend fun getWishCount(): Result<WishCount> {
    return try {
        val count = dao.getLatest()
        Result.Success(count.toDomainModel())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// ViewModel에서 처리
fun loadWishCount() {
    viewModelScope.launch {
        repository.getWishCount().collect { result ->
            when (result) {
                is Result.Success -> updateState { 
                    copy(wishCount = result.data.count) 
                }
                is Result.Error -> sendEffect(
                    HomeEffect.ShowError(result.exception)
                )
                Result.Loading -> updateState { 
                    copy(isLoading = true) 
                }
            }
        }
    }
}
```

### 글로벌 에러 핸들러

```kotlin
class GlobalErrorHandler @Inject constructor(
    private val logger: Logger
) {
    fun handleError(throwable: Throwable): ErrorState {
        logger.e(throwable)
        return when (throwable) {
            is NetworkException -> ErrorState.Network
            is BleException -> ErrorState.Bluetooth
            is DatabaseException -> ErrorState.Database
            else -> ErrorState.Unknown(throwable.message)
        }
    }
}
```

---

## 🧪 테스트 전략

### 레이어별 테스트

#### 1. Domain Layer 테스트
```kotlin
class GetWishCountUseCaseTest {
    @Test
    fun `when repository returns count, use case emits success`() = runTest {
        // Given
        val repository = mockk<WishCountRepository>()
        coEvery { repository.getWishCount() } returns WishCount(100)
        
        // When
        val useCase = GetWishCountUseCase(repository)
        val result = useCase()
        
        // Then
        assertEquals(100, result.count)
    }
}
```

#### 2. ViewModel 테스트
```kotlin
class HomeViewModelTest {
    @Test
    fun `increment count updates state correctly`() = runTest {
        // Given
        val viewModel = HomeViewModel(mockRepository)
        
        // When
        viewModel.handleEvent(HomeEvent.IncrementCount)
        
        // Then
        assertEquals(1, viewModel.state.value.wishCount)
    }
}
```

#### 3. Repository 테스트
```kotlin
class WishCountRepositoryTest {
    @Test
    fun `update wish count persists to database`() = runTest {
        // Given
        val dao = FakeWishCountDao()
        val repository = WishCountRepositoryImpl(dao)
        
        // When
        repository.updateWishCount(42)
        
        // Then
        val saved = dao.getLatest()
        assertEquals(42, saved?.count)
    }
}
```

---

## 🚀 성능 최적화

### 1. StateFlow 최적화
- `distinctUntilChanged()` 사용으로 불필요한 리컴포지션 방지
- `stateIn()` 으로 초기값과 공유 전략 설정

### 2. Coroutine 최적화
- 적절한 Dispatcher 사용 (IO, Main, Default)
- `supervisorScope` 로 에러 격리
- `cancellationException` 처리

### 3. 메모리 관리
- ViewModel `onCleared()` 에서 리소스 정리
- Flow collection 시 lifecycle-aware 수집

---

## 📐 아키텍처 결정 기록 (ADR)

### ADR-001: Clean Architecture 채택
- **날짜**: 2024-09-02
- **결정**: Clean Architecture 패턴 채택
- **이유**: 비즈니스 로직 독립성, 테스트 용이성, 유지보수성

### ADR-002: MVI 패턴 적용
- **날짜**: 2024-09-02
- **결정**: MVVM에 MVI의 단방향 데이터 플로우 추가
- **이유**: 예측 가능한 상태 관리, 디버깅 용이성

### ADR-003: Hilt 의존성 주입
- **날짜**: 2024-09-02
- **결정**: Dagger Hilt 사용
- **이유**: Android 전용 최적화, 보일러플레이트 감소

### ADR-004: Room Database
- **날짜**: 2024-09-02
- **결정**: 로컬 저장소로 Room 사용
- **이유**: SQLite 추상화, 컴파일 타임 검증, Flow 지원