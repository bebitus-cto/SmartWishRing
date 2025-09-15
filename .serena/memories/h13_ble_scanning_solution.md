# H13 BLE Device Scanning Solution

## Problem
The WISH RING device (H13 with MAC address D9:18:66:19:10:E9) was not being detected by our app, even though it appeared in the ECTRI utility app. The device doesn't show up in Android system Bluetooth settings, indicating it's a BLE-only device with special advertising.

## Root Cause
Our app was using the newer `BluetoothLeScanner` API with `startScan()`, while the MRD SDK demo (which successfully detects H13 devices) uses the older `BluetoothAdapter.startLeScan()` API.

## Solution
Changed BleConnectionManager to use the legacy scanning API:
- Replaced `BluetoothLeScanner.startScan()` with `BluetoothAdapter.startLeScan()`
- Filter out devices with empty names (as per MRD SDK)
- Remove UUID filtering to detect all BLE devices

## Key Implementation Details
```kotlin
// Legacy API usage
val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
    // Filter empty names
    if (name.isNullOrEmpty()) return@LeScanCallback
    // Process device...
}
bluetoothAdapter.startLeScan(leScanCallback)
```

## Files Modified
- `app/src/main/java/com/wishring/app/ble/managers/BleConnectionManager.kt`
  - `startScanning()` method - Uses legacy API
  - `stopScanning()` method - Updated for legacy API

## Notes
- The legacy API shows deprecation warnings but is necessary for H13 compatibility
- This matches the scanning approach in `reference/mrd-sdkdemo/`
- The H13 device uses special BLE advertising that may not be fully compatible with newer scanning APIs