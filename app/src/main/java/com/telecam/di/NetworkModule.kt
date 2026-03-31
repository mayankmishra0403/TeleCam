package com.telecam.di

import android.util.Log
import com.telecam.data.remote.TelegramApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.telegram.org/"
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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApiService(retrofit: Retrofit): TelegramApiService {
        return retrofit.create(TelegramApiService::class.java)
    }
}
