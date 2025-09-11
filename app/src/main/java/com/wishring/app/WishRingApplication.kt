package com.wishring.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * WishRing Application Class
 * 
 * Hilt의 진입점이 되는 Application 클래스
 * 앱 전역 초기화 작업 수행
 */
@HiltAndroidApp
class WishRingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize MRD SDK
        initializeMrdSdk()
        
        // TODO: Timber 또는 다른 로깅 라이브러리 초기화
        // TODO: Crash reporting (Firebase Crashlytics 등) 초기화
        // TODO: 앱 전역 설정 초기화
    }
    
    /**
     * Initialize MRD SDK
     */
    private fun initializeMrdSdk() {
        try {
            // TODO: Replace with actual MRD SDK initialization when available
            // Expected pattern:
            // Manridy.getInstance().init(this)
            // Manridy.getInstance().setCmdReturnListener(globalCmdReturnListener)
            
            // Placeholder initialization
            Log.d("WishRingApplication", "MRD SDK initialized successfully (placeholder)")
            
        } catch (e: Exception) {
            Log.e("WishRingApplication", "Failed to initialize MRD SDK", e)
            // Handle SDK initialization failure
            // Could show error dialog or disable BLE features
        }
    }
}