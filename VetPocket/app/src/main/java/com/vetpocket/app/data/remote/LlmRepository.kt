package com.vetpocket.app.data.remote

import com.vetpocket.app.BuildConfig
import com.vetpocket.app.data.db.PetProfileEntity
import com.vetpocket.app.util.formatAge
import com.vetpocket.app.util.formatWeight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

class LlmException(message: String) : Exception(message)

/**
 * Формирует System Prompt динамически из данных Room (профиль питомца) —
 * благодаря этому нейросеть даёт не абстрактную, а персональную оценку корма.
 */
object PromptBuilder {

    fun petContext(pet: PetProfileEntity, weightKg: Float?): String {
        val age = formatAge(LocalDate.ofEpochDay(pet.birthDateEpochDay))
        return buildString {
            append("Имя: ${pet.name}, $age")
            if (pet.breed.isNotBlank()) append(", порода: ${pet.breed}")
            if (weightKg != null) append(", вес: ${formatWeight(weightKg)} кг")
            append(if (pet.isNeutered) ", кастрирован/стерилизован" else ", не кастрирован")
            append(". Хронические заболевания: ")
            append(pet.chronicConditions.ifBlank { "не указаны" })
        }
    }

    fun system(pet: PetProfileEntity, weightKg: Float?): String = """
        Ты профессиональный ветеринар-диетолог. Проанализируй состав корма для кота.

        Контекст питомца: ${petContext(pet, weightKg)}

        Задача: оцени ингредиенты, выдели плюсы и минусы и дай итоговый вердикт о применимости корма именно для этого кота.
        Обязательно учитывай возраст, статус кастрации и хронические заболевания. Например, при склонности к МКБ обращай внимание
        на магний, фосфор, золу, содержание влаги и источники минералов; при кастрации — на калорийность и долю злаков.
        Текст состава распознан с фото и может содержать ошибки распознавания — трактуй его разумно и не выдумывай ингредиенты.
        Отвечай на русском языке, кратко и по делу.

        Верни ТОЛЬКО JSON без пояснений и без markdown-обёрток в таком формате:
        {"verdict":"GREEN|YELLOW|RED","summary":"2-4 предложения с итогом именно для этого питомца","pros":["плюс 1","плюс 2"],"cons":["минус 1","минус 2"]}
        Где GREEN — отлично подходит, YELLOW — подходит с осторожностью, RED — не рекомендуется.
    """.trimIndent()

    fun user(composition: String): String =
        "Состав корма (распознан с упаковки):\n${composition.trim()}"
}

@Serializable
private data class RawAnalysis(
    val verdict: String = "",
    val summary: String = "",
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
)

object FoodAnalysisParser {
    fun parse(raw: String): FoodAnalysis {
        val cleaned = raw.trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) {
            try {
                val parsed = LlmClient.json.decodeFromString<RawAnalysis>(cleaned.substring(start, end + 1))
                return FoodAnalysis(
                    verdict = mapVerdict(parsed.verdict, parsed.summary),
                    summary = parsed.summary.ifBlank { "Нейросеть не вернула текстовое резюме." },
                    pros = parsed.pros.filter { it.isNotBlank() },
                    cons = parsed.cons.filter { it.isNotBlank() },
                )
            } catch (_: SerializationException) {
                // падаем в запасной вариант ниже
            } catch (_: IllegalArgumentException) {
            }
        }
        // Запасной вариант: модель ответила обычным текстом.
        return FoodAnalysis(
            verdict = mapVerdict("", cleaned),
            summary = cleaned.ifBlank { "Пустой ответ нейросети." },
            pros = emptyList(),
            cons = emptyList(),
        )
    }

    private fun mapVerdict(verdict: String, fallbackText: String): Verdict {
        val v = verdict.uppercase()
        return when {
            "RED" in v || "BAD" in v || "НЕ РЕКОМ" in v || "🔴" in v -> Verdict.BAD
            "YELLOW" in v || "CAUTION" in v || "ОСТОРОЖ" in v || "🟡" in v -> Verdict.CAUTION
            "GREEN" in v || "GREAT" in v || "ОТЛИЧ" in v || "🟢" in v -> Verdict.GREAT
            else -> {
                val t = fallbackText.uppercase()
                when {
                    "НЕ РЕКОМЕНД" in t -> Verdict.BAD
                    "ОСТОРОЖ" in t -> Verdict.CAUTION
                    else -> Verdict.CAUTION
                }
            }
        }
    }
}

class LlmRepository(private val api: LlmApi = LlmClient.api) {

    suspend fun analyzeFood(
        pet: PetProfileEntity,
        weightKg: Float?,
        composition: String,
    ): Result<FoodAnalysis> = withContext(Dispatchers.IO) {
        try {
            val key = BuildConfig.LLM_API_KEY
            if (key.isBlank()) {
                throw LlmException(
                    "Не задан ключ нейросети. Добавьте LLM_API_KEY в local.properties и пересоберите приложение."
                )
            }
            val system = PromptBuilder.system(pet, weightKg)
            val user = PromptBuilder.user(composition)
            val raw = when (BuildConfig.LLM_PROVIDER.uppercase()) {
                "YANDEX_GPT", "YANDEX" -> callYandex(key, system, user)
                else -> callGroq(key, system, user)
            }
            Result.success(FoodAnalysisParser.parse(raw))
        } catch (e: CancellationException) {
            throw e
        } catch (e: LlmException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val msg = when (e.code()) {
                400 -> "Сервис нейросети отклонил запрос (400). Проверьте LLM_MODEL и LLM_FOLDER_ID."
                401, 403 -> "Нейросеть не приняла ключ (${e.code()}). Проверьте LLM_API_KEY и права доступа."
                429 -> "Слишком много запросов к нейросети. Попробуйте через минуту."
                else -> "Ошибка сервиса нейросети: HTTP ${e.code()}."
            }
            Result.failure(LlmException(msg))
        } catch (e: IOException) {
            Result.failure(LlmException("Нет связи с сервисом нейросети. Проверьте интернет и повторите."))
        } catch (e: SerializationException) {
            Result.failure(LlmException("Не удалось разобрать ответ нейросети."))
        } catch (e: Exception) {
            Result.failure(LlmException("Что-то пошло не так: ${e.message ?: e::class.java.simpleName}"))
        }
    }

    private suspend fun callGroq(key: String, system: String, user: String): String {
        val model = BuildConfig.LLM_MODEL.ifBlank { "openai/gpt-oss-120b" }
        val response = api.groq(
            authorization = "Bearer $key",
            body = ChatRequest(
                model = model,
                messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
                responseFormat = ResponseFormat("json_object"),
            ),
        )
        return response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Нейросеть вернула пустой ответ.")
    }

    private suspend fun callYandex(key: String, system: String, user: String): String {
        val folder = BuildConfig.LLM_FOLDER_ID
        if (folder.isBlank()) {
            throw LlmException("Для YandexGPT нужен LLM_FOLDER_ID (идентификатор каталога) в local.properties.")
        }
        val model = BuildConfig.LLM_MODEL.ifBlank { "yandexgpt/latest" }
        val response = api.yandex(
            authorization = "Api-Key $key",
            folderId = folder,
            body = YandexRequest(
                modelUri = "gpt://$folder/$model",
                messages = listOf(YandexMessage("system", system), YandexMessage("user", user)),
            ),
        )
        return response.result?.alternatives?.firstOrNull()?.message?.text
            ?: throw LlmException("Нейросеть вернула пустой ответ.")
    }
}
