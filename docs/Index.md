# 📁 WISH RING Android 프로젝트 파일 인덱스

## 🏗️ 프로젝트 구조 개요

WISH RING 앱은 **Clean Architecture + MVVM** 패턴을 따르며, 다음 5개의 주요 레이어로 구성됩니다:
- **Presentation Layer**: UI 및 사용자 상호작용 (Compose + MVI)
- **Domain Layer**: 비즈니스 로직 및 규칙
- **Data Layer**: 데이터 소스 및 저장소 구현  
- **BLE Layer**: 스마트링 BLE 통신 전담
- **Core Layer**: 공통 유틸리티 및 기반 클래스

---

## 📦 Application Entry Points

### `WishRingApplication.kt`
- **위치**: `com.wishring.app`
- **역할**: Hilt 진입점, 앱 전역 초기화
- **주요 기능**: MRD SDK 초기화, 전역 설정
- **패턴**: `@HiltAndroidApp` 어노테이션 사용

### `MainActivity.kt`
- **위치**: `com.wishring.app`
- **역할**: 메인 액티비티 (단일 액티비티 아키텍처)
- **주요 기능**: Navigation Host, Compose UI 진입점
- **기술**: Activity + Navigation Compose

---

## 🎨 UI Theme System

### `ui/theme/Color.kt`
- **역할**: WISH Ring 브랜드 색상 정의 (Figma 디자인 기반)
- **내용**: Primary/Secondary 색상, 그라데이션, 상태별 색상

### `ui/theme/Theme.kt`
- **역할**: Material3 테마 설정 및 색상 스키마
- **기능**: Light/Dark 테마, 커스텀 색상 스키마

### `ui/theme/Type.kt`
- **역할**: 타이포그래피 시스템 정의
- **폰트**: Pretendard 폰트 패밀리, 커스텀 텍스트 스타일

### `ui/theme/Dimension.kt`
- **역할**: 공통 사이즈, 간격, 패딩 값 정의
- **내용**: 컴포넌트 크기, Margin/Padding 상수

---

## 💉 DI (Dependency Injection)

### Hilt Modules

#### `di/AppModule.kt`
- **역할**: 앱 전역 의존성 제공
- **제공**: Context, Coroutine Dispatchers, Room Database, DAOs
- **패턴**: `@Singleton` 스코프 사용

#### `di/BleModule.kt`  
- **역할**: BLE 관련 의존성 제공
- **제공**: BleRepository 바인딩, MrdProtocolAdapter
- **특징**: BLE 구현체를 도메인 인터페이스에 바인딩

#### `di/RepositoryModule.kt`
- **역할**: Repository 인터페이스 바인딩
- **바인딩**: WishCount, ResetLog, Preferences 저장소
- **패턴**: `@Binds` 추상 함수로 인터페이스 바인딩

---

## 🧩 Core Module

### Base Classes

#### `core/base/BaseActivity.kt`
- **역할**: 액티비티 공통 기능 (권한, 네비게이션)
- **기능**: 권한 요청 처리, 공통 UI 로직

#### `core/base/BaseViewModel.kt`
- **역할**: ViewModel 공통 기능 (State, Effect 처리)
- **패턴**: MVI 패턴 기본 구현, StateFlow/Effect 관리

#### `core/base/BaseDao.kt`
- **역할**: Room DAO 공통 인터페이스
- **기능**: 기본 CRUD 오퍼레이션 정의

### Utilities

#### `core/util/Constants.kt`
- **내용**: 앱 전역 상수 (BLE UUID, 데이터베이스 설정, 프리퍼런스 키)

#### `core/util/DateUtils.kt`
- **역할**: 날짜/시간 포맷팅 유틸리티
- **기능**: 날짜 변환, 포맷팅, 시간대 처리

#### `core/util/BlePermissionChecker.kt`
- **역할**: BLE 권한 확인 유틸리티
- **기능**: 런타임 권한 상태 확인, 요청 로직

---

## 🔵 BLE Layer (스마트링 통신)

> **🔥 중요**: BLE 구현이 두 위치에 분산되어 있음

### Main BLE Implementation (`ble/`)

#### `ble/BleRepositoryImpl.kt`
- **역할**: BLE Repository 메인 구현체 (DI에서 사용)
- **기능**: 디바이스 스캔/연결, 데이터 송수신, 실시간 건강 데이터
- **기술**: Nordic BLE 라이브러리, Flow 기반 리액티브 통신

#### `ble/MrdProtocolAdapter.kt` ⭐
- **역할**: MRD SDK 프로토콜 어댑터 (**ACTIVE** - DI에서 주입)
- **기능**: MRD 프로토콜 변환, 디바이스 통신 프로토콜 처리

#### `ble/BleConnectionManager.kt`
- **역할**: BLE 연결 상태 관리
- **기능**: 연결/해제, 재연결 로직, 연결 상태 모니터링

#### `ble/BleAutoConnectService.kt`
- **역할**: 백그라운드 자동 연결 서비스
- **기능**: 포어그라운드 서비스로 자동 재연결 처리

#### `ble/WishRingDeviceValidator.kt`
- **역할**: WISH Ring 디바이스 검증
- **기능**: 디바이스 타입 확인, UUID 검증

#### `ble/BleConstants.kt`
- **내용**: BLE 서비스/특성 UUID, 통신 상수

### BLE Data Models (`data/ble/model/`)

#### `data/ble/model/BleConnectionState.kt`
- **역할**: BLE 연결 상태 enum 정의
- **상태**: Disconnected, Connecting, Connected, Error

#### `data/ble/model/BleConstants.kt`
- **내용**: 추가 BLE 상수 및 설정 값

#### ⚠️ `data/ble/MrdProtocolAdapter.kt`
- **상태**: **UNUSED DUPLICATE** - 사용하지 않는 중복 파일
- **주의**: 메인 구현체는 `/ble/MrdProtocolAdapter.kt` 사용

---

## 🎯 Domain Layer

### Repository Interfaces

#### `domain/repository/BleRepository.kt`
- **역할**: BLE 통신 인터페이스 정의
- **기능**: 디바이스 스캔/연결, 데이터 송수신, 실시간 건강 데이터
- **패턴**: 의존성 역전 원칙 적용

#### `domain/repository/WishCountRepository.kt`
- **역할**: 소원 카운트 관리 인터페이스
- **기능**: 카운트 증가/리셋, 일별 통계, 히스토리 관리

#### `domain/repository/ResetLogRepository.kt`
- **역할**: 리셋 기록 관리 인터페이스
- **기능**: 리셋 이벤트 로깅, 통계 분석

#### `domain/repository/PreferencesRepository.kt`
- **역할**: 설정 관리 인터페이스
- **기능**: 사용자 프로필, 앱 설정, 디바이스 설정

### Domain Models

#### `domain/model/WishCount.kt`
- **역할**: 소원 카운트 도메인 모델
- **데이터**: 카운트, 목표, 날짜, 상태

#### `domain/model/DailyRecord.kt`
- **역할**: 일별 기록 모델
- **데이터**: 일별 통계, 달성률, 트렌드

#### `domain/model/UserProfile.kt`
- **역할**: 사용자 프로필 모델
- **데이터**: 개인정보, 건강 데이터, 목표 설정

#### `domain/model/DeviceSettings.kt`
- **역할**: 디바이스 설정 모델
- **데이터**: 진동 패턴, 알림 설정, 단위 설정

#### `domain/model/HealthData.kt`
- **역할**: 건강 데이터 모델
- **데이터**: 심박수, ECG, 운동 데이터

#### `domain/model/ResetLog.kt`
- **역할**: 리셋 로그 모델
- **데이터**: 리셋 시간, 이유, 카운트 히스토리

#### `domain/model/ResetEvent.kt`
- **역할**: 리셋 이벤트 모델
- **데이터**: 이벤트 타입, 메타데이터

---

## 💾 Data Layer

### Database (Room)

#### `data/local/database/WishRingDatabase.kt`
- **역할**: Room 데이터베이스 메인 클래스
- **기능**: 데이터베이스 인스턴스 관리, 마이그레이션, 콜백

#### DAOs (Data Access Objects)

##### `data/local/database/dao/WishCountDao.kt`
- **역할**: 소원 카운트 CRUD 오퍼레이션
- **기능**: Flow 기반 리액티브 쿼리, 통계 집계

##### `data/local/database/dao/ResetLogDao.kt`
- **역할**: 리셋 로그 CRUD 오퍼레이션
- **기능**: 로그 저장, 검색, 분석 쿼리

##### `data/local/dao/BleEventDao.kt`
- **역할**: BLE 이벤트 로깅
- **기능**: BLE 연결/해제 이벤트 저장

#### Entities

##### `data/local/database/entity/WishCountEntity.kt`
- **역할**: 소원 카운트 테이블 엔티티
- **컬럼**: id, count, targetCount, date, isCompleted

##### `data/local/database/entity/ResetLogEntity.kt`
- **역할**: 리셋 로그 테이블 엔티티
- **컬럼**: id, timestamp, reason, previousCount

##### `data/local/entity/BleEventLogEntity.kt`
- **역할**: BLE 이벤트 로그 테이블 엔티티
- **컬럼**: id, timestamp, eventType, deviceAddress

#### Converters

##### `data/local/database/converter/DateConverter.kt`
- **역할**: Date ↔ Long 타입 변환기
- **기능**: Room에서 Date 타입 지원

### Repository Implementations

#### `data/repository/WishCountRepositoryImpl.kt`
- **역할**: 소원 카운트 저장소 구현체
- **기술**: Room DAO + Flow, 코루틴 지원

#### `data/repository/ResetLogRepositoryImpl.kt`
- **역할**: 리셋 로그 저장소 구현체
- **기술**: Room DAO 기반 구현

#### `data/repository/PreferencesRepositoryImpl.kt`
- **역할**: 설정 저장소 구현체
- **기술**: DataStore Preferences 사용

---

## 🎨 Presentation Layer (Feature-based Architecture)

### 🧩 Shared Components

#### `presentation/component/`

##### `presentation/component/CircularProgress.kt`
- **역할**: 원형 프로그레스 인디케이터
- **기능**: 애니메이션 지원, 커스터마이징 가능

##### `presentation/component/WishCard.kt`
- **역할**: 소원 표시 카드 컴포넌트
- **기능**: 소원 내용, 진행률 표시

##### `presentation/component/CustomNumberPicker.kt`
- **역할**: 커스텀 숫자 선택기
- **기능**: 목표 설정용 숫자 입력

### 🧭 Navigation System

#### `presentation/navigation/NavGraph.kt`
- **역할**: Navigation Compose 그래프 정의
- **기능**: 화면 라우팅, 화면 간 전환, 딥링크 처리

### ⚡ Effects & Events

#### `presentation/effect/NavigationEffect.kt`
- **역할**: 네비게이션 관련 사이드 이펙트
- **기능**: 화면 전환, 딥링크 처리

#### `presentation/event/`
- **역할**: 각 화면별 공통 이벤트 정의
- **파일**: `HomeEvent.kt`, `WishInputEvent.kt`, `DetailEvent.kt`

### 🏠 Home Feature

#### `presentation/home/HomeViewModel.kt`
- **역할**: 홈 화면 비즈니스 로직
- **기능**: 실시간 카운트 업데이트, BLE 연결 상태, 데이터 로딩
- **패턴**: MVI 패턴으로 State/Event/Effect 관리

#### `presentation/home/HomeViewState.kt`
- **역할**: 홈 화면 UI 상태 정의
- **상태**: 카운트, 배터리, BLE 연결, 로딩 상태

#### `presentation/home/HomeEvent.kt`
- **역할**: 홈 화면 사용자 이벤트
- **이벤트**: 카운트 증가, 리셋, BLE 연결, 동기화

#### `presentation/home/HomeEffect.kt`
- **역할**: 홈 화면 사이드 이펙트
- **효과**: 네비게이션, 토스트, 에러 표시

#### `presentation/home/HomeScreen.kt`
- **역할**: 홈 화면 Compose UI
- **UI**: 위시 카운트, 배터리 표시, BLE 상태, 리포트 리스트

#### Components

##### `presentation/home/component/WishCountCard.kt`
- **역할**: 위시 카운트 메인 카드
- **기능**: 카운트 표시, 애니메이션, 진행률

##### `presentation/home/component/WishReportItem.kt`
- **역할**: 리포트 아이템 컴포넌트
- **기능**: 일별 통계 표시

##### `presentation/home/component/CircularGauge.kt`
- **역할**: 원형 게이지 (목표 달성률)
- **기능**: 애니메이션 게이지, 퍼센트 표시

##### `presentation/home/components/BleStatusCard.kt`
- **역할**: BLE 연결 상태 카드
- **기능**: 연결 상태, 배터리, 디바이스 정보

### 💫 Splash Feature

#### `presentation/splash/SplashScreen.kt`
- **역할**: 스플래시/로딩 화면
- **기능**: 그라데이션 배경, 로고 애니메이션, 자동 네비게이션

### ✏️ Wish Input Feature

#### `presentation/wishinput/WishInputViewModel.kt`
- **역할**: 소원 입력 비즈니스 로직
- **기능**: 텍스트 검증, 목표 설정, 저장 처리

#### `presentation/wishinput/WishInputViewState.kt`
- **역할**: 입력 화면 UI 상태
- **상태**: 입력 텍스트, 목표 횟수, 검증 상태

#### `presentation/wishinput/WishInputEvent.kt`
- **역할**: 입력 화면 사용자 이벤트
- **이벤트**: 텍스트 변경, 목표 설정, 저장

#### `presentation/wishinput/WishInputEffect.kt`
- **역할**: 입력 화면 사이드 이펙트
- **효과**: 키보드 제어, 저장 완료 알림

#### `presentation/wishinput/WishInputScreen.kt`
- **역할**: 소원 입력 화면 UI
- **UI**: 텍스트 입력, 목표 설정, 프리셋 선택

#### Models & Components

##### `presentation/wishinput/model/WishItem.kt`
- **역할**: 소원 아이템 모델
- **데이터**: 소원 텍스트, 목표, 카테고리

##### `presentation/wishinput/component/WishTextInput.kt`
- **역할**: 소원 텍스트 입력 컴포넌트
- **기능**: 멀티라인 입력, 글자 수 제한

##### `presentation/wishinput/component/TargetCountSelector.kt`
- **역할**: 목표 횟수 선택기
- **기능**: 숫자 입력, 프리셋 버튼

##### `presentation/wishinput/component/SuggestedWishes.kt`
- **역할**: 추천 소원 템플릿
- **기능**: 카테고리별 추천, 빠른 선택

### 📊 Wish Detail Feature

#### `presentation/wishdetail/WishDetailViewModel.kt`
- **역할**: 상세 화면 비즈니스 로직
- **기능**: 통계 데이터 처리, 차트 데이터 준비

#### `presentation/wishdetail/WishDetailViewState.kt`
- **역할**: 상세 화면 UI 상태
- **상태**: 선택된 날짜, 통계 데이터, 차트 데이터

#### `presentation/wishdetail/WishDetailEvent.kt`
- **역할**: 상세 화면 사용자 이벤트
- **이벤트**: 날짜 선택, 데이터 새로고침

#### `presentation/wishdetail/WishDetailEffect.kt`
- **역할**: 상세 화면 사이드 이펙트
- **효과**: 데이터 로딩, 에러 처리

#### `presentation/wishdetail/WishDetailScreen.kt`
- **역할**: 상세 화면 UI
- **UI**: 날짜별 상세 정보, 통계 차트, 동기부여 메시지

#### Components

##### `presentation/wishdetail/component/CountDisplay.kt`
- **역할**: 대형 카운트 표시 컴포넌트
- **기능**: 숫자 애니메이션, 목표 대비 표시

##### `presentation/wishdetail/component/DateSelector.kt`
- **역할**: 날짜 선택 네비게이터
- **기능**: 캘린더 선택, 이전/다음 네비게이션

##### `presentation/wishdetail/component/MotivationCard.kt`
- **역할**: 동기부여 메시지 카드
- **기능**: 달성률 기반 메시지, 격려 문구

### ⚙️ Settings Feature

#### `presentation/settings/SettingsViewModel.kt`
- **역할**: 설정 화면 비즈니스 로직
- **기능**: 디바이스 설정 관리, 사용자 프로필 관리

#### `presentation/settings/SettingsViewState.kt`
- **역할**: 설정 화면 UI 상태
- **상태**: 설정 값, BLE 연결, 동기화 상태

#### `presentation/settings/SettingsEvent.kt`
- **역할**: 설정 화면 사용자 이벤트
- **이벤트**: 설정 변경, BLE 연결, 데이터 초기화

#### `presentation/settings/SettingsEffect.kt`
- **역할**: 설정 화면 사이드 이펙트
- **효과**: 설정 저장, 디바이스 동기화

#### `presentation/settings/SettingsScreen.kt`
- **역할**: 설정 화면 UI
- **UI**: 디바이스 설정, 앱 설정, 데이터 관리

#### Components

##### `presentation/settings/component/SettingsItems.kt`
- **역할**: 설정 아이템 컴포넌트 모음
- **컴포넌트**: 토글 설정, 클릭 설정, BLE 연결 설정

### 📱 ViewModels

#### `presentation/viewmodel/BleConnectionViewModel.kt`
- **역할**: BLE 연결 전담 ViewModel
- **기능**: 연결 상태 관리, 디바이스 스캔, 자동 재연결

---

## 🧪 Test Files (Comprehensive Testing Structure)

### Unit Tests

#### Core & Utilities
- **`core/util/DateUtilsTest.kt`**: 날짜 유틸리티 함수 테스트
- **`core/util/BlePermissionCheckerTest.kt`**: 권한 확인 로직 테스트

#### BLE Implementation
- **`ble/BleRepositoryImplTest.kt`**: BLE Repository 구현체 테스트
- **`ble/MrdProtocolAdapterTest.kt`**: MRD 프로토콜 어댑터 테스트

#### Domain Layer
- **`domain/model/`**: 도메인 모델 테스트
  - `WishCountTest.kt`, `UserProfileTest.kt`, `HealthDataTest.kt`
  - `DailyRecordTest.kt`, `ResetLogTest.kt`
- **`domain/usecase/WishCountUseCaseTest.kt`**: UseCase 로직 테스트

#### Data Layer
- **`data/repository/`**: Repository 구현체 테스트
  - `WishCountRepositoryImplTest.kt`
  - `ResetLogRepositoryImplTest.kt`
  - `PreferencesRepositoryImplTest.kt`

#### Presentation Layer
- **`presentation/viewmodel/`**: ViewModel 테스트
  - `HomeViewModelTest.kt`
  - `SettingsViewModelTest.kt`
  - `WishInputViewModelTest.kt`
  - `DetailViewModelTest.kt`

### Advanced Testing

#### Property-based Tests
- **`property/WishCountPropertyTest.kt`**: 소원 카운트 속성 기반 테스트
- **`property/StatisticsPropertyTest.kt`**: 통계 계산 속성 테스트

#### Integration Tests
- **`integration/EndToEndScenarioTest.kt`**: 전체 시나리오 통합 테스트
- **`integration/DatabaseIntegrationTest.kt`**: 데이터베이스 통합 테스트

#### Concurrency Tests
- **`concurrency/StateConsistencyTest.kt`**: 상태 일관성 테스트
- **`concurrency/RaceConditionTest.kt`**: 경쟁 상태 테스트

#### Performance Tests
- **`performance/LargeDatasetTest.kt`**: 대용량 데이터 성능 테스트

### Instrumented Tests
- **`androidTest/`**: Android UI 및 통합 테스트
  - `ui/HomeScreenTest.kt`
  - `ui/WishInputScreenTest.kt`

---

## 📋 Configuration Files

### Gradle Configuration
- **`build.gradle.kts`** (app): 앱 모듈 빌드 설정
- **`build.gradle.kts`** (project): 프로젝트 레벨 설정
- **`settings.gradle.kts`**: 모듈 및 의존성 설정
- **`gradle.properties`**: Gradle 프로퍼티 및 최적화 설정
- **`gradle/libs.versions.toml`**: 의존성 버전 카탈로그

### Android Configuration
- **`AndroidManifest.xml`**: 앱 권한, 컴포넌트, 서비스 설정
- **`proguard-rules.pro`**: ProGuard 난독화 규칙

### External Dependencies
- **`app/libs/sdk_mrd20240218_1.1.5.aar`**: MRD SDK 라이브러리

---

## 🔍 Quick Navigation Guide

### 주요 진입점
1. **앱 시작**: `WishRingApplication` → `MainActivity`
2. **홈 화면**: `HomeViewModel` → `HomeScreen`
3. **BLE 통신**: `BleRepository` (domain) → `BleRepositoryImpl` (ble/)
4. **데이터 저장**: `WishRingDatabase` → 각 DAO → Repository

### 기능별 탐색
- **🏠 홈 기능**: `presentation/home/`
- **✏️ 소원 입력**: `presentation/wishinput/`
- **📊 상세 보기**: `presentation/wishdetail/`
- **⚙️ 설정**: `presentation/settings/`
- **🔵 BLE 통신**: `ble/` (메인) + `data/ble/model/` (모델)

### 데이터 흐름
1. **UI Event** → ViewModel (MVI 패턴)
2. **ViewModel** → Repository (Domain Interface)
3. **Repository** → Data Source (Room DB / BLE / DataStore)
4. **Data** → Repository → ViewModel (Flow/StateFlow)
5. **ViewModel** → UI (ViewState 업데이트)

### 테스트 탐색
- **단위 테스트**: `test/java/com/wishring/app/{layer}/`
- **고급 테스트**: `test/java/com/wishring/app/{property|integration|concurrency|performance}/`
- **UI 테스트**: `androidTest/java/com/wishring/app/ui/`

---

## 📝 Key Architecture Notes

- **MVI 패턴**: 모든 화면이 ViewState/Event/Effect 구조 사용
- **의존성 역전**: Domain 인터페이스 → Data 구현체 (Hilt 바인딩)
- **BLE 분리**: 별도 레이어로 분리하여 복잡성 관리
- **Flow 기반**: 모든 데이터 스트림이 Flow/StateFlow 사용
- **테스트 포괄성**: 단위→통합→속성→동시성→성능 테스트 전방위 커버리지
- **Compose UI**: 100% Jetpack Compose + Material3 디자인

---

## ⚠️ Critical File Locations

- **🔥 BLE 메인 구현**: `/ble/` (NOT `/data/ble/`)
- **💡 MRD 어댑터**: `/ble/MrdProtocolAdapter.kt` (ACTIVE)
- **❌ 사용 금지**: `/data/ble/MrdProtocolAdapter.kt` (DUPLICATE)
- **📱 메인 진입점**: `WishRingApplication.kt`, `MainActivity.kt`
- **🎯 DI 설정**: `/di/` 모든 모듈
- **🧪 테스트**: `/test/` 포괄적 테스트 구조

---

**버전**: 2.0.0  
**최종 업데이트**: 2025-01-13  
**패키지**: `com.wishring.app`  
**아키텍처**: Clean Architecture + MVVM + MVI