package com.vetpocket.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Профиль питомца. Эти данные подставляются в System Prompt при обращении к LLM. */
@Entity(tableName = "pet_profile")
data class PetProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val birthDateEpochDay: Long,
    /** Текущий статус: кастрация / стерилизация. */
    val isNeutered: Boolean,
    val breed: String = "",
    /** Например: «склонность к МКБ». */
    val chronicConditions: String = "",
    val photoPath: String? = null,
)

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val weightValue: Float,
)

enum class EventType(val title: String) {
    VACCINATION("Вакцинация"),
    TREATMENT("Обработка"),
}

enum class ProcedureType(
    val title: String,
    val eventType: EventType,
    val defaultIntervalMonths: Int,
) {
    COMPLEX_VACCINE("Комплексная вакцина", EventType.VACCINATION, 12),
    RABIES("Бешенство", EventType.VACCINATION, 12),
    ANTI_TICK("От клещей", EventType.TREATMENT, 3),
    ANTI_WORM("От гельминтов", EventType.TREATMENT, 3),
}

@Entity(tableName = "medical_events")
data class MedicalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EventType,
    val procedure: ProcedureType,
    val drugName: String,
    val dateEpochDay: Long,
    /** Если true — дата следующей процедуры передаётся в AlarmManager. */
    val isScheduledReminder: Boolean,
    val nextDateEpochDay: Long? = null,
)

@Entity(tableName = "diary_notes")
data class DiaryNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val symptomText: String,
    /** Теги состояния через «|», например «Вялый|Не ест». */
    val tags: String = "",
)
