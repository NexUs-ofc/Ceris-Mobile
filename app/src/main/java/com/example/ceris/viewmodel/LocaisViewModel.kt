package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Local
import com.example.ceris.repository.AddressProblem
import com.example.ceris.repository.LocaisRepository
import com.example.ceris.repository.LocaisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocaisStatus { IDLE, LOADING, CONTENT, EMPTY, ERROR, ADDRESS_MISSING, NOT_GEOCODED, AUTH_REQUIRED }

data class LocaisUiState(
    val status: LocaisStatus = LocaisStatus.IDLE,
    val address: String = "",
    val locais: List<Local> = emptyList(),
)

/** Mantem dados na rotacao sem guardar Fragment, View ou listener da tela. */
class LocaisViewModel() : ViewModel() {
    private var generation: () -> String? = { null }
    private var load: (((LocaisResult) -> Unit) -> (() -> Unit))? = null
    private var owner: String? = null
    private var sequence = 0L
    private var cancel: (() -> Unit)? = null
    private val mutableState = MutableStateFlow(LocaisUiState())
    val state = mutableState.asStateFlow()

    internal constructor(
        generation: () -> String?,
        load: ((LocaisResult) -> Unit) -> (() -> Unit),
    ) : this() {
        this.generation = generation
        this.load = load
    }

    fun init(sessions: SessionManager) {
        if (load != null) return
        generation = { sessions.snapshot()?.generation }
        val repository = LocaisRepository.create(sessions)
        load = { deliver -> repository.load(deliver = deliver)::cancel }
    }

    /** onResume e show/hide podem chamar juntos sem repetir a consulta. */
    fun onVisible() {
        val current = generation()
        if (current != owner) {
            invalidate()
            owner = current
            mutableState.value = LocaisUiState()
        }
        if (current == null) {
            mutableState.value = LocaisUiState(LocaisStatus.AUTH_REQUIRED)
        } else if (state.value.status == LocaisStatus.IDLE) {
            refresh()
        }
    }

    /** Retry ou resultado de um endereco efetivamente salvo. */
    fun refresh() {
        val source = load ?: return
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
        var completed = false
        val cancellation =
            source { result ->
                if (request != sequence) return@source
                completed = true
                cancel = null
                if (generation() != current) {
                    invalidate()
                    owner = null
                    mutableState.value = LocaisUiState()
                    onVisible()
                    return@source
                }
                mutableState.value =
                    when (result) {
                        is LocaisResult.Loaded -> LocaisUiState(LocaisStatus.CONTENT, result.address, result.locais)
                        is LocaisResult.Empty -> LocaisUiState(LocaisStatus.EMPTY, result.address)
                        is LocaisResult.AddressRequired ->
                            LocaisUiState(
                                if (result.reason == AddressProblem.MISSING) LocaisStatus.ADDRESS_MISSING else LocaisStatus.NOT_GEOCODED,
                                result.address,
                            )
                        LocaisResult.AuthenticationRequired -> LocaisUiState(LocaisStatus.AUTH_REQUIRED)
                        is LocaisResult.Failure -> LocaisUiState(LocaisStatus.ERROR, previousAddress)
                        LocaisResult.SessionChanged -> LocaisUiState(LocaisStatus.AUTH_REQUIRED)
                    }
            }
        if (request != sequence) {
            cancellation()
        } else if (!completed) {
            cancel = cancellation
        }
    }

    private fun invalidate() {
        sequence++
        cancel?.invoke()
        cancel = null
    }

    override fun onCleared() {
        invalidate()
    }
}
