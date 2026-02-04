package com.example.voicenote.di

import com.example.voicenote.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSecurityManager(): com.example.voicenote.core.security.SecurityManager = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideWebSocketManager(): com.example.voicenote.core.network.WebSocketManager = mockk(relaxed = true)
}
