package com.example.voicenote.di

import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.remote.HmacInterceptor
import com.example.voicenote.core.network.ConnectivityObserver
import android.content.Context
import com.example.voicenote.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

import com.example.voicenote.core.config.Config

// Placeholder for location
object CurrentLocation {
    private var gpsCoords: String = ""
    fun get(): String = gpsCoords
    fun set(coords: String) {
        gpsCoords = coords
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val HMAC_SECRET = "VN_SECURE_8f7d9a2b_2026"

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(securityManager: SecurityManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(HmacInterceptor(HMAC_SECRET))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                securityManager.getSessionToken()?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                
                val coords = CurrentLocation.get()
                if (coords.isNotEmpty()) {
                    request.addHeader("X-GPS-Coords", coords)
                }

                chain.proceed(request.build())
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(
        client: OkHttpClient,
        securityManager: SecurityManager,
        gson: Gson
    ): com.example.voicenote.core.network.WebSocketManager {
        return com.example.voicenote.core.network.WebSocketManager(client, securityManager, gson)
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserver(context)
    }
}
