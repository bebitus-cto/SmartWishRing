package com.wishring.app.core.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Activity 기반 블루투스 권한 관리자
 * Runtime permission 요청 및 블루투스 활성화를 처리합니다
 */
class BlePermissionManager(
    private val activity: ComponentActivity
) {
    companion object {
        private const val TAG = "BlePermissionManager"
        
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
    private var onPermissionsDenied: ((List<String>) -> Unit)? = null
    private var onBluetoothEnabled: (() -> Unit)? = null
    private var onProgressUpdate: ((String) -> Unit)? = null
    
    // Launchers
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var bluetoothEnableLauncher: ActivityResultLauncher<Intent>? = null
    
    /**
     * Activity에서 onCreate에서 호출하여 launchers를 등록합니다
     */
    fun initialize() {
        // 권한 요청 launcher
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys.toList()
            
            if (deniedPermissions.isEmpty()) {
                Log.d(TAG, "All permissions granted")
                onPermissionsGranted?.invoke()
            } else {
                Log.w(TAG, "Permissions denied: $deniedPermissions")
                onPermissionsDenied?.invoke(deniedPermissions)
            }
        }
        
        // 블루투스 활성화 launcher
        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (isBluetoothEnabled()) {
                Log.d(TAG, "Bluetooth enabled by user")
                onBluetoothEnabled?.invoke()
            } else {
                Log.w(TAG, "Bluetooth enable request denied by user")
                onPermissionsDenied?.invoke(listOf("Bluetooth Enable Denied"))
            }
        }
    }
    
    /**
     * 전체 블루투스 설정 프로세스를 시작합니다
     */
    fun requestBluetoothSetup(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: (List<String>) -> Unit,
        onBluetoothEnabled: () -> Unit,
        onProgressUpdate: (String) -> Unit = {}
    ) {
        this.onPermissionsGranted = onPermissionsGranted
        this.onPermissionsDenied = onPermissionsDenied
        this.onBluetoothEnabled = onBluetoothEnabled
        this.onProgressUpdate = onProgressUpdate
        
        startPermissionFlow()
    }
    
    /**
     * 권한 요청 플로우 시작
     */
    private fun startPermissionFlow() {
        when {
            !isBluetoothSupported() -> {
                onProgressUpdate?.invoke("블루투스를 지원하지 않는 기기입니다")
                onPermissionsDenied?.invoke(listOf("Bluetooth Not Supported"))
            }
            !areAllPermissionsGranted() -> {
                onProgressUpdate?.invoke("블루투스 권한을 요청합니다...")
                requestPermissions()
            }
            !isBluetoothEnabled() -> {
                onProgressUpdate?.invoke("블루투스 활성화를 요청합니다...")
                requestBluetoothEnable()
            }
            else -> {
                onProgressUpdate?.invoke("블루투스 준비 완료!")
                onPermissionsGranted?.invoke()
            }
        }
    }
    
    /**
     * 모든 필요한 권한이 허용되었는지 확인
     */
    fun areAllPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 거부된 권한 목록을 반환
     */
    fun getDeniedPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 블루투스가 지원되는지 확인
     */
    fun isBluetoothSupported(): Boolean {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter != null
    }
    
    /**
     * 블루투스가 활성화되어 있는지 확인
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }
    
    /**
     * 권한 요청 실행
     */
    private fun requestPermissions() {
        if (areAllPermissionsGranted()) {
            startPermissionFlow() // 다음 단계로
            return
        }
        
        val deniedPermissions = getDeniedPermissions()
        Log.d(TAG, "Requesting permissions: $deniedPermissions")
        
        permissionLauncher?.launch(deniedPermissions.toTypedArray())
    }
    
    /**
     * 블루투스 활성화 요청
     */
    private fun requestBluetoothEnable() {
        if (isBluetoothEnabled()) {
            startPermissionFlow() // 다음 단계로
            return
        }
        
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher?.launch(enableBtIntent)
    }
    
    /**
     * 앱 설정 화면으로 이동
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
    
    /**
     * 권한별 설명 메시지
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH_SCAN -> "근처 WISH RING 기기를 찾기 위해 필요합니다"
            Manifest.permission.BLUETOOTH_CONNECT -> "WISH RING 기기와 연결하기 위해 필요합니다"
            Manifest.permission.ACCESS_FINE_LOCATION -> "BLE 기기 스캔을 위해 위치권한이 필요합니다 (GPS 사용 안함)"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "BLE 기기 스캔을 위해 위치권한이 필요합니다 (GPS 사용 안함)"
            Manifest.permission.BLUETOOTH -> "블루투스 기능 사용을 위해 필요합니다"
            Manifest.permission.BLUETOOTH_ADMIN -> "블루투스 설정 관리를 위해 필요합니다"
            else -> "앱 기능 사용을 위해 필요한 권한입니다"
        }
    }
    
    /**
     * 권한 거부 시 해결 방법 안내
     */
    fun getPermissionSolution(deniedPermissions: List<String>): String {
        return when {
            deniedPermissions.contains("Bluetooth Not Supported") -> 
                "이 기기는 블루투스를 지원하지 않습니다"
            deniedPermissions.contains("Bluetooth Enable Denied") -> 
                "설정에서 블루투스를 수동으로 활성화해주세요"
            deniedPermissions.any { it.contains("permission") } -> 
                "설정 → 앱 → WISH RING → 권한에서 필요한 권한을 허용해주세요"
            else -> 
                "블루투스 연결에 문제가 발생했습니다. 다시 시도해주세요"
        }
    }
    
    /**
     * 권한 상태 요약 정보
     */
    fun getPermissionStatusSummary(): String {
        return when {
            !isBluetoothSupported() -> "❌ 블루투스 미지원"
            !areAllPermissionsGranted() -> "⚠️ 권한 필요: ${getDeniedPermissions().size}개"
            !isBluetoothEnabled() -> "⚠️ 블루투스 비활성화"
            else -> "✅ 모든 설정 완료"
        }
    }
}