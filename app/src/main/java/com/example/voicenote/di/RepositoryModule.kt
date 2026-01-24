package com.example.voicenote.di

import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.repository.VoiceNoteRepositoryImpl
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
        apiService: ApiService
    ): VoiceNoteRepository {
        return VoiceNoteRepositoryImpl(apiService)
    }
}
