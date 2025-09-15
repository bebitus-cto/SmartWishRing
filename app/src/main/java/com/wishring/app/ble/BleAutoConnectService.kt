package com.wishring.app.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.wishring.app.R
import com.wishring.app.di.IoDispatcher
import com.wishring.app.domain.repository.BleConnectionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * BLE 자동 연결 및 관리 Foreground Service
 * 백그라운드에서 지속적으로 WISH RING 기기와의 연결을 관리
 */
@AndroidEntryPoint
class BleAutoConnectService : Service() {
    
    companion object {
        private const val TAG = "BleAutoConnectService"
        
        const val ACTION_START_SERVICE = "com.wishring.app.ble.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.wishring.app.ble.STOP_SERVICE"
        const val ACTION_START_SCAN = "com.wishring.app.ble.START_SCAN"
        const val ACTION_STOP_SCAN = "com.wishring.app.ble.STOP_SCAN"
        
        /**
         * 서비스 시작
         */
        fun startService(context: Context) {
            val intent = Intent(context, BleAutoConnectService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }
        
        /**
         * 서비스 중지
         */
        fun stopService(context: Context) {
            val intent = Intent(context, BleAutoConnectService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
        
        /**
         * 스캔 시작
         */
        fun startScan(context: Context) {
            val intent = Intent(context, BleAutoConnectService::class.java).apply {
                action = ACTION_START_SCAN
            }
            context.startForegroundService(intent)
        }
        
        /**
         * 스캔 중지
         */
        fun stopScan(context: Context) {
            val intent = Intent(context, BleAutoConnectService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
            context.startService(intent)
        }
    }
    
    @Inject
    lateinit var bleConnectionManager: BleConnectionManager
    
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher
    
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationManager: NotificationManager? = null
    
    // 서비스 상태 관리
    private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    
    // 연결 상태 모니터링
    private var connectionMonitorJob: Job? = null
    
    enum class ServiceState {
        STOPPED,
        SCANNING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BLE Auto Connect Service created")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 연결 상태 모니터링 시작
        startConnectionMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                startAutoConnection()
            }
            ACTION_STOP_SERVICE -> {
                stopAutoConnection()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_SCAN -> {
                if (_serviceState.value == ServiceState.STOPPED) {
                    startForegroundService()
                }
                startScanning()
            }
            ACTION_STOP_SCAN -> {
                stopScanning()
            }
        }
        
        // 시스템에 의해 서비스가 종료되면 자동으로 재시작
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "BLE Auto Connect Service destroyed")
        
        stopAutoConnection()
        connectionMonitorJob?.cancel()
        serviceScope.cancel()
        
        bleConnectionManager.destroy()
        
        super.onDestroy()
    }
    
    /**
     * Foreground Service 시작에 필요한 권한이 있는지 확인
     */
    private fun hasRequiredPermissions(): Boolean {
        // Basic FOREGROUND_SERVICE 권한 확인 (모든 API 레벨)
        val foregroundServicePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED
        
        // BLE 관련 권한 확인
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: 새로운 BLE 권한
            val bluetoothConnect = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            
            val bluetoothScan = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            bluetoothConnect && bluetoothScan
        } else {
            // Android 11 이하: 기존 BLE 권한
            val bluetooth = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
            
            val bluetoothAdmin = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
            
            bluetooth && bluetoothAdmin
        }
        
        // 알림 권한 확인 (Android 13+)
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 이하에서는 자동으로 허용
        }
        
        val hasAllPermissions = foregroundServicePermission && bluetoothPermissions && notificationPermission
        
        Log.d(TAG, "Permission check - API Level: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Foreground Service: $foregroundServicePermission")
        Log.d(TAG, "Bluetooth: $bluetoothPermissions") 
        Log.d(TAG, "Notification: $notificationPermission")
        Log.d(TAG, "All permissions: $hasAllPermissions")
        
        return hasAllPermissions
    }
    
    /**
     * Foreground Service 시작
     */
    private fun startForegroundService() {
        try {
            Log.d(TAG, "Starting foreground service...")
            
            // 알림 채널이 생성되었는지 확인
            createNotificationChannel()
            
            val hasPermissions = hasRequiredPermissions()
            Log.d(TAG, "Has required permissions: $hasPermissions")
            
            val notification = createNotification(
                title = "WISH RING 연결 중...",
                content = if (hasPermissions) {
                    "스마트 링을 찾고 있습니다"
                } else {
                    "BLE 권한이 필요합니다"
                },
                state = ServiceState.SCANNING
            )
            
            // For maximum compatibility, avoid using foregroundServiceType entirely
            // This prevents SecurityException on different Android versions
            startForeground(
                BleConstants.SERVICE_NOTIFICATION_ID, 
                notification
            )
            
            Log.d(TAG, "Foreground service started successfully")
            
            // Check permissions AFTER startForeground
            if (!hasPermissions) {
                Log.w(TAG, "Missing required permissions - service running in limited mode")
                updateNotification(
                    title = "WISH RING 권한 필요",
                    content = "앱에서 BLE 권한을 허용해주세요",
                    state = ServiceState.ERROR
                )
                _serviceState.value = ServiceState.ERROR
            } else {
                _serviceState.value = ServiceState.SCANNING
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service - missing permissions", e)
            _serviceState.value = ServiceState.ERROR
            
            // 기본 알림으로라도 서비스 시작
            try {
                val basicNotification = createBasicNotification()
                startForeground(BleConstants.SERVICE_NOTIFICATION_ID, basicNotification)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start foreground service with basic notification", ex)
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting foreground service", e)
            _serviceState.value = ServiceState.ERROR
            
            // 기본 알림으로라도 서비스 시작
            try {
                val basicNotification = createBasicNotification()
                startForeground(BleConstants.SERVICE_NOTIFICATION_ID, basicNotification)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start foreground service with basic notification", ex)
                stopSelf()
            }
        }
    }
    
    /**
     * 자동 연결 시작
     */
    private fun startAutoConnection() {
        Log.d(TAG, "Starting auto connection")
        
        serviceScope.launch {
            try {
                // 이미 연결된 기기가 있는지 확인
                if (bleConnectionManager.connectionState.value == BleConnectionState.CONNECTED) {
                    Log.d(TAG, "Already connected")
                    _serviceState.value = ServiceState.CONNECTED
                    return@launch
                }
                
                // 스마트 스캔 시작
                bleConnectionManager.startSmartScan()
                _serviceState.value = ServiceState.SCANNING
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting auto connection", e)
                _serviceState.value = ServiceState.ERROR
            }
        }
    }
    
    /**
     * 자동 연결 중지
     */
    private fun stopAutoConnection() {
        Log.d(TAG, "Stopping auto connection")
        
        try {
            bleConnectionManager.stopScanning()
            bleConnectionManager.disconnect()
            _serviceState.value = ServiceState.STOPPED
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping auto connection", e)
        }
    }
    
    /**
     * 스캔 시작
     */
    private fun startScanning() {
        serviceScope.launch {
            try {
                bleConnectionManager.startSmartScan()
                _serviceState.value = ServiceState.SCANNING
                
                updateNotification(
                    title = "WISH RING 검색 중",
                    content = "주변에서 스마트 링을 찾고 있습니다",
                    state = ServiceState.SCANNING
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting scan", e)
                _serviceState.value = ServiceState.ERROR
            }
        }
    }
    
    /**
     * 스캔 중지
     */
    private fun stopScanning() {
        try {
            bleConnectionManager.stopScanning()
            
            if (_serviceState.value == ServiceState.SCANNING) {
                _serviceState.value = ServiceState.STOPPED
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
    }
    
    /**
     * 연결 상태 모니터링 시작
     */
    private fun startConnectionMonitoring() {
        connectionMonitorJob = serviceScope.launch {
            // BLE 연결 상태 모니터링
            bleConnectionManager.connectionState.collect { connectionState ->
                handleConnectionStateChange(connectionState)
            }
        }
    }
    
    /**
     * 연결 상태 변경 처리
     */
    private suspend fun handleConnectionStateChange(connectionState: BleConnectionState) {
        Log.d(TAG, "Connection state changed: $connectionState")
        
        when (connectionState) {
            BleConnectionState.DISCONNECTED -> {
                _serviceState.value = ServiceState.SCANNING
                updateNotification(
                    title = "WISH RING 연결 끊김",
                    content = "재연결을 시도하고 있습니다",
                    state = ServiceState.RECONNECTING
                )
                
                // 자동 재연결 시작
                delay(1000) // 짧은 지연 후 재연결 시도
                bleConnectionManager.startAutoReconnect()
            }
            
            BleConnectionState.CONNECTING -> {
                _serviceState.value = ServiceState.RECONNECTING
                updateNotification(
                    title = "WISH RING 연결 중",
                    content = "스마트 링과 연결하고 있습니다",
                    state = ServiceState.RECONNECTING
                )
            }
            
            BleConnectionState.CONNECTED -> {
                _serviceState.value = ServiceState.CONNECTED
                
                val deviceName = bleConnectionManager.connectedDevice.value?.name ?: "Unknown"
                val batteryLevel = bleConnectionManager.batteryLevel.value
                
                val batteryText = batteryLevel?.let { " (배터리: $it%)" } ?: ""
                
                updateNotification(
                    title = "WISH RING 연결됨",
                    content = "$deviceName$batteryText",
                    state = ServiceState.CONNECTED
                )
            }
            
            BleConnectionState.DISCONNECTING -> {
                updateNotification(
                    title = "WISH RING 연결 해제 중",
                    content = "연결을 해제하고 있습니다",
                    state = ServiceState.SCANNING
                )
            }
            
            BleConnectionState.ERROR -> {
                _serviceState.value = ServiceState.ERROR
                updateNotification(
                    title = "WISH RING 연결 오류",
                    content = "연결에 문제가 발생했습니다",
                    state = ServiceState.ERROR
                )
            }
        }
    }
    
    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        try {
            if (notificationManager == null) {
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            
            // 이미 채널이 있는지 확인
            val existingChannel = notificationManager?.getNotificationChannel(BleConstants.NOTIFICATION_CHANNEL_ID)
            if (existingChannel != null) {
                Log.d(TAG, "Notification channel already exists")
                return
            }
            
            val channel = NotificationChannel(
                BleConstants.NOTIFICATION_CHANNEL_ID,
                BleConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WISH RING BLE 연결 상태를 표시합니다"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }
    
    /**
     * 알림 생성
     */
    private fun createNotification(
        title: String,
        content: String,
        state: ServiceState
    ): Notification {
        
        val icon = when (state) {
            ServiceState.CONNECTED -> R.drawable.ic_bluetooth_connected
            ServiceState.RECONNECTING -> R.drawable.ic_bluetooth_searching
            ServiceState.SCANNING -> R.drawable.ic_bluetooth_searching
            ServiceState.ERROR -> R.drawable.ic_bluetooth_disabled
            ServiceState.STOPPED -> R.drawable.ic_bluetooth_disabled
        }
        
        return NotificationCompat.Builder(this, BleConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon as Int)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()
    }
    
    /**
     * 기본 알림 생성 (권한 없을 때 사용)
     */
    private fun createBasicNotification(): Notification {
        return NotificationCompat.Builder(this, BleConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("WISH RING 서비스")
            .setContentText("백그라운드에서 실행 중입니다")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .build()
    }
    
    /**
     * 알림 업데이트
     */
    private fun updateNotification(
        title: String,
        content: String,
        state: ServiceState
    ) {
        try {
            val notification = createNotification(title, content, state)
            notificationManager?.notify(BleConstants.SERVICE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }
}