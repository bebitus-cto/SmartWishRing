package com.wishring.app.core.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 단순화된 블루투스 권한 관리자
 * Runtime permission 요청 및 블루투스 활성화만 처리
 */
class SimpleBlePermissionManager(
    private val activity: ComponentActivity
) {
    companion object {
        private const val TAG = "SimpleBlePermissionManager"

        /**
         * Android 버전별 필요한 블루투스 권한 목록
         */
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Callbacks
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: (() -> Unit)? = null

    // Permission request launcher
    private val permissionRequestLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }

    // Bluetooth enable launcher
    private val enableBluetoothLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            checkBluetoothEnabled()
        }

    /**
     * 블루투스 권한 및 활성화 상태 체크
     */
    fun checkBluetoothPermissions(): Boolean = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 블루투스 활성화 상태 체크
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    /**
     * BLUETOOTH_SCAN 권한 보유 여부 확인 (Android 12+)
     * Android 12 미만에서는 항상 true를 반환합니다.
     */
    fun hasBluetoothScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 권한 요청 및 블루투스 활성화 플로우 시작
     */
    fun requestBluetoothSetup(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit = {}
    ) {
        this.onPermissionsGranted = onPermissionsGranted
        this.onPermissionsDenied = onPermissionsDenied

        when {
            !checkBluetoothPermissions() -> {
                Log.d(TAG, "권한 요청 필요")
                requestPermissions()
            }

            !isBluetoothEnabled() -> {
                Log.d(TAG, "블루투스 활성화 필요")
                requestEnableBluetooth()
            }

            else -> {
                Log.d(TAG, "모든 권한 승인됨")
                onPermissionsGranted()
            }
        }
    }

    /**
     * 권한 요청
     */
    private fun requestPermissions() {
        permissionRequestLauncher.launch(REQUIRED_PERMISSIONS)
    }

    /**
     * 권한 요청 결과 처리
     */
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Log.d(TAG, "모든 권한 승인됨")
            // 권한 승인 후 블루투스 활성화 체크
            if (!isBluetoothEnabled()) {
                requestEnableBluetooth()
            } else {
                onPermissionsGranted?.invoke()
            }
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys.toList()
            Log.w(TAG, "권한 거부됨: $deniedPermissions")
            onPermissionsDenied?.invoke()
        }
    }

    /**
     * 블루투스 활성화 요청
     */
    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            enableBluetoothLauncher.launch(enableBtIntent)
        } catch (e: Exception) {
            Log.e(TAG, "블루투스 활성화 요청 실패", e)
            onPermissionsDenied?.invoke()
        }
    }

    /**
     * 블루투스 활성화 상태 확인 후 콜백 호출
     */
    private fun checkBluetoothEnabled() {
        if (isBluetoothEnabled()) {
            Log.d(TAG, "블루투스 활성화됨")
            onPermissionsGranted?.invoke()
        } else {
            Log.w(TAG, "블루투스 활성화 거부됨")
            onPermissionsDenied?.invoke()
        }
    }
}