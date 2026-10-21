package com.vetpocket.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.PetRepository
import com.vetpocket.app.data.db.PetProfileEntity
import com.vetpocket.app.data.remote.FoodAnalysis
import com.vetpocket.app.data.remote.LlmRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

enum class ScanStage { Camera, Recognizing, Review, Analyzing, Result }

data class ScannerState(
    val stage: ScanStage = ScanStage.Camera,
    val text: String = "",
    val analysis: FoodAnalysis? = null,
    val error: String? = null,
)

class ScannerViewModel(
    private val repo: PetRepository,
    private val llm: LlmRepository,
    private val context: Context // <-- 1. Добавили Context в конструктор
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    val pet: StateFlow<PetProfileEntity?> = repo.observePet()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Распознавание полностью локальное: сеть не нужна, изображение никуда не отправляется.
    // 2. Создаем Tesseract. Инициализация будет внутри onPhotoCaptured, чтобы не вешать главный поток.
    private var tessBaseAPI: TessBaseAPI? = null

    fun onPhotoCaptured(bitmap: Bitmap) {
        _state.update { it.copy(stage = ScanStage.Recognizing, error = null) }
        viewModelScope.launch(Dispatchers.Default) { // Работаем в фоновом потоке
            try {
                // Ленивая инициализация Tesseract (только при первом снимке)
                if (tessBaseAPI == null) {
                    TessDataHelper.ensureTessData(context) // Копируем файлы из assets
                    val dataPath = File(context.filesDir, "tesseract").absolutePath

                    tessBaseAPI = TessBaseAPI().apply {
                        // init возвращает true, если всё ок
                        val success = init(dataPath, "rus+eng")
                        if (!success) {
                            throw IllegalStateException("Не удалось загрузить языковые модели Tesseract")
                        }
                    }
                }

                // Распознаем текст
                val tess = tessBaseAPI ?: throw IllegalStateException("Tesseract не инициализирован")
                tess.setImage(bitmap)
                val recognizedText = tess.utF8Text ?: "" // Получаем текст

                val cleaned = recognizedText
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                    .take(MAX_TEXT_LENGTH)

                if (cleaned.isBlank()) {
                    _state.update {
                        it.copy(
                            stage = ScanStage.Camera,
                            error = "Текст не найден. Наведите рамку на состав и попробуйте ещё раз при хорошем освещении.",
                        )
                    }
                } else {
                    _state.update { it.copy(stage = ScanStage.Review, text = cleaned) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(stage = ScanStage.Camera, error = "Не удалось распознать текст: ${e.message ?: "неизвестная ошибка"}")
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    fun onCameraError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun onTextEdited(text: String) {
        _state.update { it.copy(text = text.take(MAX_TEXT_LENGTH)) }
    }

    fun analyze() {
        val composition = _state.value.text.trim()
        if (composition.isEmpty()) return
        _state.update { it.copy(stage = ScanStage.Analyzing, error = null) }
        viewModelScope.launch {
            val pet = repo.getPet()
            if (pet == null) {
                _state.update {
                    it.copy(stage = ScanStage.Review, error = "Сначала создайте профиль питомца на главном экране — без него оценка не будет персональной.")
                }
                return@launch
            }
            val result = llm.analyzeFood(pet, repo.latestWeight(), composition)
            result.onSuccess { analysis ->
                _state.update { it.copy(stage = ScanStage.Result, analysis = analysis) }
            }.onFailure { e ->
                _state.update { it.copy(stage = ScanStage.Review, error = e.message ?: "Не удалось получить ответ нейросети.") }
            }
        }
    }

    fun backToCamera() {
        _state.value = ScannerState()
    }

    fun backToReview() {
        _state.update { it.copy(stage = ScanStage.Review, analysis = null, error = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    // 3. Освобождаем память Tesseract, когда ViewModel уничтожается
    override fun onCleared() {
        tessBaseAPI?.recycle() // ОБЯЗАТЕЛЬНО: освобождает нативную память
        tessBaseAPI = null
        super.onCleared()
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 4000

        // 4. Изменили фабрику, чтобы она принимала Context
        fun factory(container: AppContainer, context: Context) = viewModelFactory {
            initializer {
                ScannerViewModel(
                    repo = container.repository,
                    llm = container.llm,
                    context = context.applicationContext // Берем именно applicationContext
                )
            }
        }
    }
}
