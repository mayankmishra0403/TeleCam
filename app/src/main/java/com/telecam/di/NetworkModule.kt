package com.telecam.di

import android.util.Log
import com.telecam.BuildConfig
import com.telecam.data.remote.TelegramApiService
import com.telecam.data.remote.TelegramAuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TELEGRAM_BASE_URL = "https://api.telegram.org/"
    private const val TIMEOUT_SECONDS = 60L
    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val urlLoggingInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "HTTP ${request.method} ${request.url}")
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(urlLoggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("telegramRetrofit")
    fun provideTelegramRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TELEGRAM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("authRetrofit")
    fun provideAuthRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val rawBaseUrl = BuildConfig.TELEGRAM_AUTH_BASE_URL
        val normalizedBaseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApiService(@Named("telegramRetrofit") retrofit: Retrofit): TelegramApiService {
        return retrofit.create(TelegramApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTelegramAuthApiService(@Named("authRetrofit") retrofit: Retrofit): TelegramAuthApiService {
        return retrofit.create(TelegramAuthApiService::class.java)
    }
}
