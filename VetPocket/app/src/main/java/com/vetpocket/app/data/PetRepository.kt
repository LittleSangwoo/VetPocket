package com.vetpocket.app.data

import android.content.Context
import android.net.Uri
import com.vetpocket.app.data.db.AppDatabase
import com.vetpocket.app.data.db.DiaryNoteEntity
import com.vetpocket.app.data.db.MedicalEventEntity
import com.vetpocket.app.data.db.PetProfileEntity
import com.vetpocket.app.data.db.ProcedureType
import com.vetpocket.app.data.db.WeightRecordEntity
import com.vetpocket.app.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class PetRepository(
    private val context: Context,
    db: AppDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val petDao = db.petDao()
    private val weightDao = db.weightDao()
    private val medicalDao = db.medicalDao()
    private val diaryDao = db.diaryDao()

    fun observePet(): Flow<PetProfileEntity?> = petDao.observe()
    fun observeWeights(): Flow<List<WeightRecordEntity>> = weightDao.observeAll()
    fun observeEvents(): Flow<List<MedicalEventEntity>> = medicalDao.observeAll()
    fun observeNotes(): Flow<List<DiaryNoteEntity>> = diaryDao.observeAll()

    suspend fun getPet(): PetProfileEntity? = petDao.get()
    suspend fun latestWeight(): Float? = weightDao.latest()?.weightValue

    // ---------- Профиль ----------

    suspend fun saveProfile(
        name: String,
        birthDate: LocalDate,
        isNeutered: Boolean,
        breed: String,
        chronicConditions: String,
        newPhoto: Uri?,
    ) = withContext(Dispatchers.IO) {
        val existing = petDao.get()
        var photoPath = existing?.photoPath
        if (newPhoto != null) {
            val copied = copyPhoto(newPhoto)
            if (copied != null) {
                existing?.photoPath?.let { File(it).delete() }
                photoPath = copied
            }
        }
        petDao.upsert(
            PetProfileEntity(
                id = 1,
                name = name.trim(),
                birthDateEpochDay = birthDate.toEpochDay(),
                isNeutered = isNeutered,
                breed = breed.trim(),
                chronicConditions = chronicConditions.trim(),
                photoPath = photoPath,
            )
        )
    }

    private fun copyPhoto(uri: Uri): String? = runCatching {
        val target = File(context.filesDir, "pet_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    // ---------- Вес ----------

    suspend fun addWeight(kg: Float) {
        weightDao.insert(WeightRecordEntity(timestamp = System.currentTimeMillis(), weightValue = kg))
    }

    suspend fun deleteWeight(record: WeightRecordEntity) = weightDao.delete(record)

    // ---------- Прививки и обработки ----------

    suspend fun addMedicalEvent(
        procedure: ProcedureType,
        drugName: String,
        date: LocalDate,
        remind: Boolean,
        intervalMonths: Int,
    ) = withContext(Dispatchers.IO) {
        val next = if (remind) date.plusMonths(intervalMonths.toLong()) else null

        // Старые напоминания по этой же процедуре больше не нужны.
        medicalDao.getAll()
            .filter { it.procedure == procedure && it.isScheduledReminder }
            .forEach { scheduler.cancel(it.id) }

        val id = medicalDao.insert(
            MedicalEventEntity(
                type = procedure.eventType,
                procedure = procedure,
                drugName = drugName.trim(),
                dateEpochDay = date.toEpochDay(),
                isScheduledReminder = remind,
                nextDateEpochDay = next?.toEpochDay(),
            )
        )
        if (remind && next != null) {
            scheduler.schedule(id, petDao.get()?.name.orEmpty(), procedure, next)
        }
    }

    suspend fun deleteMedicalEvent(event: MedicalEventEntity) = withContext(Dispatchers.IO) {
        scheduler.cancel(event.id)
        medicalDao.delete(event)
    }

    /** Вызывается после перезагрузки устройства: AlarmManager теряет все будильники. */
    suspend fun rescheduleAllReminders() = withContext(Dispatchers.IO) {
        val petName = petDao.get()?.name.orEmpty()
        val upcoming = computeUpcomingWithIds(medicalDao.getAll())
        upcoming.forEach { (event, next) ->
            scheduler.schedule(event.id, petName, event.procedure, next)
        }
    }

    private fun computeUpcomingWithIds(events: List<MedicalEventEntity>): List<Pair<MedicalEventEntity, LocalDate>> =
        events.groupBy { it.procedure }.mapNotNull { (_, list) ->
            val latest = list.maxByOrNull { it.dateEpochDay } ?: return@mapNotNull null
            val next = latest.nextDateEpochDay
            if (latest.isScheduledReminder && next != null) latest to LocalDate.ofEpochDay(next) else null
        }

    // ---------- Дневник ----------

    suspend fun addNote(text: String, tags: List<String>, timestamp: Long) {
        diaryDao.insert(
            DiaryNoteEntity(
                timestamp = timestamp,
                symptomText = text.trim(),
                tags = tags.joinToString("|"),
            )
        )
    }

    suspend fun deleteNote(note: DiaryNoteEntity) = diaryDao.delete(note)
}
