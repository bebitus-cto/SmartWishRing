package com.wishring.app.di

import android.content.Context
import androidx.room.Room
import com.wishring.app.data.local.database.WishRingDatabase
import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.dao.WishCountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Main application module
 * Provides common dependencies across the app
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides application context
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    /**
     * Provides IO dispatcher for background operations
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    /**
     * Provides Main dispatcher for UI operations
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    /**
     * Provides Default dispatcher for CPU intensive work
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    /**
     * Provides Room database instance
     */
    @Provides
    @Singleton
    fun provideWishRingDatabase(@ApplicationContext context: Context): WishRingDatabase {
        return WishRingDatabase.getInstance(context)
    }
    
    /**
     * Provides WishCountDao
     */
    @Provides
    fun provideWishCountDao(database: WishRingDatabase): WishCountDao {
        return database.wishCountDao()
    }
    
    /**
     * Provides ResetLogDao  
     */
    @Provides
    fun provideResetLogDao(database: WishRingDatabase): ResetLogDao {
        return database.resetLogDao()
    }
}

/**
 * Qualifier for IO dispatcher
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier for Main dispatcher
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Qualifier for Default dispatcher
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher