package com.example.voicenote.di

import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.remote.HmacInterceptor
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.voicenote.ai/api/v1/"
    private const val HMAC_SECRET = "REPLACE_WITH_SECURE_VAULT_KEY"

    @Provides
    @Singleton
    fun provideSecurityManager(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): SecurityManager {
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
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(HmacInterceptor(HMAC_SECRET))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                securityManager.getSessionToken()?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(request.build())
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(
        client: OkHttpClient,
        securityManager: SecurityManager,
        gson: com.google.gson.Gson
    ): com.example.voicenote.core.network.WebSocketManager {
        return com.example.voicenote.core.network.WebSocketManager(client, securityManager, gson)
    }
}
