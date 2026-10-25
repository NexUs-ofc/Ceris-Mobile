package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.local.SessionManager
import com.example.ceris.location.LocationSource
import com.example.ceris.model.Local
import com.example.ceris.repository.AddressProblem
import com.example.ceris.repository.LocaisRepository
import com.example.ceris.repository.LocaisResult
import com.example.ceris.repository.NearbyLocationLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocaisStatus {
    IDLE,
    LOADING,
    CONTENT,
    EMPTY,
    ERROR,
    ADDRESS_MISSING,
    NOT_GEOCODED,
    AUTH_REQUIRED,
    LOCATION_PERMISSION,
    LOCATION_UNAVAILABLE,
}

data class LocaisUiState(
    val status: LocaisStatus = LocaisStatus.IDLE,
    val address: String = "",
    val locais: List<Local> = emptyList(),
    val usesProfileAddress: Boolean = false,
)

/** Mantem dados na rotacao sem guardar Fragment, View ou listener da tela. */
class LocaisViewModel() : ViewModel() {
    private var generation: () -> String? = { null }
    private var load: (((LocaisResult) -> Unit) -> (() -> Unit))? = null
    private var owner: String? = null
    private var sequence = 0L
    private var cancel: (() -> Unit)? = null
    private var visible = false
    private var permissionDenied = false
    private val mutableState = MutableStateFlow(LocaisUiState())
    val state = mutableState.asStateFlow()

    internal constructor(
        generation: () -> String?,
        load: ((LocaisResult) -> Unit) -> (() -> Unit),
    ) : this() {
        this.generation = generation
        this.load = load
    }

    fun init(
        sessions: SessionManager,
        location: LocationSource,
    ) {
        if (load != null) return
        generation = { sessions.snapshot()?.generation }
        val repository = LocaisRepository.create(sessions)
        val loader =
            NearbyLocationLoader(
                location,
                generation,
                fallback = { deliver -> repository.load(useProfileFallback = true, deliver = deliver)::cancel },
                permissionFallbackAllowed = { permissionDenied },
            ) { point, deliver ->
                repository.load(latitude = point.latitude, longitude = point.longitude, deliver = deliver)::cancel
            }
        load = loader::load
    }

    /** onResume e show/hide podem chamar juntos sem repetir a consulta. */
    fun onVisible() {
        val returning = !visible
        visible = true
        val current = generation()
        if (current != owner) {
            invalidate()
            owner = current
            mutableState.value = LocaisUiState()
        }
        if (current == null) {
            mutableState.value = LocaisUiState(LocaisStatus.AUTH_REQUIRED)
        } else if (returning || state.value.status == LocaisStatus.IDLE) {
            refresh()
        }
    }

    /** Retry ou resultado de um endereco efetivamente salvo. */
    fun refresh() {
        val source = load ?: return
        visible = true
        val current = generation()
        val previousAddress = state.value.address.takeIf { current == owner }.orEmpty()
        invalidate()
        owner = current
        if (current == null) {
            mutableState.value = LocaisUiState(LocaisStatus.AUTH_REQUIRED)
            return
        }
        mutableState.value = LocaisUiState(LocaisStatus.LOADING, previousAddress)
        val request = sequence
        val cancellation =
            source { result ->
                if (request != sequence) return@source
                if (generation() != current) {
                    invalidate()
                    owner = null
                    mutableState.value = LocaisUiState()
                    onVisible()
                    return@source
                }
                mutableState.value =
                    when (result) {
                        is LocaisResult.QueryStarted ->
                            if (state.value.usesProfileAddress == result.usesProfileAddress &&
                                state.value.status in setOf(LocaisStatus.CONTENT, LocaisStatus.EMPTY)
                            ) {
                                state.value
                            } else {
                                LocaisUiState(LocaisStatus.LOADING, usesProfileAddress = result.usesProfileAddress)
                            }
                        is LocaisResult.Loaded ->
                            LocaisUiState(LocaisStatus.CONTENT, result.address, result.locais, state.value.usesProfileAddress)
                        is LocaisResult.Empty ->
                            LocaisUiState(LocaisStatus.EMPTY, result.address, usesProfileAddress = state.value.usesProfileAddress)
                        is LocaisResult.AddressRequired ->
                            LocaisUiState(
                                if (result.reason == AddressProblem.MISSING) LocaisStatus.ADDRESS_MISSING else LocaisStatus.NOT_GEOCODED,
                                result.address,
                                usesProfileAddress = true,
                            )
                        LocaisResult.AuthenticationRequired -> LocaisUiState(LocaisStatus.AUTH_REQUIRED)
                        is LocaisResult.Failure ->
                            LocaisUiState(LocaisStatus.ERROR, state.value.address, usesProfileAddress = state.value.usesProfileAddress)
                        LocaisResult.SessionChanged -> LocaisUiState(LocaisStatus.AUTH_REQUIRED)
                        LocaisResult.LocationPermissionRequired -> LocaisUiState(LocaisStatus.LOCATION_PERMISSION)
                        LocaisResult.LocationUnavailable -> LocaisUiState(LocaisStatus.LOCATION_UNAVAILABLE)
                    }
            }
        if (request != sequence) {
            cancellation()
        } else {
            cancel = cancellation
        }
    }

    private fun invalidate() {
        sequence++
        cancel?.invoke()
        cancel = null
    }

    fun onNotVisible() {
        visible = false
        invalidate()
        if (state.value.status == LocaisStatus.LOADING) {
            mutableState.value = LocaisUiState()
        }
    }

    fun allowPermissionFallback(allowed: Boolean) {
        permissionDenied = allowed
    }

    fun onLocationPermissionResult(granted: Boolean) {
        permissionDenied = !granted
        refresh()
    }

    override fun onCleared() {
        invalidate()
    }
}
