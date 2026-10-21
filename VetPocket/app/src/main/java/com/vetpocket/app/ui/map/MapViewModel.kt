package com.vetpocket.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraTarget(val point: Point, val zoom: Float, val token: Long = System.nanoTime())

data class MapUiState(
    val userPoint: Point? = null,
    val clinics: List<Clinic> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val openNowOnly: Boolean = false,
    val selectedId: String? = null,
    val cameraTarget: CameraTarget? = null,
) {
    val visibleClinics: List<Clinic>
        get() = if (openNowOnly) clinics.filter { it.isOpenNow == true } else clinics

    val selected: Clinic?
        get() = visibleClinics.firstOrNull { it.id == selectedId }
}

class MapViewModel : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val searcher by lazy { ClinicSearcher() }

    fun onLocation(point: Point?) {
        if (point == null) {
            _state.update {
                it.copy(
                    loading = false,
                    message = "Не удалось определить местоположение. Включите геолокацию и нажмите «Обновить».",
                )
            }
            return
        }
        _state.update { it.copy(userPoint = point, cameraTarget = CameraTarget(point, 13.5f), message = null) }
        search(point)
    }

    fun refreshSearch() {
        _state.value.userPoint?.let { search(it) }
    }

    private fun search(center: Point) {
        _state.update { it.copy(loading = true, message = null) }
        searcher.search(
            center,
            onResult = { clinics ->
                _state.update {
                    it.copy(
                        loading = false,
                        clinics = clinics,
                        selectedId = null,
                        message = if (clinics.isEmpty()) "Рядом не найдено ветеринарных клиник." else null,
                    )
                }
            },
            onFailure = { msg -> _state.update { it.copy(loading = false, message = msg) } },
        )
    }

    fun setOpenNow(enabled: Boolean) {
        _state.update { s ->
            val keepSelection = s.selectedId?.let { id ->
                !enabled || s.clinics.firstOrNull { it.id == id }?.isOpenNow == true
            } ?: false
            s.copy(
                openNowOnly = enabled,
                selectedId = if (keepSelection) s.selectedId else null,
                message = null,
            )
        }
    }

    fun select(clinic: Clinic?) {
        _state.update {
            it.copy(
                selectedId = clinic?.id,
                cameraTarget = clinic?.let { c -> CameraTarget(c.point, 15f) } ?: it.cameraTarget,
            )
        }
    }

    fun recenter() {
        _state.update { s -> s.userPoint?.let { s.copy(cameraTarget = CameraTarget(it, 14f)) } ?: s }
    }

    override fun onCleared() {
        if (MapKitSetup.isConfigured) runCatching { searcher.cancel() }
        super.onCleared()
    }

    companion object {
        fun factory() = viewModelFactory { initializer { MapViewModel() } }
    }
}
