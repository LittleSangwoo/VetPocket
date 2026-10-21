package com.vetpocket.app.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.PetRepository
import com.vetpocket.app.data.UpcomingEvent
import com.vetpocket.app.data.computeUpcoming
import com.vetpocket.app.data.db.PetProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val loaded: Boolean = false,
    val pet: PetProfileEntity? = null,
    val latestWeight: Float? = null,
    val upcoming: List<UpcomingEvent> = emptyList(),
)

class DashboardViewModel(private val repo: PetRepository) : ViewModel() {

    val state: StateFlow<DashboardState> = combine(
        repo.observePet(),
        repo.observeWeights(),
        repo.observeEvents(),
    ) { pet, weights, events ->
        DashboardState(
            loaded = true,
            pet = pet,
            latestWeight = weights.lastOrNull()?.weightValue,
            upcoming = computeUpcoming(events),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun addWeight(kg: Float) {
        viewModelScope.launch { repo.addWeight(kg) }
    }

    fun saveProfile(
        name: String,
        birthDate: LocalDate,
        neutered: Boolean,
        breed: String,
        chronic: String,
        photo: Uri?,
    ) {
        viewModelScope.launch { repo.saveProfile(name, birthDate, neutered, breed, chronic, photo) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { DashboardViewModel(container.repository) }
        }
    }
}
