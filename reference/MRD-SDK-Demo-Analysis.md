# MRD SDK 데모 앱 분석 보고서

> H13 스마트링 버튼/카운트 데이터 처리를 위한 MRD SDK 구현 패턴 분석

**분석일**: 2025-01-19  
**목적**: WishRing 앱의 버튼 감지 로직 개선을 위한 실제 구현 패턴 발견

---

## 🎯 Executive Summary

### 핵심 발견사항
1. **실제 데이터 파싱**: `MrdPushCore.getInstance().readData(value)` 사용이 핵심
2. **WishRing 앱 문제점**: 추측 기반 패턴(`0xFC 0x3C 0x01`) 사용 중
3. **올바른 처리 방식**: Raw 데이터를 MRD SDK에 위임하여 파싱
4. **데이터 해석**: `Manridy.getMrdRead().read(datas)`로 구조화된 데이터 획득
5. **개선 방향**: 하드코딩된 패턴 매칭 대신 MRD SDK 활용

---

## 📂 File Index

### 분석 완료 파일들

| 파일명 | 역할 | 핵심 내용 |
|--------|------|-----------|
| **ParseDataAdapter.java** | 데이터 표시 어댑터 | 단순 RecyclerView 어댑터 (실제 파싱 X) |
| **MainActivity.java** | 메인 로직 & 이벤트 처리 | 📍 **핵심**: MRD SDK 사용법, 데이터 파싱 로직 |
| **BleAdapter.java** | BLE 통신 관리 | 📍 Raw 데이터 수신 및 MRD SDK 연동 |
| **BleTool.java** | BLE 유틸리티 | 바이트 변환, 어댑터 관리 |
| **ScanActivity.java** | 기기 스캔 | 기기 검색 및 연결 초기화 |

---

## 🔄 데이터 처리 플로우

### 올바른 MRD SDK 처리 방식
```
1. Raw BLE 데이터 수신 (onCharacteristicChanged)
   ↓
2. MRD SDK에 데이터 전달 (MrdPushCore.getInstance().readData(value))
   ↓  
3. 파싱된 데이터 요청 (Manridy.getMrdRead().read(datas))
   ↓
4. 구조화된 JSON 데이터 획득 (MrdReadRequest)
   ↓
5. UI 업데이트 또는 로직 처리
```

### 현재 WishRing 앱의 잘못된 방식
```
❌ Raw 데이터 → 하드코딩된 패턴 매칭 (0xFC 0x3C 0x01) → 버튼 이벤트
```

### 개선된 방식
```
✅ Raw 데이터 → MRD SDK 파싱 → 구조화된 이벤트 → 버튼 처리
```

---

## 💡 MRD SDK 올바른 사용법

### 1. 초기화 (BleAdapter.java:57)
```java
// GATT 연결 시 MRD SDK 초기화
gatt = device.connectGatt(context, false, this);
MrdPushCore.getInstance().init(gatt);
```

### 2. Raw 데이터 처리 (BleAdapter.java:163-169)
```java
@Override
public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    byte[] value = characteristic.getValue();
    
    // 🔥 핵심: MRD SDK에 Raw 데이터 전달
    MrdPushCore.getInstance().readData(value);
    
    // 앱 로직으로 전달
    putHandler(value, gatt.getDevice());
    Log.i(TAG, "read=" + BleTool.ByteToString(value));
}
```

### 3. 파싱된 데이터 획득 (MainActivity.java:704-723)
```java
@Override
public void onReadChange(BluetoothDevice device, byte[] datas) {
    // 🔥 핵심: MRD SDK로 데이터 파싱
    MrdReadRequest readRequest = Manridy.getMrdRead().read(datas);
    
    String body;
    Log.i("MrdRead", "read enum type is : "
            + readRequest.getMrdReadEnum()
            + " body is : "
            + readRequest.getJson()
    );
    
    if (readRequest.getMrdReadEnum() == MrdReadEnum.Failure) {
        body = "불지원 명령어";
    } else if (readRequest.getStatus() == 0) {
        body = "파싱 오류";
    } else {
        body = readRequest.getMrdReadEnum().name();
        if (!TextUtils.isEmpty(readRequest.getJson())) {
            body += "=" + formatJson(readRequest.getJson());
        }
    }
    
    // UI 업데이트
    dataAdapter.emit(body);
}
```

### 4. MTU 및 쓰기 처리
```java
@Override
public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    super.onMtuChanged(gatt, mtu, status);
    MrdPushCore.getInstance().onMtuChanged(mtu, status);
}

@Override 
public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicWrite(gatt, characteristic, status);
    MrdPushCore.getInstance().onCharacteristicWrite(status, characteristic);
}
```

---

## 🔍 버튼 이벤트 감지 패턴

### WishRing 앱 현재 방식 (잘못됨)
```kotlin
// 추측에 기반한 하드코딩
when {
    data.size >= 3 && data[0] == 0xFC.toByte() && data[1] == 0x3C.toByte() && data[2] == 0x01.toByte() -> {
        Log.i(WR_EVENT, "[BleRepositoryImpl] 버튼 이벤트 감지")
        val event = ButtonPressEvent(...)
        _buttonPressEvents.tryEmit(event)
    }
}
```

### 올바른 방식 (MRD SDK 활용)
```kotlin
// onCharacteristicChanged에서
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
    val data = characteristic.value
    
    // Raw 데이터를 MRD SDK에 전달
    MrdPushCore.getInstance().readData(data)
    
    // 파싱 결과 처리
    processParsedData(data)
}

private fun processParsedData(rawData: ByteArray) {
    val readRequest = Manridy.getMrdRead().read(rawData)
    
    when (readRequest.mrdReadEnum) {
        MrdReadEnum.ButtonPress -> {
            // 실제 버튼 이벤트 처리
            val event = ButtonPressEvent(
                timestamp = System.currentTimeMillis(),
                pressCount = extractPressCount(readRequest.json),
                pressType = extractPressType(readRequest.json)
            )
            _buttonPressEvents.tryEmit(event)
        }
        // 다른 이벤트 타입들...
    }
}
```

---

## 🛠️ WishRing 앱 개선 방안

### 현재 문제점
1. **하드코딩된 패턴**: `0xFC 0x3C 0x01` 추측 기반
2. **MRD SDK 미활용**: Raw 데이터를 직접 파싱 시도
3. **불완전한 이벤트 감지**: 다양한 버튼 누름 방식 미지원
4. **유지보수성 부족**: 패턴 변경 시 코드 수정 필요

### 개선 방안

#### 1. MRD SDK 통합 강화
```kotlin
// BleRepositoryImpl.kt 수정
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
    val data = characteristic.value
    
    // ❌ 기존 방식: 하드코딩된 패턴 매칭 제거
    // ✅ 새로운 방식: MRD SDK 활용
    MrdPushCore.getInstance().readData(data)
    
    // 파싱된 데이터 처리
    handleParsedData(data)
}

private fun handleParsedData(rawData: ByteArray) {
    val readRequest = Manridy.getMrdRead().read(rawData)
    
    when (readRequest.mrdReadEnum) {
        MrdReadEnum.ButtonEvent -> handleButtonEvent(readRequest)
        MrdReadEnum.Battery -> handleBatteryEvent(readRequest) 
        MrdReadEnum.HeartRate -> handleHeartRateEvent(readRequest)
        // 다른 이벤트들...
    }
}
```

#### 2. 이벤트 타입별 세분화 처리
```kotlin
private fun handleButtonEvent(readRequest: MrdReadRequest) {
    if (readRequest.status != 1) return
    
    val json = readRequest.json
    if (json.isNotEmpty()) {
        val buttonData = parseButtonData(json)
        
        val event = ButtonPressEvent(
            timestamp = System.currentTimeMillis(),
            pressCount = buttonData.count,
            pressType = when (buttonData.type) {
                "single" -> PressType.SINGLE
                "double" -> PressType.DOUBLE  
                "long" -> PressType.LONG
                else -> PressType.SINGLE
            },
            pressure = buttonData.pressure
        )
        
        _buttonPressEvents.tryEmit(event)
        _buttonPresses.tryEmit(buttonData.count)
    }
}
```

#### 3. 설정 가능한 이벤트 매핑
```kotlin
// 이벤트 타입을 설정으로 관리
class BleEventMapper {
    private val eventMap = mapOf(
        MrdReadEnum.ButtonPress to ::handleButtonPress,
        MrdReadEnum.Battery to ::handleBattery,
        // 확장 가능한 구조
    )
    
    fun processEvent(readRequest: MrdReadRequest) {
        eventMap[readRequest.mrdReadEnum]?.invoke(readRequest)
    }
}
```

---

## 📊 데이터 타입 및 구조

### MRD SDK에서 제공하는 주요 이벤트 타입들
```java
// MainActivity.java에서 확인된 이벤트들
- SystemEnum.version (펌웨어)
- SystemEnum.battery (배터리)  
- getStep() (걸음수)
- getHrData() (심박수)
- getBpData() (혈압)
- getSleep() (수면)
// ... 기타 다양한 이벤트들
```

### 예상 버튼 이벤트 구조
```json
{
  "type": "button_press",
  "count": 1,
  "pressure": 75,
  "duration": 120,
  "timestamp": 1642123456789
}
```

---

## 🔧 구현 우선순위

### Phase 1: 핵심 MRD SDK 통합
1. **기존 패턴 매칭 제거**
2. **MRD SDK 파싱 로직 적용**  
3. **기본 버튼 이벤트 감지 구현**

### Phase 2: 이벤트 세분화
1. **다양한 버튼 누름 방식 지원**
2. **압력 감지 (지원되는 경우)**
3. **배터리, 연결 상태 등 부가 이벤트**

### Phase 3: 안정성 및 최적화
1. **오류 처리 강화**
2. **성능 최적화**
3. **로깅 및 디버깅 개선**

---

## 🎯 결론 및 권장사항

### 핵심 권장사항
1. **즉시 적용**: 하드코딩된 패턴을 MRD SDK 파싱으로 교체
2. **구조적 개선**: 이벤트 처리를 타입별로 분리
3. **확장성 확보**: 새로운 이벤트 타입 추가 용이하게 구조 설계
4. **실제 테스트**: H13 기기로 실제 버튼 이벤트 패턴 확인

### 기대 효과
- ✅ **정확한 버튼 감지**: 추측이 아닌 검증된 SDK 활용
- ✅ **다양한 이벤트 지원**: 단일/더블/길게 누름 구분 가능
- ✅ **유지보수성 향상**: 패턴 변경에 유연하게 대응
- ✅ **안정성 증대**: SDK의 검증된 파싱 로직 활용

### 다음 단계
1. **WishRing 앱에 적용**: 분석 결과를 바탕으로 코드 개선
2. **실제 기기 테스트**: H13으로 버튼 이벤트 검증
3. **성능 모니터링**: 개선된 로직의 안정성 확인
4. **사용자 피드백 수집**: 실제 사용 환경에서의 정확도 평가

---

**분석 완료일**: 2025-01-19  
**다음 리뷰 예정**: WishRing 앱 적용 후