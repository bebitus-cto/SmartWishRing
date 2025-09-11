# 📁 WISH RING Android 프로젝트 파일 인덱스

## 🏗️ 프로젝트 구조 개요

WISH RING 앱은 **Clean Architecture + MVVM** 패턴을 따르며, 다음 3개의 주요 레이어로 구성됩니다:
- **Presentation Layer**: UI 및 사용자 상호작용
- **Domain Layer**: 비즈니스 로직 및 규칙
- **Data Layer**: 데이터 소스 및 저장소 구현

---

## 📦 Application

### `WishRingApplication.kt`
- **위치**: `com.wishring.app`
- **역할**: Hilt 진입점, 앱 전역 초기화
- **주요 기능**: MRD SDK 초기화, 전역 설정

### `MainActivity.kt`
- **위치**: `com.wishring.app`
- **역할**: 앱의 메인 액티비티
- **주요 기능**: Navigation Host, Compose UI 진입점

---

## 🧩 Core Module

### Base Classes
- **`BaseActivity.kt`**: 액티비티 공통 기능 (권한, 네비게이션)
- **`BaseViewModel.kt`**: ViewModel 공통 기능 (State, Effect 처리)
- **`BaseDao.kt`**: Room DAO 공통 인터페이스

### Utilities
- **`Constants.kt`**: 앱 전역 상수 정의 (BLE UUID, 프리퍼런스 키)
- **`DateUtils.kt`**: 날짜/시간 포맷팅 유틸리티

---

## 💉 DI (Dependency Injection)

### Hilt Modules
- **`AppModule.kt`**: 앱 전역 의존성 (Context, Dispatcher, Room DB)
- **`BleModule.kt`**: BLE 관련 의존성 (MRD SDK, Manager)
- **`RepositoryModule.kt`**: Repository 인터페이스 바인딩

---

## 🎯 Domain Layer

### Repository Interfaces
- **`BleRepository.kt`**: BLE 통신 인터페이스
  - 디바이스 스캔/연결
  - 데이터 송수신
  - 실시간 건강 데이터
  
- **`WishRepository.kt`**: 소원 관리 인터페이스
  - 소원 생성/수정/삭제
  
- **`WishCountRepository.kt`**: 카운트 관리 인터페이스
  - 카운트 증가/리셋
  - 일별 통계
  
- **`PreferencesRepository.kt`**: 설정 관리 인터페이스
  - 사용자 프로필
  - 앱 설정
  
- **`ResetLogRepository.kt`**: 리셋 기록 관리 인터페이스

### Domain Models
- **`WishCount.kt`**: 소원 카운트 데이터 모델
- **`DailyRecord.kt`**: 일별 기록 모델
- **`UserProfile.kt`**: 사용자 프로필 모델
- **`DeviceSettings.kt`**: 디바이스 설정 모델
- **`HealthData.kt`**: 건강 데이터 모델
- **`ResetLog.kt`**: 리셋 로그 모델

### Utilities
- **`util/`**: 도메인 레이어 유틸리티 함수들

---

## 💾 Data Layer

### BLE Implementation
- **`BleRepositoryImpl.kt`**: BLE Repository 구현체
  - Nordic BLE 라이브러리 사용
  - 연결 상태 관리
  - 커맨드 큐 처리
  
- **`MrdProtocolAdapter.kt`**: MRD SDK 어댑터
  - MRD 프로토콜 변환
  - 디바이스 통신 프로토콜

### Database
- **`WishRingDatabase.kt`**: Room 데이터베이스 정의
  - 버전 관리
  - 마이그레이션

#### DAOs
- **`WishCountDao.kt`**: 소원 카운트 CRUD
- **`ResetLogDao.kt`**: 리셋 로그 CRUD

#### Entities  
- **`WishCountEntity.kt`**: 소원 카운트 테이블
- **`ResetLogEntity.kt`**: 리셋 로그 테이블

#### Converters
- **`DateConverter.kt`**: Date ↔ Long 타입 변환

### Repository Implementations
- **`WishCountRepositoryImpl.kt`**: 카운트 저장소 구현
- **`PreferencesRepositoryImpl.kt`**: DataStore 기반 설정 저장
- **`ResetLogRepositoryImpl.kt`**: 리셋 로그 저장소 구현

### BLE Models
- **`BleConnectionState.kt`**: BLE 연결 상태 enum
- **`BleConstants.kt`**: BLE UUID 및 상수

---

## 🎨 Presentation Layer (Feature-based Architecture)

### 🎨 Theme System  
- **`ui/theme/Color.kt`**: WISH Ring 색상 정의 (Figma 추출)
  - Primary/Secondary 색상
  - 그라데이션 색상
  - 상태별 색상
  
- **`ui/theme/Theme.kt`**: Material3 테마 설정
  - Light/Dark 테마
  - Custom color scheme
  
- **`ui/theme/Type.kt`**: 타이포그래피 정의
  - Pretendard 폰트
  - 커스텀 텍스트 스타일
  
- **`ui/theme/Dimension.kt`**: 공통 사이즈 및 간격
  - Padding/Margin 값
  - 컴포넌트 크기

### 🧩 Common Components
- **`presentation/component/CircularProgress.kt`**: 원형 프로그레스 인디케이터
  - 애니메이션 지원
  - 커스터마이징 가능
  
- **`presentation/component/BatteryIndicator.kt`**: 배터리 상태 표시
  - 레벨별 색상 변경
  - 퍼센티지 표시

### 🏠 Home Feature
- **`home/HomeViewModel.kt`**: 홈 화면 비즈니스 로직
  - 실시간 카운트 업데이트
  - BLE 연결 상태
  
- **`home/HomeViewState.kt`**: 홈 화면 UI 상태
- **`home/HomeEvent.kt`**: 홈 화면 사용자 이벤트
- **`home/HomeEffect.kt`**: 홈 화면 사이드 이펙트
- **`home/HomeScreen.kt`**: 홈 화면 Compose UI
  - 위시 카운트 표시
  - 리포트 리스트
  
- **`home/component/WishCountCard.kt`**: 위시 카운트 카드
- **`home/component/WishReportItem.kt`**: 리포트 아이템
- **`home/component/CircularGauge.kt`**: 원형 게이지 컴포넌트

### 🚀 Loading Feature
- **`loading/LoadingScreen.kt`**: 스플래시/로딩 화면
  - 그라데이션 배경
  - 로고 애니메이션
  - 자동 네비게이션

### ✏️ Wish Input Feature  
- **`wishinput/WishInputViewModel.kt`**: 소원 입력 로직
- **`wishinput/WishInputViewState.kt`**: 입력 화면 상태
- **`wishinput/WishInputEvent.kt`**: 입력 이벤트
- **`wishinput/WishInputEffect.kt`**: 입력 효과
- **`wishinput/WishInputScreen.kt`**: 소원 입력 화면 UI
  - 텍스트 입력
  - 목표 설정
  - 프리셋 선택
  
- **`wishinput/component/WishTextInput.kt`**: 소원 텍스트 입력 컴포넌트
- **`wishinput/component/TargetCountSelector.kt`**: 목표 횟수 선택기
- **`wishinput/component/SuggestedWishes.kt`**: 추천 소원 템플릿

### 📊 Detail Feature
- **`detail/DetailViewModel.kt`**: 상세 화면 로직
  - 통계 데이터 처리
  - 차트 데이터 준비
  
- **`detail/DetailViewState.kt`**: 상세 화면 상태
- **`detail/DetailEvent.kt`**: 상세 화면 이벤트
- **`detail/DetailEffect.kt`**: 상세 화면 효과
- **`detail/DetailScreen.kt`**: 상세 화면 UI
  - 날짜별 상세 정보
  - 통계 및 차트
  - 동기부여 메시지
  
- **`detail/component/CountDisplay.kt`**: 대형 카운트 표시
- **`detail/component/DateSelector.kt`**: 날짜 선택 네비게이터
- **`detail/component/MotivationCard.kt`**: 동기부여 메시지 카드

### ⚙️ Settings Feature
- **`settings/SettingsViewModel.kt`**: 설정 화면 로직
  - 디바이스 설정 관리
  - 사용자 프로필 관리
  
- **`settings/SettingsViewState.kt`**: 설정 상태
- **`settings/SettingsEvent.kt`**: 설정 이벤트
- **`settings/SettingsEffect.kt`**: 설정 효과
- **`settings/SettingsScreen.kt`**: 설정 화면 UI
  - 디바이스 설정
  - 앱 설정
  - 데이터 관리
  
- **`settings/component/SettingsItems.kt`**: 설정 아이템 컴포넌트
  - 토글 설정
  - 클릭 가능 설정
  - 블루투스 연결


### 🧭 Navigation
- **`navigation/NavGraph.kt`**: Navigation 그래프
  - 화면 라우팅 정의
  - 화면 간 전환 로직
  - 딥링크 처리

### 🎯 Presentation Events
- **`event/HomeEvent.kt`**: 홈 화면 공통 이벤트
- **`event/WishInputEvent.kt`**: 소원 입력 공통 이벤트  
- **`event/DetailEvent.kt`**: 상세 화면 공통 이벤트

### ⚡ Presentation Effects
- **`effect/NavigationEffect.kt`**: 네비게이션 관련 사이드 이펙트

---

## 🧪 Test Files

### Unit Tests
- **`presentation/viewmodel/*ViewModelTest.kt`**: 각 ViewModel 유닛 테스트
- **`data/repository/*RepositoryImplTest.kt`**: Repository 구현체 테스트
- **`domain/model/*Test.kt`**: 도메인 모델 테스트
- **`domain/usecase/WishCountUseCaseTest.kt`**: UseCase 테스트
- **`ble/MrdProtocolAdapterTest.kt`**: BLE 프로토콜 어댑터 테스트
- **`core/util/DateUtilsTest.kt`**: 유틸리티 테스트

### Property-based Tests  
- **`property/WishCountPropertyTest.kt`**: 소원 카운트 속성 테스트
- **`property/StatisticsPropertyTest.kt`**: 통계 속성 테스트

### Integration Tests
- **`integration/EndToEndScenarioTest.kt`**: 전체 시나리오 통합 테스트
- **`integration/DatabaseIntegrationTest.kt`**: 데이터베이스 통합 테스트

### Concurrency Tests
- **`concurrency/StateConsistencyTest.kt`**: 상태 일관성 테스트
- **`concurrency/RaceConditionTest.kt`**: 경쟁 상태 테스트

### Performance Tests
- **`performance/LargeDatasetTest.kt`**: 대용량 데이터 성능 테스트

### Instrumented Tests
- **`ui/HomeScreenTest.kt`**: 홈 화면 UI 테스트
- **`ui/WishInputScreenTest.kt`**: 소원 입력 화면 UI 테스트

---

## 📋 Configuration Files

### Gradle
- **`build.gradle.kts`** (app): 앱 모듈 설정
- **`build.gradle.kts`** (project): 프로젝트 설정
- **`settings.gradle.kts`**: 모듈 설정
- **`gradle.properties`**: Gradle 프로퍼티

### Android
- **`AndroidManifest.xml`**: 앱 권한 및 구성
- **`proguard-rules.pro`**: ProGuard 규칙

---

## 🔍 Quick Navigation

### 주요 진입점
1. **앱 시작**: `WishRingApplication` → `MainActivity`
2. **홈 화면**: `HomeViewModel` → `HomeScreen`
3. **BLE 통신**: `BleRepository` → `BleRepositoryImpl`
4. **데이터 저장**: `WishRingDatabase` → 각 DAO

### 데이터 흐름
1. **UI Event** → ViewModel
2. **ViewModel** → UseCase/Repository
3. **Repository** → Data Source (DB/BLE)
4. **Data** → ViewModel (Flow/StateFlow)
5. **ViewModel** → UI (ViewState)

---

## 📝 Notes

- 모든 Repository는 인터페이스를 통해 의존성 역전 원칙 적용
- ViewModel은 Android 프레임워크에 의존하지 않음
- BLE 통신은 MRD SDK를 통해 추상화됨
- 모든 데이터베이스 작업은 suspend 함수 또는 Flow로 처리