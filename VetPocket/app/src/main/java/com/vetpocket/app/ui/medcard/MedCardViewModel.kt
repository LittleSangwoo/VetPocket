package com.vetpocket.app.ui.medcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.PetRepository
import com.vetpocket.app.data.db.DiaryNoteEntity
import com.vetpocket.app.data.db.MedicalEventEntity
import com.vetpocket.app.data.db.ProcedureType
import com.vetpocket.app.data.db.WeightRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedCardState(
    val events: List<MedicalEventEntity> = emptyList(),
    val weights: List<WeightRecordEntity> = emptyList(),
    val notes: List<DiaryNoteEntity> = emptyList(),
)

class MedCardViewModel(private val repo: PetRepository) : ViewModel() {

    val state: StateFlow<MedCardState> = combine(
        repo.observeEvents(),
        repo.observeWeights(),
        repo.observeNotes(),
    ) { events, weights, notes ->
        MedCardState(events, weights, notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedCardState())

    fun addEvent(procedure: ProcedureType, drug: String, date: LocalDate, remind: Boolean, intervalMonths: Int) {
        viewModelScope.launch { repo.addMedicalEvent(procedure, drug, date, remind, intervalMonths) }
    }

    fun deleteEvent(event: MedicalEventEntity) {
        viewModelScope.launch { repo.deleteMedicalEvent(event) }
    }

    fun deleteWeight(record: WeightRecordEntity) {
        viewModelScope.launch { repo.deleteWeight(record) }
    }

    fun addNote(text: String, tags: List<String>, timestamp: Long) {
        viewModelScope.launch { repo.addNote(text, tags, timestamp) }
    }

    fun deleteNote(note: DiaryNoteEntity) {
        viewModelScope.launch { repo.deleteNote(note) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MedCardViewModel(container.repository) }
        }
    }
}
