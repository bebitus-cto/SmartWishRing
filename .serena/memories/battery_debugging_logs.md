# 배터리 디버깅 로그 추가 내역

## 추가된 디버깅 로그 위치 및 내용

### 1. MainActivity.kt - BLE 데이터 수신
- **onCharacteristicChanged**: 
  - 모든 BLE 데이터 수신 시 `[BATTERY_DEBUG]` 태그로 로깅
  - 배터리 패턴 감지 시 특별 표시
  - HEX 데이터, 크기, UUID 정보 출력

### 2. MainActivity.kt - 배터리 데이터 처리
- **BATTERY 케이스**:
  - MRD SDK readEnum 및 jsonData 값 로깅
  - 파싱된 배터리 레벨 표시
  - MainViewModel 업데이트 전후 상태 로깅

### 3. MainActivity.kt - 폴백 배터리 파싱
- 직접 바이트 파싱 시 상세 로그
- 원시 데이터 크기, 헤더, 배터리 값 위치 표시

### 4. MainActivity.kt - 서비스 검색 및 연결
- **onServicesDiscovered**:
  - Notification 설정, 연결 상태 업데이트, 배터리 폴링 시작 로깅
  - 즉시 배터리 요청 추가 (1초 딜레이 후)

### 5. MainActivity.kt - Notification 설정
- **setupNotifications**:
  - 서비스 및 특성 찾기 성공 여부
  - Notification 활성화 및 Descriptor 쓰기 결과

### 6. MainActivity.kt - 배터리 폴링
- **startBatteryPolling**:
  - 30초 주기 배터리 요청 시 상태 로깅
  - GATT 연결 상태 및 디바이스 주소 표시

### 7. MainActivity.kt - 배터리 요청
- **requestBatteryLevel**:
  - 서비스/특성 찾기 성공 여부
  - 배터리 명령어 전송 및 결과 로깅

### 8. MainViewModel.kt - 배터리 업데이트
- **updateBatteryLevel** (suspend 제거):
  - 이전 배터리 레벨과 새 레벨 비교
  - StateFlow 업데이트 확인

### 9. HomeScreen.kt - UI 상태 모니터링
- **LaunchedEffect**:
  - batteryLevel, isConnected 변경 시 로깅
  - UI 표시 조건 확인

### 10. HomeScreen.kt - 배터리 아이콘 렌더링
- **FloatingBottomBar**:
  - 배터리 아이콘 렌더링 시 레벨 및 색상 로깅

## 디버깅 시 확인 사항

1. **BLE 연결 플로우**:
   - GATT 연결 → 서비스 검색 → Notification 설정 → 배터리 요청

2. **배터리 데이터 플로우**:
   - BLE 수신 → MRD SDK 파싱 → MainViewModel 업데이트 → HomeScreen UI 반영

3. **주요 체크포인트**:
   - MRD SDK가 "BATTERY" enum을 반환하는지
   - MainViewModel의 suspend 제거로 업데이트가 제대로 되는지
   - HomeScreen이 StateFlow 변경을 감지하는지
   - FloatingBottomBar가 조건에 맞게 배터리를 표시하는지

## 로그 필터링 명령어
```bash
adb logcat | grep BATTERY_DEBUG
```

## 문제 해결 단계
1. 위 명령어로 로그 확인
2. 어느 단계에서 끊기는지 파악
3. 해당 단계의 문제 해결