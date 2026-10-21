package com.vetpocket.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- OpenAI-совместимый формат (Groq) ----------

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int = 1500,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
)

@Serializable
data class ChatResponse(val choices: List<Choice> = emptyList()) {
    @Serializable
    data class Choice(val message: ChatMessage? = null)
}

// ---------- YandexGPT ----------

@Serializable
data class YandexMessage(val role: String, val text: String)

@Serializable
data class YandexCompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.2,
    val maxTokens: String = "1500",
)

@Serializable
data class YandexRequest(
    val modelUri: String,
    val completionOptions: YandexCompletionOptions = YandexCompletionOptions(),
    val messages: List<YandexMessage>,
)

@Serializable
data class YandexResponse(val result: Result? = null) {
    @Serializable
    data class Result(val alternatives: List<Alternative> = emptyList())

    @Serializable
    data class Alternative(val message: YandexMessage? = null)
}

// ---------- Доменная модель ответа ----------

enum class Verdict { GREAT, CAUTION, BAD }

data class FoodAnalysis(
    val verdict: Verdict,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
)
