package com.voicenotesai.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicenotesai.data.remote.api.AnthropicService
import com.voicenotesai.data.remote.api.GoogleAIService
import com.voicenotesai.data.remote.api.OpenAIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAIRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnthropicRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleAIRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L
    private const val OPENAI_BASE_URL = "https://api.openai.com/"
    private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/"
    private const val GOOGLE_AI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        // Only add logging interceptor in debug builds
        // Note: In production, ensure this is disabled by not including the logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Use NONE level for production, BODY for debug
            // You can control this via build variants
            level = HttpLoggingInterceptor.Level.BODY  // Enable for debugging API issues
        }
        builder.addInterceptor(loggingInterceptor)
        
        return builder.build()
    }

    @Provides
    @Singleton
    @OpenAIRetrofit
    fun provideOpenAIRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @AnthropicRetrofit
    fun provideAnthropicRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ANTHROPIC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @GoogleAIRetrofit
    fun provideGoogleAIRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GOOGLE_AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIService(@OpenAIRetrofit retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnthropicService(@AnthropicRetrofit retrofit: Retrofit): AnthropicService {
        return retrofit.create(AnthropicService::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleAIService(@GoogleAIRetrofit retrofit: Retrofit): GoogleAIService {
        return retrofit.create(GoogleAIService::class.java)
    }
}
