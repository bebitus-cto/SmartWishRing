# 🔧 WISH RING Android 앱 TRD (Technical Requirements Document)

## 📋 목차
1. [문서 개요](#1-문서-개요)
2. [기술 아키텍처](#2-기술-아키텍처)
3. [기술 스택](#3-기술-스택)
4. [아키텍처 패턴](#4-아키텍처-패턴)
5. [베이스 클래스 설계](#5-베이스-클래스-설계)
6. [데이터 레이어 설계](#6-데이터-레이어-설계)
7. [프레젠테이션 레이어 설계](#7-프레젠테이션-레이어-설계)
8. [보안 요구사항](#8-보안-요구사항)
9. [코딩 컨벤션](#9-코딩-컨벤션)
10. [테스트 전략](#10-테스트-전략)

---

## 1. 문서 개요

### 1.1 목적
본 문서는 WISH RING Android 앱 개발을 위한 기술적 요구사항과 아키텍처 설계를 정의합니다.

### 1.2 범위
- Android 네이티브 앱 개발
- BLE 디바이스 연동
- 로컬 데이터베이스 관리
- UI/UX 구현

### 1.3 대상 독자
- Android 개발자
- 프로젝트 매니저
- QA 엔지니어

---

## 2. 기술 아키텍처

### 2.1 아키텍처 원칙

| 원칙 | 설명 |
|------|------|
| **단방향 데이터 플로우** | UDA (Unidirectional Data Architecture) 패턴 적용 |
| **단일 진실 공급원** | State는 ViewModel이 유일하게 소유 |
| **관심사 분리** | 레이어별 명확한 책임 분리 |
| **의존성 역전** | Interface를 통한 레이어 간 통신 |
| **재사용성** | BaseViewModel/BaseActivity를 통한 코드 재사용 |

### 2.2 레이어 구조

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Compose UI + ViewModel + State)       │
├─────────────────────────────────────────┤
│           Domain Layer                  │
│  (Repository Interface + Models)        │
├─────────────────────────────────────────┤
│            Data Layer                   │
│  (Repository Impl + Room DAO)           │
└─────────────────────────────────────────┘
```

**주요 특징:**
- UseCase 레이어 제외 (Google App Architecture 단순화)
- Repository에 Room DAO 직접 주입
- DataSource 레이어 제외

---

## 3. 기술 스택

### 3.1 핵심 기술

| 카테고리 | 기술 | 버전 | 용도 |
|----------|------|------|------|
| **UI Framework** | Jetpack Compose | BOM 2024.02.00 | 선언적 UI 구성 |
| **DI Framework** | Hilt | 2.50 | 의존성 주입 |
| **비동기 처리** | Coroutines | 1.7.3 | 비동기 작업 처리 |
| **반응형 프로그래밍** | Flow | - | 데이터 스트림 관리 |
| **로컬 DB** | Room | 2.6.1 | 영구 데이터 저장 |
| **Navigation** | Navigation Compose | 2.7.6 | 화면 전환 관리 |

### 3.2 지원 라이브러리

| 라이브러리 | 용도 |
|------------|------|
| **Lifecycle ViewModel Compose** | ViewModel 생명주기 관리 |
| **DataStore Preferences** | 키-값 저장소 |
| **Accompanist Permissions** | 권한 처리 |
| **Coil Compose** | 이미지 로딩 |
| **Material3** | Material Design 3 컴포넌트 |

### 3.3 개발 도구

| 도구 | 용도 |
|------|------|
| **Android Studio** | IDE |
| **Kotlin 1.9.22** | 프로그래밍 언어 |
| **Gradle 8.x** | 빌드 도구 |
| **Git** | 버전 관리 |

---

## 4. 아키텍처 패턴

### 4.1 MVVM + UDA 패턴

#### 4.1.1 구성 요소

```kotlin
// View (Compose)
@Composable
fun Screen(
    uiState: State,
    onEvent: (Event) -> Unit
)

// ViewModel
class ViewModel {
    val uiState: StateFlow<State>
    val effect: Flow<Effect>
    fun onEvent(event: Event)
}

// State (불변 데이터)
data class UiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList()
)

// Event (사용자 액션)
sealed class UiEvent {
    object LoadData : UiEvent()
    data class ItemClicked(val id: String) : UiEvent()
}

// Effect (일회성 이벤트)
sealed class UiEffect {
    data class ShowToast(val message: String) : UiEffect()
    object NavigateBack : UiEffect()
}
```

#### 4.1.2 데이터 플로우

```
User Action → Event → ViewModel → State Update → UI Recomposition
                 ↓
              Effect → One-time Action (Navigation, Toast, etc.)
```

### 4.2 Repository 패턴

#### 4.2.1 Interface 정의 (Domain Layer)

```kotlin
interface WishRepository {
    fun getTodayCount(): Flow<Int>
    suspend fun incrementCount()
    suspend fun saveWish(text: String, target: Int)
}
```

#### 4.2.2 Implementation (Data Layer)

```kotlin
@Singleton
class WishRepositoryImpl @Inject constructor(
    private val wishCountDao: WishCountDao,  // Room DAO 직접 주입
    private val bleManager: BleManager
) : WishRepository {
    
    override fun getTodayCount(): Flow<Int> = 
        wishCountDao.getTodayCount()
    
    override suspend fun incrementCount() {
        wishCountDao.incrementCount()
    }
}
```

**특징:**
- DataSource 레이어 없이 DAO 직접 사용
- 단일 책임 원칙 준수
- 테스트 용이성 확보

---

## 5. 베이스 클래스 설계

### 5.1 BaseViewModel

```kotlin
abstract class BaseViewModel<State, Event, Effect> : ViewModel() {
    
    // State Management
    protected abstract val _uiState: MutableStateFlow<State>
    val uiState: StateFlow<State> get() = _uiState.asStateFlow()
    
    // Effect Channel
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // Event Handler
    abstract fun onEvent(event: Event)
    
    // State Update Helper
    protected fun updateState(update: State.() -> State) {
        _uiState.value = _uiState.value.update()
    }
    
    // Effect Sender
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
    
    // Error Handling
    protected fun handleError(throwable: Throwable) {
        Timber.e(throwable)
        // 공통 에러 처리 로직
    }
    
    // Loading State Management
    protected suspend fun <T> withLoading(
        block: suspend () -> T
    ): T {
        updateState { setLoading(true) }
        return try {
            block()
        } finally {
            updateState { setLoading(false) }
        }
    }
}
```

### 5.2 BaseActivity

```kotlin
abstract class BaseActivity<VM : ViewModel> : ComponentActivity() {
    
    protected abstract val viewModel: VM
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 공통 초기화
        setupSystemUI()
        setupPermissions()
        
        setContent {
            WishRingTheme {
                Surface {
                    Content()
                }
            }
        }
    }
    
    @Composable
    protected abstract fun Content()
    
    // System UI 설정
    private fun setupSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Status bar, Navigation bar 설정
    }
    
    // 권한 처리
    protected open fun setupPermissions() {
        // BLE, Notification 등 공통 권한 처리
    }
    
    // 생명주기 로깅
    override fun onResume() {
        super.onResume()
        Timber.d("Activity resumed: ${this::class.simpleName}")
    }
}
```

### 5.3 재사용 가능한 ViewModel 설계

```kotlin
// Generic List ViewModel
abstract class ListViewModel<T, State : ListState<T>, Event : ListEvent, Effect> 
    : BaseViewModel<State, Event, Effect>() {
    
    protected abstract fun loadItems(): Flow<List<T>>
    
    protected fun refreshList() {
        viewModelScope.launch {
            withLoading {
                loadItems()
                    .catch { handleError(it) }
                    .collect { items ->
                        updateState { withItems(items) }
                    }
            }
        }
    }
}

// State Interface
interface ListState<T> {
    val items: List<T>
    val isLoading: Boolean
    val error: String?
    
    fun withItems(items: List<T>): ListState<T>
    fun setLoading(loading: Boolean): ListState<T>
}
```

---

## 6. 데이터 레이어 설계

### 6.1 Room Database 구조

#### 6.1.1 Database 정의

```kotlin
@Database(
    entities = [
        WishCountEntity::class,
        CounterResetLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class WishRingDatabase : RoomDatabase() {
    abstract fun wishCountDao(): WishCountDao
    abstract fun resetLogDao(): ResetLogDao
}
```

#### 6.1.2 Entity 설계

```kotlin
@Entity(tableName = "wish_counts")
data class WishCountEntity(
    @PrimaryKey 
    val date: String,  // "2025-01-02" 형식
    val totalCount: Int = 0,
    val wishText: String = "",
    val targetCount: Int = 1000,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### 6.1.3 DAO 인터페이스

```kotlin
@Dao
interface WishCountDao {
    
    @Query("SELECT * FROM wish_counts WHERE date = :date")
    suspend fun getByDate(date: String): WishCountEntity?
    
    @Query("SELECT totalCount FROM wish_counts WHERE date = date('now', 'localtime')")
    fun getTodayCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WishCountEntity)
    
    @Update
    suspend fun update(entity: WishCountEntity)
    
    @Transaction
    suspend fun incrementTodayCount() {
        val today = DateUtils.getTodayString()
        val entity = getByDate(today) ?: WishCountEntity(date = today)
        insert(entity.copy(
            totalCount = entity.totalCount + 1,
            isCompleted = entity.totalCount + 1 >= entity.targetCount,
            updatedAt = System.currentTimeMillis()
        ))
    }
}
```

### 6.2 Repository 구현

```kotlin
@Singleton
class WishRepositoryImpl @Inject constructor(
    private val wishCountDao: WishCountDao,
    private val resetLogDao: ResetLogDao,
    private val bleManager: BleManager
) : WishRepository {
    
    override fun getTodayCount(): Flow<Int> = 
        wishCountDao.getTodayCount()
            .map { it ?: 0 }
            .flowOn(Dispatchers.IO)
    
    override suspend fun incrementCount() = withContext(Dispatchers.IO) {
        wishCountDao.incrementTodayCount()
    }
    
    override suspend fun saveWish(text: String, target: Int) = withContext(Dispatchers.IO) {
        val today = DateUtils.getTodayString()
        val entity = wishCountDao.getByDate(today) ?: WishCountEntity(date = today)
        wishCountDao.insert(
            entity.copy(
                wishText = text,
                targetCount = target,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
```

---

## 7. 프레젠테이션 레이어 설계

### 7.1 State Management

#### 7.1.1 State 정의

```kotlin
data class HomeUiState(
    // Counter State
    val currentCount: Int = 0,
    val targetCount: Int = 1000,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    
    // Wish State
    val wishText: String = "나는 매일 성장하고 있다.",
    val dateString: String = DateUtils.getTodayString(),
    
    // BLE State
    val isConnected: Boolean = false,
    val batteryLevel: Int = 100,
    
    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val dailyRecords: List<DailyRecord> = emptyList()
) {
    val progressPercentage: Int 
        get() = (progress * 100).toInt()
}
```

#### 7.1.2 Event 정의

```kotlin
sealed class HomeEvent {
    // User Actions
    object WishButtonClicked : HomeEvent()
    object ShareClicked : HomeEvent()
    data class RecordClicked(val date: String) : HomeEvent()
    
    // System Events
    object ScreenInitialized : HomeEvent()
    object RefreshRequested : HomeEvent()
    
    // BLE Events
    object RetryConnection : HomeEvent()
}
```

#### 7.1.3 Effect 정의

```kotlin
sealed class HomeEffect {
    // Navigation
    object NavigateToWishInput : HomeEffect()
    data class NavigateToDetail(val date: String) : HomeEffect()
    
    // UI Feedback
    data class ShowToast(val message: String) : HomeEffect()
    data class ShowError(val error: String) : HomeEffect()
    
    // Share
    data class ShareContent(
        val imageUri: Uri,
        val text: String
    ) : HomeEffect()
}
```

### 7.2 ViewModel 구현

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WishRepository,
    private val bleManager: BleManager
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>() {
    
    override val _uiState = MutableStateFlow(HomeUiState())
    
    init {
        observeTodayCount()
        observeBleConnection()
        loadDailyRecords()
    }
    
    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.WishButtonClicked -> navigateToWishInput()
            is HomeEvent.ShareClicked -> shareContent()
            is HomeEvent.RecordClicked -> navigateToDetail(event.date)
            is HomeEvent.RefreshRequested -> refresh()
            is HomeEvent.RetryConnection -> retryBleConnection()
            else -> {}
        }
    }
    
    private fun observeTodayCount() {
        repository.getTodayCount()
            .onEach { count ->
                updateState {
                    copy(
                        currentCount = count,
                        progress = count.toFloat() / targetCount,
                        isCompleted = count >= targetCount
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun navigateToWishInput() {
        sendEffect(HomeEffect.NavigateToWishInput)
    }
}
```

### 7.3 Compose UI 구현

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is HomeEffect.NavigateToWishInput -> {
                    // Navigation 처리
                }
                // ...
            }
        }
    }
    
    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        HeaderSection(
            wishText = uiState.wishText,
            dateString = uiState.dateString
        )
        
        // Counter Display
        CounterSection(
            count = uiState.currentCount,
            progress = uiState.progress,
            isCompleted = uiState.isCompleted
        )
        
        // Action Buttons
        ActionButtons(
            onWishClick = { onEvent(HomeEvent.WishButtonClicked) },
            onShareClick = { onEvent(HomeEvent.ShareClicked) }
        )
        
        // Daily Records
        DailyRecordsList(
            records = uiState.dailyRecords,
            onRecordClick = { date -> 
                onEvent(HomeEvent.RecordClicked(date))
            }
        )
    }
}
```

---

## 8. 보안 요구사항

### 8.1 Android 16KB 페이지 크기 대응

#### 8.1.1 배경
Android 15부터 일부 기기에서 기본 메모리 페이지 크기가 4KB에서 16KB로 변경됩니다. 이는 성능 향상을 위한 조치이지만, 기존 앱의 호환성 문제를 일으킬 수 있습니다.

#### 8.1.2 대응 전략

```gradle
android {
    defaultConfig {
        targetSdk = 35  // Android 15 대응
        
        // NDK 설정 (BLE SDK 네이티브 라이브러리 사용 시)
        ndk {
            // 16KB 페이지 정렬 플래그 추가
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
    
    packagingOptions {
        // 16KB 정렬된 네이티브 라이브러리 포함
        jniLibs {
            useLegacyPackaging = false
        }
    }
}
```

#### 8.1.3 체크리스트

| 항목 | 대응 방법 | 상태 |
|------|----------|------|
| **네이티브 라이브러리** | 16KB 정렬 확인 (`zipalign -p 16384`) | 필수 |
| **BLE SDK** | 제조사 SDK 16KB 호환성 확인 | 필수 |
| **메모리 할당** | 페이지 경계 정렬 확인 | 권장 |
| **JNI 코드** | Direct ByteBuffer 정렬 검증 | 해당 시 |

#### 8.1.4 검증 방법

```kotlin
// 런타임 페이지 크기 확인
object PageSizeChecker {
    fun getPageSize(): Int {
        return try {
            val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
            Log.d("PageSize", "System page size: $pageSize bytes")
            pageSize.toInt()
        } catch (e: Exception) {
            4096  // 기본값
        }
    }
    
    fun verify16KBCompatibility() {
        val pageSize = getPageSize()
        if (pageSize > 4096) {
            // 16KB 페이지 환경
            Log.i("PageSize", "Running on 16KB page size device")
            // 추가 호환성 검증 로직
        }
    }
}
```

### 8.2 코드 난독화

#### 8.2.1 ProGuard/R8 설정

```proguard
# Keep data classes
-keep class com.wishring.app.domain.model.** { *; }
-keep class com.wishring.app.data.local.entity.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

### 8.3 데이터 보안

#### 8.3.1 민감 데이터 암호화

```kotlin
object SecurityManager {
    
    // SharedPreferences 암호화
    fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // Room Database 암호화 (SQLCipher)
    fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<WishRingDatabase> {
        val passphrase = SQLiteDatabase.getBytes("your-secure-passphrase".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            WishRingDatabase::class.java,
            "wishring_encrypted.db"
        ).openHelperFactory(factory)
    }
}
```

#### 8.3.2 BLE 통신 보안

```kotlin
class SecureBleManager {
    
    // 데이터 검증
    private fun validateBleData(data: ByteArray): Boolean {
        // CRC 체크섬 검증
        val receivedCrc = data.takeLast(2).toByteArray()
        val calculatedCrc = calculateCRC(data.dropLast(2).toByteArray())
        return receivedCrc.contentEquals(calculatedCrc)
    }
    
    // 페어링 검증
    private fun verifyPairing(device: BluetoothDevice): Boolean {
        return device.bondState == BluetoothDevice.BOND_BONDED
    }
}
```

### 8.4 앱 보안

#### 8.4.1 앱 무결성 검증

```kotlin
object AppIntegrityChecker {
    
    fun verifyAppSignature(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            
            val signatures = packageInfo.signatures
            val expectedSignature = "YOUR_RELEASE_SIGNATURE_HASH"
            
            return signatures.any { 
                hashSignature(it.toByteArray()) == expectedSignature 
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun hashSignature(signature: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(signature).toHexString()
    }
}
```

---

## 9. 코딩 컨벤션

### 9.1 Kotlin 코딩 스타일

#### 9.1.1 네이밍 규칙

| 요소 | 규칙 | 예시 |
|------|------|------|
| **Package** | lowercase | `com.wishring.app.data` |
| **Class** | PascalCase | `WishCountEntity` |
| **Function** | camelCase | `getTodayCount()` |
| **Constant** | UPPER_SNAKE_CASE | `MAX_COUNT` |
| **Variable** | camelCase | `currentCount` |
| **Composable** | PascalCase | `HomeScreen()` |

#### 9.1.2 파일 구조

```kotlin
// 1. Package declaration
package com.wishring.app.presentation.home

// 2. Import statements (순서: Android → Third-party → Project)
import android.os.Bundle
import androidx.compose.material3.Text
import com.wishring.app.domain.model.WishCount

// 3. Class/Interface definition
class HomeViewModel : ViewModel() {
    // 3.1 Companion object
    companion object {
        const val TAG = "HomeViewModel"
    }
    
    // 3.2 Properties
    private val repository: WishRepository
    
    // 3.3 Initialization
    init {
        // initialization code
    }
    
    // 3.4 Public functions
    fun publicFunction() {}
    
    // 3.5 Private functions
    private fun privateFunction() {}
}
```

### 9.2 Compose 가이드라인

#### 9.2.1 Composable 구조

```kotlin
@Composable
fun MyComponent(
    modifier: Modifier = Modifier,  // 항상 첫 번째 파라미터
    state: UiState,                 // State 파라미터
    onEvent: (Event) -> Unit        // Event handler
) {
    // Implementation
}
```

#### 9.2.2 State Hoisting

```kotlin
// ❌ Bad: Internal state management
@Composable
fun BadTextField() {
    var text by remember { mutableStateOf("") }
    TextField(value = text, onValueChange = { text = it })
}

// ✅ Good: State hoisting
@Composable
fun GoodTextField(
    text: String,
    onTextChange: (String) -> Unit
) {
    TextField(value = text, onValueChange = onTextChange)
}
```

---

## 10. 테스트 전략

### 10.1 테스트 유형

| 테스트 유형 | 범위 | 도구 | 커버리지 목표 |
|------------|------|------|--------------|
| **Unit Test** | 개별 함수/클래스 | JUnit, MockK | 80% |
| **Integration Test** | 모듈 간 통합 | JUnit, Hilt Test | 60% |
| **UI Test** | Compose UI | Compose Test | 40% |
| **E2E Test** | 전체 시나리오 | Espresso | 주요 플로우 |

### 10.2 Unit Test 예제

```kotlin
@ExperimentalCoroutinesApi
class HomeViewModelTest {
    
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    
    @MockK
    lateinit var repository: WishRepository
    
    private lateinit var viewModel: HomeViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = HomeViewModel(repository)
    }
    
    @Test
    fun `when increment count event, should update state`() = runTest {
        // Given
        coEvery { repository.incrementCount() } returns Unit
        every { repository.getTodayCount() } returns flowOf(1)
        
        // When
        viewModel.onEvent(HomeEvent.IncrementCount)
        
        // Then
        assertEquals(1, viewModel.uiState.value.currentCount)
        coVerify { repository.incrementCount() }
    }
}
```

### 10.3 Compose UI Test

```kotlin
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun counterDisplay_ShowsCorrectCount() {
        // Given
        val uiState = HomeUiState(currentCount = 42)
        
        // When
        composeTestRule.setContent {
            HomeContent(
                uiState = uiState,
                onEvent = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("42")
            .assertIsDisplayed()
    }
}
```

### 10.4 테스트 커버리지 도구

```gradle
android {
    buildTypes {
        debug {
            testCoverageEnabled = true
        }
    }
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
```

---

## 📝 개정 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0.0 | 2025-01-06 | 초기 작성 | Dev Team |

---

**문서 상태**: ✅ 승인  
**최종 검토일**: 2025년 1월 6일  
**다음 검토일**: 2025년 2월 6일