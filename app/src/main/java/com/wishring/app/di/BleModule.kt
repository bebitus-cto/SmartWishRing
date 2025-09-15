package com.wishring.app.di

import android.content.Context
import com.wishring.app.ble.MrdProtocolAdapter
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
 * Hilt module for BLE-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {
    
    /**
     * Bind BLE Repository implementation
     */
    @Binds
    @Singleton
    abstract fun bindBleRepository(
        bleRepositoryImpl: BleRepositoryImpl
    ): BleRepository
    
    companion object {
        
        /**
         * Provide MRD Protocol Adapter
         */
        @Provides
        @Singleton
        fun provideMrdProtocolAdapter(
            @ApplicationContext context: Context
        ): MrdProtocolAdapter {
            return MrdProtocolAdapter(context)
        }
    }
}