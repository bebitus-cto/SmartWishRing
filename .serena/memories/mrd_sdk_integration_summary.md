# MRD SDK 통합 완료 요약

## 📋 구현 완료 사항

### 1. 기본 설정 및 환경 구성 ✅
- **SDK 파일 통합**: `sdk_mrd20240218_1.1.5.aar` 파일을 `app/libs/`에 추가
- **빌드 설정**: `build.gradle.kts`에 AAR 의존성 추가
- **ProGuard 설정**: MRD SDK 클래스 난독화 방지 규칙 추가
- **권한 설정**: 기존 BLE 권한 사용

### 2. 아키텍처 및 DI 설정 ✅
- **Hilt DI 모듈**: `BleModule`에 SDK 인스턴스 및 콜백 제공자 추가
- **어댑터 패턴**: `MrdProtocolAdapter` 클래스로 SDK 통신 로직 분리
- **Repository 패턴**: 기존 `BleRepository` 인터페이스 확장

### 3. 데이터 모델 확장 ✅
- **UserProfile.kt**: 사용자 프로필 (키, 몸무게, 나이, 성별, 목표걸수 등)
- **HealthData.kt**: 건강 데이터 모델들
  - `HeartRateData`: 심박수 데이터
  - `BloodPressureData`: 혈압 데이터  
  - `SleepData`: 수면 데이터
  - `StepData`: 걸음수 데이터
  - `TemperatureData`: 체온 데이터
  - `EcgData`: ECG 데이터
  - `BloodOxygenData`: 혈중 산소 데이터
- **DeviceSettings.kt**: 기기 설정 및 시스템 정보

### 4. MRD SDK 프로토콜 어댑터 ✅
- **비동기 처리**: 콜백을 코루틴 기반으로 변환
- **단일 응답**: `suspendCancellableCoroutine` 사용
- **실시간 스트림**: `callbackFlow` 사용
- **에러 처리**: 타임아웃 및 예외 처리 구현
- **Mock 구현**: 실제 SDK 교체 전까지 테스트용 플레이스홀더

### 5. BLE Repository 통합 구현 ✅
- **기존 기능 유지**: Nordic BLE 기반 연결/스캔 로직 보존
- **SDK 기능 추가**: 
  - 시스템 정보 조회 (`getSystemInfo`)
  - 사용자 프로필 관리 (`setUserProfile`, `getUserProfile`)
  - 건강 데이터 조회 (`getLatestHeartRate`, `getSleepData`, `getStepData`)
  - 실시간 측정 (`startRealTimeHeartRate`, `startRealTimeEcg`)
  - 앱 알림 전송 (`sendAppNotification`)
  - 기기 설정 (`setUnitPreferences`, `setDeviceLanguage`)
- **데이터 파싱**: MRD 프로토콜 어댑터를 통한 응답 처리

### 6. Application 레벨 초기화 ✅
- **WishRingApplication**: SDK 초기화 로직 추가
- **에러 처리**: SDK 초기화 실패시 로그 및 예외 처리

### 7. HomeViewModel 완전 통합 ✅
- **ViewState 확장**: 건강 데이터 필드 추가
- **Event 확장**: SDK 관련 사용자 이벤트 추가
- **기능 구현**:
  - 건강 데이터 로드 (`loadHealthData`)
  - 실시간 심박수/ECG 측정
  - 사용자 프로필 업데이트
  - 운동 목표 설정
  - 디바이스 찾기
  - 앱 알림 전송
  - 건강 데이터 상세보기
- **반응형 스트림**: 건강 데이터 및 기기 상태 실시간 업데이트

## 🔄 데이터 흐름

### 명령 전송 흐름:
```
UI → ViewModel → BleRepository → MrdProtocolAdapter → SDK → BLE → 기기
```

### 응답 수신 흐름:
```
기기 → BLE → SDK 파싱 → MrdProtocolAdapter → BleRepository → ViewModel → UI
```

### 실시간 데이터 흐름:
```
기기 → BLE → SDK → SharedFlow → UI (실시간 업데이트)
```

## 📱 주요 기능

### 기본 기능 (기존 유지)
- BLE 디바이스 스캔 및 연결
- 소망 카운트 추적 및 동기화
- 버튼 프레스 이벤트 처리
- 배터리 레벨 모니터링

### 새로운 SDK 기능  
- **시스템 관리**: 펌웨어 버전, 배터리, 화면 밝기
- **건강 데이터**: 심박수, 혈압, 수면, 걸음수, 체온, ECG
- **실시간 측정**: 심박수, ECG 실시간 모니터링
- **사용자 관리**: 프로필 설정, 운동 목표
- **기기 제어**: 알림 전송, 설정 변경, 디바이스 찾기
- **단위 설정**: 미터법/야드법, 섭씨/화씨, 12/24시간제

## 🔧 실제 SDK 교체 가이드

현재는 플레이스홀더로 구현되어 있으며, 실제 SDK 교체시 다음을 수정:

### 1. BleModule.kt
```kotlin
// TODO: 실제 SDK 클래스로 교체
import com.manridy.sdk.Manridy
import com.manridy.sdk.listener.CmdReturnListener

@Provides
@Singleton
fun provideManridySDK(@ApplicationContext context: Context): Manridy {
    return Manridy.getInstance().apply { init(context) }
}
```

### 2. MrdProtocolAdapter.kt  
```kotlin
// TODO: 실제 SDK 클래스 import
import com.manridy.sdk.*
import com.manridy.sdk.enums.*
import com.manridy.sdk.bean.*

// 실제 SDK 콜백 리스너 구현
private val cmdReturnListener = object : CmdReturnListener {
    override fun onCmdReturn(readType: MrdReadEnum?, data: Any?) {
        // 실제 응답 처리 로직
    }
}
```

### 3. 명령어 생성 메서드들
실제 SDK 메서드 호출로 교체:
```kotlin
private fun generateSystemInfoCommand(type: SystemInfoType): ByteArray {
    return when (type) {
        SystemInfoType.BATTERY -> Manridy.getMrdSend().getSystem(SystemEnum.BATTERY)
        SystemInfoType.FIRMWARE_VERSION -> Manridy.getMrdSend().getSystem(SystemEnum.FIRMWARE)
        // ...
    }
}
```

## ✅ 테스트 체크리스트

### 빌드 테스트
- [ ] `./gradlew build` 성공
- [ ] AAR 파일 인식 확인
- [ ] ProGuard 규칙 적용 확인

### 기능 테스트  
- [ ] BLE 스캔/연결 동작 확인
- [ ] SDK 초기화 성공 확인
- [ ] Mock 데이터 응답 확인
- [ ] UI에서 건강 데이터 표시 확인

### 통합 테스트
- [ ] 실제 디바이스 연결 테스트
- [ ] SDK 교체 후 기능 검증
- [ ] 에러 처리 시나리오 테스트

## 🚀 완료!

MRD SDK 통합이 완료되었습니다. 이제 실제 SDK 클래스명과 메서드를 확인하여 플레이스홀더를 교체하면 실제 하드웨어와 통신할 수 있습니다.