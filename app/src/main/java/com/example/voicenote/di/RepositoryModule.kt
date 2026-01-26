package com.example.voicenote.di

import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.repository.VoiceNoteRepositoryImpl
import com.example.voicenote.core.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideVoiceNoteRepository(
        apiService: ApiService,
        webSocketManager: WebSocketManager
    ): VoiceNoteRepository {
        return VoiceNoteRepositoryImpl(apiService, webSocketManager)
    }
}
