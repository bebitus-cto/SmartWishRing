package com.wishring.app.di

import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.data.repository.PreferencesRepositoryImpl
import com.wishring.app.data.repository.WishRepository
import com.wishring.app.data.repository.WishRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository implementations
 * Binds repository interfaces to their concrete implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds WishCountRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindWishCountRepository(
        wishCountRepositoryImpl: WishRepositoryImpl
    ): WishRepository
    
    /**
     * Binds PreferencesRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
    
}