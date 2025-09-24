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

    override fun onCreate() {
        super.onCreate()

        initializeMrdSdk()
    }

    private fun initializeMrdSdk() {
        try {
            Manridy.init(applicationContext)
            Log.d("WishRingApplication", "MRD SDK initialized successfully")
        } catch (e: Exception) {
            Log.e("WishRingApplication", "Failed to initialize MRD SDK", e)
        }
    }
}