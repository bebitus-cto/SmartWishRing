# H13 Legacy BLE Scan Implementation

## 핵심 사항
- H13 기기는 `BluetoothAdapter.startLeScan()` legacy API로만 제대로 감지됨
- 새 API인 `BluetoothLeScanner.startScan()`으로는 감지 안됨
- 이름 없는 기기는 필터링해서 리스트에 표시 안함
- MRD SDK 데모 앱과 동일한 방식 사용

## 구현 방법
1. bluetoothAdapter.startLeScan(callback) 사용
2. 이름이 null이거나 빈 문자열인 기기는 무시
3. H13 또는 WISH RING 이름 포함 기기만 표시

## 테스트 확인사항
- H13 기기가 리스트에 나타나는지
- 이름 없는 기기가 필터링되는지
- 연결이 정상적으로 되는지