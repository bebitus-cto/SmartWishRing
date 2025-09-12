package com.wishring.app.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
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
     * Foreground Service 시작
     */
    private fun startForegroundService() {
        val notification = createNotification(
            title = "WISH RING 연결 중...",
            content = "스마트 링을 찾고 있습니다",
            state = ServiceState.SCANNING
        )
        
        startForeground(BleConstants.SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        _serviceState.value = ServiceState.SCANNING
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
        val channel = NotificationChannel(
            BleConstants.NOTIFICATION_CHANNEL_ID,
            BleConstants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "WISH RING BLE 연결 상태를 표시합니다"
            setShowBadge(false)
            setSound(null, null)
        }
        
        notificationManager?.createNotificationChannel(channel)
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
     * 알림 업데이트
     */
    private fun updateNotification(
        title: String,
        content: String,
        state: ServiceState
    ) {
        val notification = createNotification(title, content, state)
        notificationManager?.notify(BleConstants.SERVICE_NOTIFICATION_ID, notification)
    }
}