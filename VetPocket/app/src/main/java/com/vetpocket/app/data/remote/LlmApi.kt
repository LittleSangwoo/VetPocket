package com.vetpocket.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface LlmApi {
    @POST("https://api.groq.com/openai/v1/chat/completions")
    suspend fun groq(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest,
    ): ChatResponse

    @POST("https://llm.api.cloud.yandex.net/foundationModels/v1/completion")
    suspend fun yandex(
        @Header("Authorization") authorization: String,
        @Header("x-folder-id") folderId: String,
        @Body body: YandexRequest,
    ): YandexResponse
}

object LlmClient {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    val api: LlmApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            // Базовый адрес формальный: полные URL заданы в аннотациях @POST.
            .baseUrl("https://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LlmApi::class.java)
    }
}
