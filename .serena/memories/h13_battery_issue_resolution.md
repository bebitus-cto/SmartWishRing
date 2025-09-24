# H13 배터리 연결 문제 해결

## 문제 상황
- MRD SDK의 `getSystem(SystemEnum.battery, 1)`이 반환하는 객체의 `.datas`가 null
- BluetoothGatt.writeCharacteristic()에 null이 전달되어 배터리 요청 실패
- 에러 메시지: "value must not be null, mClient= 8"

## H13 기기 특성
- **자동 배터리 전송**: H13 기기는 약 12초 간격으로 배터리 데이터를 자동 전송
- 배터리 데이터 패턴: HEX로 `0F0619091812...62...` 형태
- 자동으로 98% 등의 배터리 레벨이 수신됨

## 해결 방법
1. **매개변수 변경 시도**: getSystem(SystemEnum.battery, 0) 먼저 시도
2. **Null 체크 강화**: commandData.datas null 체크 추가
3. **자동 수신 의존**: H13은 자동으로 배터리를 전송하므로 수동 요청 실패해도 문제 없음

## 코드 수정 내용
- MainActivity.kt의 requestBatteryLevel() 함수에서:
  - 매개변수 0과 1을 순차적으로 시도
  - .datas null 체크 추가
  - 실패 시 자동 수신에 의존한다는 로그 추가

## 결론
H13 기기는 배터리 데이터를 자동으로 주기적 전송하므로, 수동 요청이 실패해도 배터리 정보를 정상적으로 받을 수 있음.