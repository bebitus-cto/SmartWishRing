package com.wishring.app

import android.app.Application
import android.util.Log
import com.manridy.sdk_mrd2019.Manridy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * WishRing Application Class
 * 
 * Hilt의 진입점이 되는 Application 클래스
 * 앱 전역 초기화 작업 수행
 */
@HiltAndroidApp
class WishRingApplication : Application() {
    
    // @Inject
    // lateinit var workerFactory: HiltWorkerFactory
    
    // @Inject
    // lateinit var midnightResetScheduler: MidnightResetScheduler
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize MRD SDK
        initializeMrdSdk()
        
        // Schedule midnight reset (자정 자동 리셋 예약)
        // scheduleMidnightReset()
        
        // TODO: Timber 또는 다른 로깅 라이브러리 초기화
        // TODO: Crash reporting (Firebase Crashlytics 등) 초기화
        // TODO: 앱 전역 설정 초기화
    }
    
    /**
     * WorkManager configuration for Hilt integration
     */
    // override fun getWorkManagerConfiguration(): Configuration {
    //     return Configuration.Builder()
    //         .setWorkerFactory(workerFactory)
    //         .setMinimumLoggingLevel(
    //             if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR
    //         )
    //         .build()
    // }
    
    /**
     * Initialize MRD SDK
     */
    private fun initializeMrdSdk() {
        try {
            // Initialize MRD SDK - Following demo code pattern
            Manridy.init(applicationContext)
            
            // Set up global command return listener
            // Manridy.getInstance().setCmdReturnListener(globalCmdReturnListener)
            
            Log.d("WishRingApplication", "MRD SDK initialized successfully")
            
        } catch (e: Exception) {
            Log.e("WishRingApplication", "Failed to initialize MRD SDK", e)
            // Handle SDK initialization failure
            // Could show error dialog or disable BLE features
        }
    }
    
    /**
     * Schedule midnight reset work
     * 자정마다 자동으로 새 날짜 레코드 생성 (COM-01, COM-02)
     */
    // private fun scheduleMidnightReset() {
    //     try {
    //         midnightResetScheduler.scheduleMidnightReset()
    //         Log.d("WishRingApplication", "Midnight reset scheduled successfully")
    //     } catch (e: Exception) {
    //         Log.e("WishRingApplication", "Failed to schedule midnight reset", e)
    //     }
    // }
}