package com.wishring.app.ble

import java.util.*

/**
 * BLE 통신 관련 상수 정의
 * WISH RING 기기와의 블루투스 연결에 사용되는 모든 상수를 통합 관리
 */
object BleConstants {
    
    // ===== Service & Characteristic UUIDs =====
    
    /** Main Service UUID - PRD 문서 기준 */
    const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    
    /** Counter Characteristic UUID - 카운터 데이터 송수신 */
    const val COUNTER_CHAR_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
    
    /** Battery Characteristic UUID - 배터리 정보 */
    const val BATTERY_CHAR_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
    
    /** Reset Characteristic UUID - 리셋 신호 */
    const val RESET_CHAR_UUID = "0000fff3-0000-1000-8000-00805f9b34fb"
    
    /** Client Characteristic Configuration Descriptor UUID */
    const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    
    // ===== Device Identification =====
    
    /** WISH RING 기기명 접두사 */
    const val DEVICE_NAME_PREFIX = "WISH_RING"
    
    /** 대체 기기명 접두사 (호환성) */
    const val DEVICE_NAME_PREFIX_ALT = "WishRing"
    
    /** MRD 기기명 접두사 (SDK 기준) */
    const val MRD_DEVICE_PREFIX = "MRD"
    
    // ===== Timeout Settings =====
    
    /** BLE 스캔 타임아웃 (밀리초) */
    const val SCAN_TIMEOUT_MS = 10_000L
    
    /** BLE 연결 타임아웃 (밀리초) */
    const val CONNECTION_TIMEOUT_MS = 15_000L
    
    /** Service Discovery 타임아웃 (밀리초) */
    const val DISCOVERY_TIMEOUT_MS = 5_000L
    
    /** SDK 검증 타임아웃 (밀리초) */
    const val SDK_VALIDATION_TIMEOUT_MS = 3_000L
    
    // ===== Reconnection Strategy =====
    
    /** 초기 재연결 지연 시간 (밀리초) */
    const val INITIAL_RECONNECT_DELAY_MS = 3_000L
    
    /** 최대 재연결 지연 시간 (밀리초) */
    const val MAX_RECONNECT_DELAY_MS = 60_000L
    
    /** 최대 재연결 시도 횟수 */
    const val MAX_RECONNECT_ATTEMPTS = 5
    
    /** Exponential Backoff 배수 */
    const val RECONNECT_BACKOFF_MULTIPLIER = 2.0
    
    // ===== Scan Strategy =====
    
    /** 집중 스캔 주기 - 연결 끊김 직후 (밀리초) */
    const val INTENSIVE_SCAN_INTERVAL_MS = 5_000L
    
    /** 일반 스캔 주기 - 장시간 연결 실패 시 (밀리초) */
    const val NORMAL_SCAN_INTERVAL_MS = 30_000L
    
    /** 집중 스캔 지속 시간 (밀리초) */
    const val INTENSIVE_SCAN_DURATION_MS = 60_000L
    
    // ===== Battery Monitoring =====
    
    /** 배터리 모니터링 주기 (밀리초) */
    const val BATTERY_MONITOR_INTERVAL_MS = 30_000L
    
    /** 저전력 경고 임계값 (퍼센트) */
    const val LOW_BATTERY_THRESHOLD = 15
    
    /** 배터리 위험 임계값 (퍼센트) */
    const val CRITICAL_BATTERY_THRESHOLD = 5
    
    // ===== Health Check =====
    
    /** 연결 상태 확인 주기 (밀리초) */
    const val HEALTH_CHECK_INTERVAL_MS = 30_000L
    
    /** 연결 상태 확인 타임아웃 (밀리초) */
    const val HEALTH_CHECK_TIMEOUT_MS = 5_000L
    
    // ===== Data Parsing =====
    
    /** 카운터 데이터 크기 (바이트) */
    const val COUNTER_DATA_SIZE = 4
    
    /** 배터리 데이터 크기 (바이트) */
    const val BATTERY_DATA_SIZE = 2
    
    /** 최대 카운트 값 */
    const val MAX_COUNT_VALUE = 99_999
    
    // ===== Foreground Service =====
    
    /** BLE 서비스 알림 채널 ID */
    const val NOTIFICATION_CHANNEL_ID = "wish_ring_ble_service"
    
    /** BLE 서비스 알림 채널 이름 */
    const val NOTIFICATION_CHANNEL_NAME = "WISH RING BLE Connection"
    
    /** BLE 서비스 알림 ID */
    const val SERVICE_NOTIFICATION_ID = 1001
    
    /** 배터리 경고 알림 ID */
    const val BATTERY_WARNING_NOTIFICATION_ID = 1002
    
    // ===== Error Codes =====
    
    /** 블루투스 비활성화 에러 */
    const val ERROR_BLUETOOTH_DISABLED = "ERROR_BLUETOOTH_DISABLED"
    
    /** 권한 부족 에러 */
    const val ERROR_PERMISSION_DENIED = "ERROR_PERMISSION_DENIED"
    
    /** 기기 없음 에러 */
    const val ERROR_DEVICE_NOT_FOUND = "ERROR_DEVICE_NOT_FOUND"
    
    /** 연결 실패 에러 */
    const val ERROR_CONNECTION_FAILED = "ERROR_CONNECTION_FAILED"
    
    /** SDK 검증 실패 에러 */
    const val ERROR_SDK_VALIDATION_FAILED = "ERROR_SDK_VALIDATION_FAILED"
    
    /** 서비스 Discovery 실패 에러 */
    const val ERROR_SERVICE_DISCOVERY_FAILED = "ERROR_SERVICE_DISCOVERY_FAILED"
    
    // ===== UUID Objects =====
    
    /** Service UUID 객체 */
    val SERVICE_UUID_OBJ: UUID = UUID.fromString(SERVICE_UUID)
    
    /** Counter Characteristic UUID 객체 */
    val COUNTER_CHAR_UUID_OBJ: UUID = UUID.fromString(COUNTER_CHAR_UUID)
    
    /** Battery Characteristic UUID 객체 */
    val BATTERY_CHAR_UUID_OBJ: UUID = UUID.fromString(BATTERY_CHAR_UUID)
    
    /** Reset Characteristic UUID 객체 */
    val RESET_CHAR_UUID_OBJ: UUID = UUID.fromString(RESET_CHAR_UUID)
    
    /** CCCD UUID 객체 */
    val CCCD_UUID_OBJ: UUID = UUID.fromString(CCCD_UUID)
    
    // ===== Utility Functions =====
    
    /**
     * 기기명이 WISH RING 기기인지 확인
     * @param deviceName 검사할 기기명
     * @return WISH RING 기기 여부
     */
    fun isWishRingDeviceName(deviceName: String?): Boolean {
        return deviceName?.let { name ->
            name.startsWith(DEVICE_NAME_PREFIX, ignoreCase = true) ||
            name.startsWith(DEVICE_NAME_PREFIX_ALT, ignoreCase = true) ||
            name.startsWith(MRD_DEVICE_PREFIX, ignoreCase = true)
        } ?: false
    }
    
    /**
     * 배터리 레벨이 저전력인지 확인
     * @param batteryLevel 배터리 레벨 (0-100)
     * @return 저전력 여부
     */
    fun isLowBattery(batteryLevel: Int): Boolean {
        return batteryLevel <= LOW_BATTERY_THRESHOLD
    }
    
    /**
     * 배터리 레벨이 위험 수준인지 확인
     * @param batteryLevel 배터리 레벨 (0-100)
     * @return 위험 수준 여부
     */
    fun isCriticalBattery(batteryLevel: Int): Boolean {
        return batteryLevel <= CRITICAL_BATTERY_THRESHOLD
    }
    
    /**
     * 재연결 지연 시간 계산 (Exponential Backoff)
     * @param attemptCount 재연결 시도 횟수 (0부터 시작)
     * @return 지연 시간 (밀리초)
     */
    fun calculateReconnectDelay(attemptCount: Int): Long {
        if (attemptCount <= 0) return INITIAL_RECONNECT_DELAY_MS
        
        val delay = (INITIAL_RECONNECT_DELAY_MS * 
                    Math.pow(RECONNECT_BACKOFF_MULTIPLIER, attemptCount.toDouble())).toLong()
        
        return delay.coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }
}