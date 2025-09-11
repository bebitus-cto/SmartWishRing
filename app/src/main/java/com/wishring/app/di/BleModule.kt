package com.wishring.app.di

import android.content.Context
import com.wishring.app.ble.BleRepositoryImpl
import com.wishring.app.domain.repository.BleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for BLE dependencies
 * Provides BLE repository implementation and MRD SDK integration
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {
    
    companion object {
        
        /**
         * Provides MRD SDK Manridy instance
         * Note: Actual package and class names need to be verified with real SDK
         */
        @Provides
        @Singleton
        fun provideManridySDK(@ApplicationContext context: Context): Any {
            // TODO: Replace with actual Manridy SDK initialization
            // Expected pattern: Manridy.getInstance().init(context)
            // return Manridy.getInstance()
            
            // Placeholder for now - will be replaced with actual SDK
            return object {
                fun init() = Unit
                fun getMrdSend() = this
                fun getSystem(type: Any, vararg params: Any) = ByteArray(0)
                fun setTime() = ByteArray(0)
                fun setUserInfo(info: Any) = ByteArray(0)
                fun getHeartRate(type: Any) = ByteArray(0)
                fun getStepData(type: Any) = ByteArray(0)
                fun setSportTarget(steps: Int) = ByteArray(0)
                fun findDevice(enable: Boolean) = ByteArray(0)
                fun sendAppPush(push: Any) = ByteArray(0)
                fun testHeartRateEcg(action: Any) = ByteArray(0)
            }
        }
        
        /**
         * Provides MRD SDK callback listener
         * Note: Actual interface names need to be verified with real SDK
         */
        @Provides
        @Singleton
        fun provideMrdCallback(): Any {
            // TODO: Replace with actual callback interface
            // Expected pattern: CmdReturnListener implementation
            return object {
                fun onCmdReturn(readType: Any, data: Any) = Unit
                fun onError(errorCode: Int, message: String) = Unit
                fun onDeviceConnected() = Unit
                fun onDeviceDisconnected() = Unit
            }
        }
    }
    
    /**
     * Binds BleRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindBleRepository(
        bleRepositoryImpl: BleRepositoryImpl
    ): BleRepository
}