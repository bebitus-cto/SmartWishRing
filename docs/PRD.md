# 📱 WISH RING Android 앱 PRD (Product Requirements Document)

## 📋 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택 및 아키텍처](#2-기술-스택-및-아키텍처)
3. [하드웨어 명세](#3-하드웨어-명세)
4. [SDK 통합 가이드](#4-sdk-통합-가이드)
5. [코딩 컨벤션](#5-코딩-컨벤션)
6. [프로젝트 구조](#6-프로젝트-구조)
7. [Data Layer 설계](#7-data-layer-설계)
8. [Presentation Layer 설계](#8-presentation-layer-설계)
9. [페이지별 기능 명세](#9-페이지별-기능-명세)
10. [BLE 통신 사양](#10-ble-통신-사양)
11. [개발 일정](#11-개발-일정)

---

## 1. 프로젝트 개요

### 1.1 제품 정보
| 항목 | 내용 |
|------|------|
| **앱 이름** | WISH RING (WISHLOG) |
| **플랫폼** | Android 전용 |
| **목적** | BLE 연동 확언 카운터 앱 - 반지 디바이스와 연동하여 확언 횟수 기록 및 시각화 |
| **핵심 철학** | 디지털 디톡스 - 최소한의 화면, 직관적 UI, 빠른 피드백 |
| **타겟 사용자** | 긍정 확언과 목표 달성을 추구하는 사용자 |

### 1.2 핵심 기능
- ✅ BLE 반지 디바이스 연동 (실시간 카운터 수신)
- ✅ 일일 확언 횟수 기록 및 시각화
- ✅ 확언 문장 및 목표 설정
- ✅ 진행률 게이지 표시 (목표 달성 시 무지개 효과)
- ✅ SNS 공유 기능
- ✅ 백그라운드 자동 재연결

### 1.3 제약사항
- Android 8.0 (API 26) 이상
- BLE 4.0 이상 지원 기기
- 인터넷 연결 불필요 (오프라인 우선)

---

## 2. 기술 스택 및 아키텍처

### 2.1 기술 스택
```kotlin
// Build Configuration
minSdk = 26           // Android 8.0 (Oreo)
targetSdk = 35       // Android 15
compileSdk = 35

// Core Libraries
Kotlin = "1.9.22"
Compose BOM = "2024.02.00"
Hilt = "2.50"
Room = "2.6.1"
Coroutines = "1.7.3"
DataStore = "1.0.0"
```

### 2.2 아키텍처 원칙
- **아키텍처 패턴**: MVVM + UDA (Unidirectional Data Architecture)
- **데이터 흐름**: View → ViewModel → Repository → Room DAO (DataSource 레이어 제외)
- **상태 관리**: 단일 State/Effect 패턴
- **의존성 주입**: Hilt
- **비동기 처리**: Coroutines + Flow
- **베이스 클래스**: BaseViewModel, BaseActivity를 통한 공통 로직 관리

### 2.3 레이어 책임

| Layer | 책임 | 주요 컴포넌트 |
|-------|------|--------------|
| **Presentation** | UI 렌더링, 사용자 입력 처리 | Compose UI, ViewModel, BaseViewModel |
| **Domain** | 비즈니스 로직, 도메인 모델 | Repository Interface, Domain Model (UseCase 제외) |
| **Data** | 데이터 저장 및 통신 | Repository Implementation + Room DAO (직접 주입), BLE Manager |

---

## 3. 하드웨어 명세

### 3.1 WishRing 디바이스 스펙

| 항목 | 사양 | 비고 |
|------|------|------|
| **연결 방식** | BLE (Bluetooth Low Energy) 4.0+ | |
| **배터리** | 내장 충전식 배터리 | 배터리 잔량 실시간 모니터링 |
| **물리적 입력** | 터치/탭 센서 | 카운터 증가 (+1) |
| **리셋 기능** | 하드웨어 리셋 버튼 | 디바이스 카운터 초기화 |
| **LED 표시** | 상태 표시 LED | 연결/배터리 상태 |

### 3.2 디바이스 동작 방식

#### 카운터 증가
- **트리거**: 반지 터치/탭
- **동작**: 디바이스 내부 카운터 +1 → BLE로 앱에 전송
- **응답**: LED 피드백 (짧은 깜빡임)

#### 배터리 모니터링  
- **주기**: 5분마다 또는 상태 변경 시
- **전송**: 배터리 레벨 (0-100%) → BLE로 앱에 전송
- **저전력**: 배터리 15% 이하 시 경고

#### 하드웨어 리셋
- **트리거**: 리셋 버튼 3초 장압
- **동작**: 디바이스 내부 카운터 → 0 초기화
- **알림**: 앱에 RESET 이벤트 전송 (리셋 전 카운트 값 포함)

---

## 4. MRD SDK 통합 가이드

### 4.1 Manridy SDK 개요

WishRing 디바이스는 Manridy(MRD) SDK를 통해 연동됩니다. UUID 직접 관리 없이 고수준 명령어로 통신합니다.

| 구성 요소 | 형태 | 용도 |
|----------|------|------|
| **MRD SDK** | AAR 라이브러리 (libs/mrd_xxx.aar) | 디바이스 통신, 명령어 처리 |
| **SDK 문서** | PDF | SystemEnum, MrdReadEnum, 콜백 가이드 |
| **Demo 코드** | Java/Kotlin | 연동 패턴 예제 |

### 4.2 MRD SDK 주요 기능

#### 고수준 명령어 시스템
```kotlin
// MRD SDK 실제 API 패턴
Manridy.getMrdSend().getSystem(SystemEnum.battery, 1)      // 배터리 조회
Manridy.getMrdSend().getSystem(SystemEnum.brightness, 1)   // 밝기 설정
Manridy.getMrdSend().getSystem(SystemEnum.firmware, 1)     // 펌웨어 정보

// 콜백 등록
Manridy.setOnMrdReadListener { mrdReadEnum, data ->
    when (mrdReadEnum) {
        MrdReadEnum.HEART -> handleHeartData(data)
        MrdReadEnum.BP -> handleBatteryData(data) 
        MrdReadEnum.RESET -> handleResetEvent(data)
    }
}
```

#### 데이터 수신 패턴
- **콜백 기반**: MrdReadEnum으로 데이터 타입 구분
- **실시간 처리**: 디바이스 → SDK 콜백 → 앱 Flow
- **상태 관리**: 연결/해제 상태 자동 관리

### 4.3 통합 요구사항

#### Gradle 의존성
```gradle
dependencies {
    implementation files('libs/sdk_mrd_xxx.aar')
    
    // BLE 기본 라이브러리 (SDK 내부 사용)
    implementation 'no.nordicsemi.android:ble:2.7.0'
}
```

#### 권한 설정
```xml
<!-- BLE 기본 권한 (SDK 요구사항) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

#### 초기화 패턴
```kotlin
// Application 클래스에서 초기화
class WishRingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // MRD SDK 초기화
        Manridy.init(this)
        
        // 콜백 등록
        setupMrdCallbacks()
    }
}

---

## 5. 코딩 컨벤션

### 3.1 네이밍 규칙

#### 패키지명
```kotlin
// 소문자, 단어 구분 없음
com.wishring.app.data.local
com.wishring.app.presentation.ui.home
```

#### 클래스명
```kotlin
// PascalCase
class WishCountEntity
class HomeViewModel
interface WishRepository
```

#### 함수명
```kotlin
// camelCase, 동사로 시작
fun getTodayCount(): Flow<Int>
fun updateWishText(text: String)
suspend fun saveToDatabase()
```

#### 변수명
```kotlin
// camelCase
val currentCount: Int
private val _uiState = MutableStateFlow()
const val MAX_COUNT = 99999  // 상수는 UPPER_SNAKE_CASE
```

#### Compose 함수
```kotlin
// PascalCase, 명사형
@Composable
fun HomeScreen()

@Composable
fun CircularProgressGauge()
```

### 3.2 파일 구조

#### 단일 책임 원칙
- 한 파일에 하나의 public 클래스/인터페이스
- 관련 extension 함수는 같은 파일에 포함 가능

#### 파일명
```kotlin
// 클래스명과 동일
HomeViewModel.kt
WishCountEntity.kt
DateExtensions.kt  // Extension 함수 모음
```

### 3.3 코드 스타일

#### Import 정렬
```kotlin
// 1. Android imports
import android.content.Context
import android.os.Bundle

// 2. Third-party imports
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

// 3. Project imports
import com.wishring.app.data.model.WishCount
```

#### 클래스 구조 순서
```kotlin
class ExampleClass {
    // 1. Companion object
    companion object {
        const val CONSTANT = "value"
    }
    
    // 2. Properties
    private val property: String
    
    // 3. Init block
    init {
        // initialization
    }
    
    // 4. Functions
    fun publicFunction() {}
    private fun privateFunction() {}
}
```

### 3.4 Compose 컨벤션

#### State Hoisting
```kotlin
// ❌ Bad
@Composable
fun BadExample() {
    var text by remember { mutableStateOf("") }
    TextField(value = text, onValueChange = { text = it })
}

// ✅ Good
@Composable
fun GoodExample(
    text: String,
    onTextChange: (String) -> Unit
) {
    TextField(value = text, onValueChange = onTextChange)
}
```

#### Modifier 전달
```kotlin
@Composable
fun CustomComponent(
    modifier: Modifier = Modifier,  // 항상 첫 번째 파라미터
    // other parameters
) {
    Box(modifier = modifier) {
        // content
    }
}
```

### 3.5 ViewModel 컨벤션

#### UDA 패턴 구조
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    
    // State - Single source of truth
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Effects - One-time events
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    // Event handler - Single entry point
    fun onEvent(event: Event) {
        when (event) {
            is Event.Action -> handleAction()
        }
    }
}
```

### 3.6 Repository 컨벤션

#### 인터페이스 우선
```kotlin
// Domain layer
interface WishRepository {
    fun getTodayCount(): Flow<Int>
    suspend fun saveWish(text: String, target: Int)
}

// Data layer - DAO 직접 주입 (DataSource 레이어 제외)
@Singleton
class WishRepositoryImpl @Inject constructor(
    private val wishCountDao: WishCountDao,  // Room DAO 직접 주입
    private val bleManager: BleManager
) : WishRepository {
    // Repository에서 DAO 메서드 직접 호출
    override fun getTodayCount(): Flow<Int> = 
        wishCountDao.getTodayCount()
}
```

---

## 4. 프로젝트 구조

### 4.1 모듈 구조
```
app/
├── src/
│   └── main/
│       ├── java/com/wishring/app/
│       │   ├── WishRingApplication.kt
│       │   ├── data/
│       │   ├── domain/
│       │   ├── presentation/
│       │   └── di/
│       └── res/
```

### 4.2 패키지 상세 구조
```
com.wishring.app/
├── data/                          # Data Layer
│   ├── local/
│   │   ├── database/
│   │   │   ├── WishRingDatabase.kt
│   │   │   ├── converter/         # Type Converters
│   │   │   ├── entity/            # Room Entities
│   │   │   └── dao/               # Data Access Objects
│   │   └── preferences/
│   │       ├── WishRingPreferences.kt
│   │       └── DataStoreManager.kt
│   ├── ble/
│   │   ├── BleManager.kt
│   │   ├── BleService.kt          # Foreground Service
│   │   ├── GattCallback.kt
│   │   └── model/
│   │       ├── BleDevice.kt
│   │       ├── BleConnectionState.kt
│   │       └── BleConstants.kt
│   └── repository/
│       └── WishRepositoryImpl.kt
│
├── domain/                        # Domain Layer
│   ├── model/
│   │   ├── WishCount.kt
│   │   ├── DailyRecord.kt
│   │   ├── ResetLog.kt
│   │   └── ShareData.kt
│   ├── repository/
│   │   └── WishRepository.kt
│   └── util/
│       ├── DateUtils.kt
│       └── CounterUtils.kt
│
├── presentation/                  # Presentation Layer
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Theme.kt
│   │   ├── splash/
│   │   │   └── SplashScreen.kt
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── components/
│   │   │       ├── CircularGauge.kt
│   │   │       ├── CountDisplay.kt
│   │   │       └── RecordList.kt
│   │   ├── detail/
│   │   │   └── DetailScreen.kt
│   │   ├── input/
│   │   │   └── WishInputScreen.kt
│   │   └── common/
│   │       ├── LoadingIndicator.kt
│   │       └── ErrorDialog.kt
│   ├── viewmodel/
│   │   ├── SplashViewModel.kt
│   │   ├── HomeViewModel.kt
│   │   ├── DetailViewModel.kt
│   │   └── WishInputViewModel.kt
│   ├── state/
│   │   ├── HomeUiState.kt
│   │   ├── DetailUiState.kt
│   │   └── WishInputUiState.kt
│   ├── event/
│   │   ├── HomeEvent.kt
│   │   ├── DetailEvent.kt
│   │   └── WishInputEvent.kt
│   ├── effect/
│   │   ├── NavigationEffect.kt
│   │   ├── ToastEffect.kt
│   │   └── ShareEffect.kt
│   ├── navigation/
│   │   ├── WishRingNavigation.kt
│   │   └── Screen.kt
│   └── util/
│       ├── ShareManager.kt
│       └── PermissionHelper.kt
│
└── di/                            # Dependency Injection
    ├── DatabaseModule.kt
    ├── BleModule.kt
    ├── RepositoryModule.kt
    ├── ServiceModule.kt
    └── PreferencesModule.kt
```

---

## 5. Data Layer 설계

### 5.1 Room Database

#### Entity 정의
```kotlin
@Entity(tableName = "wish_counts")
data class WishCountEntity(
    @PrimaryKey val date: String,        // "2025-01-02" 형식
    val totalCount: Int,                 // 일일 누적 카운트
    val wishText: String,                // 확언 문장
    val targetCount: Int,                // 목표 횟수
    val isCompleted: Boolean,            // 목표 달성 여부
    val createdAt: Long,                 // 생성 시간
    val updatedAt: Long                  // 마지막 업데이트
)

@Entity(tableName = "reset_logs")
data class CounterResetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                    // 리셋 발생 날짜
    val resetTime: Long,                 // 리셋 시점 timestamp
    val countBeforeReset: Int            // 리셋 전 카운트
)
```

#### DAO 인터페이스
```kotlin
@Dao
interface WishCountDao {
    @Query("SELECT * FROM wish_counts WHERE date = :date")
    suspend fun getByDate(date: String): WishCountEntity?
    
    @Query("SELECT * FROM wish_counts ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 30): Flow<List<WishCountEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wishCount: WishCountEntity)
    
    @Update
    suspend fun update(wishCount: WishCountEntity)
}
```

### 5.2 BLE Manager 구조

#### 연결 상태 관리
```kotlin
enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}
```

#### BLE Service UUID
```kotlin
object BleConstants {
    const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val COUNTER_CHAR_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
    const val BATTERY_CHAR_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
    const val RESET_CHAR_UUID = "0000fff3-0000-1000-8000-00805f9b34fb"
    
    const val SCAN_TIMEOUT_MS = 10000L
    const val CONNECTION_TIMEOUT_MS = 5000L
}
```

### 5.3 Repository Pattern

#### Repository Interface (Domain Layer)
```kotlin
interface WishRepository {
    // 오늘 카운트
    fun getTodayCount(): Flow<Int>
    fun getTodayRecord(): Flow<DailyRecord?>
    
    // 위시 관리
    suspend fun saveWish(text: String, target: Int)
    suspend fun incrementCount()
    
    // 기록 조회
    suspend fun getRecordByDate(date: String): DailyRecord?
    fun getRecentRecords(days: Int = 30): Flow<List<DailyRecord>>
    
    // BLE 상태
    fun getBleConnectionState(): Flow<BleConnectionState>
    fun getBatteryLevel(): Flow<Int>
}
```

---

## 6. Presentation Layer 설계

### 6.1 UDA (Unidirectional Data Architecture)

#### 핵심 원칙
1. **단방향 데이터 흐름**: View → Event → ViewModel → State → View
2. **불변 State**: State는 data class로 정의, copy()로만 업데이트
3. **Effect 분리**: 일회성 이벤트는 Effect로 처리
4. **단일 진실 공급원**: ViewModel이 State의 유일한 소유자

### 6.2 State 정의

#### HomeUiState
```kotlin
data class HomeUiState(
    // 카운터 관련
    val currentCount: Int = 0,
    val targetCount: Int = 1000,
    val progress: Float = 0f,           // 0.0 ~ 1.0
    val isCompleted: Boolean = false,
    
    // 위시 정보
    val wishText: String = "나는 매일 성장하고 있다.",
    val dateString: String = "",
    
    // BLE 상태
    val isConnected: Boolean = false,
    val batteryLevel: Int = 100,
    
    // UI 상태
    val isLoading: Boolean = false,
    val dailyRecords: List<DailyRecord> = emptyList()
)
```

#### WishInputUiState
```kotlin
data class WishInputUiState(
    val wishText: String = "",
    val targetCount: String = "1000",
    val isLoading: Boolean = false,
    
    // Validation
    val wishTextError: String? = null,
    val targetCountError: String? = null,
    val isValid: Boolean = false
)
```

### 6.3 Event 정의

#### HomeEvent
```kotlin
sealed class HomeEvent {
    object WishButtonClicked : HomeEvent()
    object ShareClicked : HomeEvent()
    data class RecordClicked(val date: String) : HomeEvent()
    object RefreshRequested : HomeEvent()
    object RetryConnection : HomeEvent()
}
```

### 6.4 Effect 정의

#### Navigation Effect
```kotlin
sealed class NavigationEffect {
    object NavigateToHome : NavigationEffect()
    object NavigateToWishInput : NavigationEffect()
    data class NavigateToDetail(val date: String) : NavigationEffect()
    object NavigateBack : NavigationEffect()
}
```

#### UI Effect
```kotlin
sealed class UiEffect {
    data class ShowToast(val message: String) : UiEffect()
    data class ShowError(val error: String) : UiEffect()
    data class ShareImage(
        val bitmap: Bitmap,
        val text: String,
        val hashtags: List<String>
    ) : UiEffect()
}
```

### 6.5 ViewModel 구조

#### BaseViewModel - 재사용 가능한 기본 구조
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
    
    // Helper Functions
    protected fun updateState(update: State.() -> State) {
        _uiState.value = _uiState.value.update()
    }
    
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
    
    // Common Functions
    protected suspend fun <T> withLoading(block: suspend () -> T): T {
        updateState { setLoading(true) }
        return try {
            block()
        } finally {
            updateState { setLoading(false) }
        }
    }
    
    protected fun handleError(throwable: Throwable) {
        // 공통 에러 처리
    }
}
```

#### BaseActivity - 공통 Activity 로직
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
    
    // System UI, 권한 등 공통 처리
    private fun setupSystemUI() { /* ... */ }
    protected open fun setupPermissions() { /* ... */ }
}
```

---

## 7. 페이지별 기능 명세

### 7.1 스플래시 화면

| 구분 | 내용 |
|------|------|
| **목적** | 앱 초기화 및 브랜딩 |
| **표시 시간** | 1.5 ~ 2초 |
| **UI 요소** | WISH RING 로고, 홀로그램 애니메이션 |
| **백그라운드 작업** | BLE 권한 체크, DB 초기화, 자동 연결 시도 |
| **전환** | 자동으로 홈 화면 이동 |

### 7.2 홈 화면 (Today's Count)

| 기능 | 설명 |
|------|------|
| **카운트 표시** | 실시간 카운트 (최대 99,999) |
| **진행률 게이지** | 원형 게이지, 목표 대비 % |
| **색상 변경** | 미달성: 보라색, 달성: 무지개 그라데이션 |
| **위시 버튼** | 탭 시 위시 입력 화면 이동 |
| **기록 리스트** | 최근 7일 기록 표시 |
| **배터리 표시** | BLE 디바이스 배터리 잔량 |
| **공유 버튼** | SNS 공유 기능 실행 |

### 7.3 위시 입력 화면

| 기능 | 설명 | Validation |
|------|------|------------|
| **위시 문장** | 텍스트 입력 | 1-100자 |
| **목표 횟수** | 숫자 입력 | 1-99,999 |
| **저장** | DB 저장 및 홈 복귀 | 유효성 통과 시 |
| **취소** | 변경사항 무시하고 복귀 | - |

### 7.4 상세 기록 화면

| 기능 | 설명 |
|------|------|
| **날짜 표시** | 선택한 날짜 |
| **카운트 정보** | 총 카운트, 목표, 달성률 |
| **위시 문장** | 해당일 위시 표시 |
| **캐릭터** | 위시링 캐릭터 이미지 |
| **리셋 로그** | 해당일 리셋 기록 |

### 7.5 SNS 공유 기능

| 단계 | 설명 |
|------|------|
| **1. 이미지 생성** | 홈 화면 카운트 영역 캡처 |
| **2. 텍스트 생성** | "오늘 나는 N번의 위시를 실천했습니다 ✨" |
| **3. 해시태그** | #WishRing #잠재의식 #긍정확언 #성공습관 |
| **4. 공유 시트** | Android 기본 공유 다이얼로그 |
| **5. 갤러리 저장** | 생성된 공유 카드를 갤러리에 저장하는 옵션 제공 |

---

## 8. MRD SDK 통신 사양

### 8.1 고수준 통신 프로토콜

#### SystemEnum 명령어 체계
| 명령어 | 용도 | 파라미터 | 응답 |
|-------|------|---------|------|
| SystemEnum.battery | 배터리 조회 | 1 (조회) | MrdReadEnum.BP |
| SystemEnum.brightness | 밝기 설정 | 0-100 | 상태 확인 |
| SystemEnum.firmware | 펌웨어 정보 | 1 (조회) | 버전 정보 |
| SystemEnum.reset | 디바이스 리셋 | 1 (실행) | MrdReadEnum.RESET |

#### MrdReadEnum 콜백 체계
```kotlin
// SDK 콜백 데이터 파싱
Manridy.setOnMrdReadListener { mrdReadEnum, data ->
    when (mrdReadEnum) {
        MrdReadEnum.HEART -> {
            // 카운터 데이터 (심박수 채널을 카운터로 활용)
            val count = parseCounterData(data)
            handleCounterIncrement(count)
        }
        MrdReadEnum.BP -> {
            // 배터리 데이터 (혈압 채널을 배터리로 활용)
            val batteryLevel = parseBatteryData(data)
            handleBatteryUpdate(batteryLevel)
        }
        MrdReadEnum.RESET -> {
            // 리셋 이벤트
            val previousCount = parseResetData(data)
            handleResetEvent(previousCount)
        }
    }
}
```

### 8.2 MRD SDK 연결 시나리오

#### 초기 연결 (SDK 관리)
1. MRD SDK 초기화
2. 콜백 리스너 등록
3. SDK 자동 디바이스 검색
4. 연결 상태 콜백 수신
5. 데이터 수신 준비 완료

#### 재연결 로직 (SDK 자동 처리)
1. SDK가 연결 끊김 자동 감지
2. 내부 재연결 로직 실행
3. 연결 상태 변경 콜백 전달
4. 앱에서 UI 상태만 업데이트

### 8.3 MRD SDK 백그라운드 처리

#### MRD 통합 Foreground Service
```kotlin
class MrdBleService : Service() {
    companion object {
        const val CHANNEL_ID = "wish_ring_mrd"
        const val NOTIFICATION_ID = 1001
    }
    
    private lateinit var mrdDataSource: MrdSdkDataSource
    
    override fun onCreate() {
        super.onCreate()
        // MRD SDK 서비스 레벨 초기화
        mrdDataSource = MrdSdkDataSource()
        mrdDataSource.initialize()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMrdConnection()
        return START_STICKY
    }
    
    private fun startMrdConnection() {
        // SDK 백그라운드 연결 유지
        mrdDataSource.startConnection()
    }
}
```

#### 권한 요구사항
- Android 12+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT  
- Android 11 이하: ACCESS_FINE_LOCATION
- 공통: FOREGROUND_SERVICE, POST_NOTIFICATIONS
- MRD SDK 전용 권한 (필요 시)

---

## 9. 공통 기능 및 데이터 관리

### 9.1 자정 처리 로직

#### 일일 데이터 생명주기
앱의 모든 데이터는 **로컬 타임존 자정(00:00)** 을 기준으로 관리됩니다.

| 시점 | 동작 | 설명 |
|------|------|------|
| **자정 00:00** | 새 일자 레코드 생성 | 새로운 날짜의 WishCountEntity 생성 (count=0) |
| **자정 직후** | 전일 데이터 확정 | 전일 데이터는 읽기 전용으로 고정 |
| **앱 실행 시** | 오늘 날짜 확인 | 없으면 자동 생성, 있으면 기존 데이터 로드 |

#### 구현 세부사항

```kotlin
// Repository에서 자정 처리
class WishRepositoryImpl {
    suspend fun ensureTodayRecord(): WishCountEntity {
        val today = LocalDate.now().toString() // "2025-01-02"
        val existing = wishCountDao.getByDate(today)
        
        return existing ?: run {
            // 자정 이후 첫 실행 시 새 레코드 생성
            val newRecord = WishCountEntity(
                date = today,
                totalCount = 0,
                wishText = "나는 매일 성장하고 있다.",
                targetCount = 1000,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            wishCountDao.insert(newRecord)
            newRecord
        }
    }
}

// ViewModel에서 자정 확인
class HomeViewModel {
    private val midnightChecker = MidnightChecker { 
        // 자정 감지 시 새 일자 데이터 생성
        viewModelScope.launch {
            repository.ensureTodayRecord()
            refreshTodayData()
        }
    }
}
```

#### 데이터 일관성 보장
1. **전일 데이터 불변성**: 자정 이후 과거 데이터는 수정 불가
2. **실시간 자정 감지**: 앱 실행 중 자정 전환 시 자동 처리
3. **오프라인 처리**: 네트워크 없어도 로컬 타임존 기준 정상 동작

### 9.2 즉시 반영 로직

#### 위시 입력 저장 시 실시간 갱신
사용자가 위시 입력 화면에서 데이터를 저장하면 **홈 화면에 즉시 반영**됩니다.

| 단계 | 동작 | 구현 |
|------|------|------|
| **1. 저장 버튼 클릭** | 유효성 검증 후 Repository 저장 | ViewModel 이벤트 처리 |
| **2. DB 업데이트** | 오늘 날짜 레코드 즉시 업데이트 | Repository → DAO 트랜잭션 |
| **3. 홈 화면 갱신** | StateFlow를 통한 자동 UI 갱신 | Flow 기반 반응형 업데이트 |
| **4. 화면 전환** | 입력 화면 닫고 홈으로 복귀 | Navigation Effect 처리 |

#### 구현 세부사항

```kotlin
// WishInputViewModel - 저장 로직
class WishInputViewModel {
    fun onEvent(event: WishInputEvent.SaveClicked) {
        viewModelScope.launch {
            // 1. 유효성 검증
            if (!isValid()) return@launch
            
            // 2. DB 즉시 업데이트
            repository.saveWish(
                text = uiState.value.wishText,
                target = uiState.value.targetCount.toInt()
            )
            
            // 3. 홈 화면 즉시 갱신을 위한 Effect
            _effect.send(NavigationEffect.NavigateToHome)
        }
    }
}

// Repository - 즉시 반영 보장
class WishRepositoryImpl {
    suspend fun saveWish(text: String, target: Int) {
        val today = LocalDate.now().toString()
        val existing = wishCountDao.getByDate(today) ?: ensureTodayRecord()
        
        // 즉시 업데이트 (Flow 자동 갱신)
        wishCountDao.update(
            existing.copy(
                wishText = text,
                targetCount = target,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Room Flow가 자동으로 HomeViewModel에 변경사항 전파
    }
}

// HomeViewModel - 자동 갱신
class HomeViewModel {
    private val todayRecord = repository.getTodayRecord()
        .stateIn(/* 즉시 반영 */)
}
```

#### 반응형 업데이트 보장
1. **Flow 기반 아키텍처**: Room DAO → Repository → ViewModel 자동 전파
2. **단일 진실 공급원**: Database가 모든 화면의 데이터 소스
3. **트랜잭션 보장**: 저장 실패 시 UI 변경 없음

---

## 10. 개발 일정

### 9.1 개발 단계별 일정

| Phase | 작업 내용 | 기간 | 산출물 |
|-------|----------|------|--------|
| **Phase 1** | Data Layer 구현 | 3일 | Room DB, Entity, DAO, Repository |
| **Phase 2** | BLE Integration | 3일 | BLE Manager, Service, GATT |
| **Phase 3** | Presentation Layer | 4일 | ViewModel, State, Effect |
| **Phase 4** | UI Implementation | 5일 | Compose Screens, Navigation |
| **Phase 5** | Features | 3일 | SNS 공유, 알림, 자동 리셋 |
| **Phase 6** | Testing & Polish | 2일 | 버그 수정, 최적화 |

**총 예상 기간: 20일**

### 9.2 상세 작업 순서

#### Phase 1: Data Layer (3일)
- [ ] Day 1: Room Database 설정, Entity 정의
- [ ] Day 2: DAO 구현, Database 마이그레이션
- [ ] Day 3: Repository 구현, DataStore 설정

#### Phase 2: BLE Integration (3일)
- [ ] Day 4: BLE Manager 기본 구조
- [ ] Day 5: GATT Callback, 데이터 파싱
- [ ] Day 6: Foreground Service, 재연결 로직

#### Phase 3: Presentation Layer (4일)
- [ ] Day 7: State/Event/Effect 정의
- [ ] Day 8: HomeViewModel 구현
- [ ] Day 9: WishInputViewModel 구현
- [ ] Day 10: DetailViewModel, Navigation 설정

#### Phase 4: UI Implementation (5일)
- [ ] Day 11: Theme 설정, 공통 컴포넌트
- [ ] Day 12: Splash Screen, 애니메이션
- [ ] Day 13: Home Screen, 게이지 구현
- [ ] Day 14: Input/Detail Screen
- [ ] Day 15: UI Polish, 전환 효과

#### Phase 5: Features (3일)
- [ ] Day 16: SNS 공유 기능
- [ ] Day 17: 일일 자동 리셋, 알림
- [ ] Day 18: 데이터 백업/복원

#### Phase 6: Testing & Polish (2일)
- [ ] Day 19: 통합 테스트, 버그 수정
- [ ] Day 20: 성능 최적화, 최종 점검

### 9.3 리스크 및 대응 방안

| 리스크 | 발생 가능성 | 영향도 | 대응 방안 |
|--------|------------|--------|----------|
| BLE 연결 불안정 | 중 | 높음 | Nordic BLE 라이브러리 사용 고려 |
| 백그라운드 제약 | 중 | 중간 | Foreground Service 최적화 |
| UI 성능 이슈 | 낮음 | 중간 | Compose 최적화, LazyColumn 사용 |
| 데이터 마이그레이션 | 낮음 | 높음 | Room 마이그레이션 전략 수립 |

---

## 10. 부록

### 10.1 Dependencies

```gradle
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // BLE
    implementation("no.nordicsemi.android:ble:2.7.0")  // Nordic BLE Library
    
    // Permission
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Image
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

### 10.2 Permissions (AndroidManifest.xml)

```xml
<!-- BLE Permissions -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Location (Android 11 이하) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" 
    android:maxSdkVersion="30" />

<!-- Foreground Service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Others -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Features -->
<uses-feature 
    android:name="android.hardware.bluetooth_le" 
    android:required="true" />
```

### 10.3 ProGuard Rules

```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Data classes
-keep class com.wishring.app.domain.model.** { *; }
-keep class com.wishring.app.data.local.entity.** { *; }
```

---

**문서 버전**: 1.0.0  
**작성일**: 2025년 1월 2일  
**작성자**: WISH RING Development Team  
**상태**: 개발 준비 완료

---

## 📝 개정 이력

| 버전 | 날짜 | 내용 | 작성자 |
|------|------|------|--------|
| 1.0.0 | 2025-01-02 | 초기 작성 | Team |