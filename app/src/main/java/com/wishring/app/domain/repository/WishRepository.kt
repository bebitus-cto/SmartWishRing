package com.wishring.app.domain.repository

import com.wishring.app.data.ble.model.BleConnectionState
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.model.WishCount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for wish data management
 * 
 * 위시 데이터 관리를 위한 Repository 인터페이스 (Domain Layer)
 */
interface WishRepository {
    
    // 오늘 데이터
    fun getTodayCount(): Flow<Int>
    fun getTodayRecord(): Flow<WishCount?>
    
    // 위시 관리
    suspend fun saveWish(text: String, target: Int)
    suspend fun incrementCount()
    suspend fun updateCount(date: LocalDate, count: Int)
    
    // 기록 조회
    suspend fun getRecordByDate(date: LocalDate): DailyRecord?
    fun getRecentRecords(days: Int = 30): Flow<List<DailyRecord>>
    suspend fun getRecordsBetween(startDate: LocalDate, endDate: LocalDate): List<DailyRecord>
    
    // 리셋 로그
    suspend fun addResetLog(date: LocalDate, countBeforeReset: Int)
    suspend fun getResetLogsForDate(date: LocalDate): List<ResetLog>
    
    // BLE 상태
    fun getBleConnectionState(): Flow<BleConnectionState>
    fun getBatteryLevel(): Flow<Int>
    
    // BLE 제어
    suspend fun startBleScan()
    suspend fun connectToDevice()
    suspend fun disconnectFromDevice()
    
    // 데이터 관리
    suspend fun cleanupOldData(olderThanDays: Int = 90)
}