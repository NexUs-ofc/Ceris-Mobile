package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.LocaisAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.Local
import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.ApiError
import com.example.ceris.model.formattedAddress
import com.example.ceris.model.toLocal
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed interface LocaisResult {
    data class QueryStarted(val usesProfileAddress: Boolean) : LocaisResult

    data class Loaded(val address: String, val locais: List<Local>) : LocaisResult

    data class Empty(val address: String) : LocaisResult

    data class AddressRequired(val reason: AddressProblem, val address: String = "") : LocaisResult

    data object AuthenticationRequired : LocaisResult

    data object SessionChanged : LocaisResult

    data class Failure(val statusCode: Int? = null) : LocaisResult

    data object LocationPermissionRequired : LocaisResult

    data object LocationUnavailable : LocaisResult
}

enum class AddressProblem { MISSING, NOT_GEOCODED }

/** Cancela tanto a leitura do perfil quanto a consulta que a sucede. */
class LocaisRequest internal constructor() {
    @Volatile
    var isCanceled: Boolean = false
        private set

    private var active: Call<*>? = null

    @Synchronized
    fun cancel() {
        isCanceled = true
        active?.cancel()
    }

    @Synchronized
    internal fun attach(call: Call<*>) {
        active = call
        if (isCanceled) call.cancel()
    }
}

class LocaisRepository(
    private val api: LocaisAPI,
    private val sessions: SessionManager,
) {
    companion object {
        fun create(sessions: SessionManager): LocaisRepository =
            LocaisRepository(RetrofitClient.getApiAutenticada(ApiBaseUrl.core, LocaisAPI::class.java), sessions)

        /** Endereço resumido exibido entre os endereços salvos. */
        fun enderecoCurto(endereco: AddressDto?): String? {
            endereco ?: return null
            val rua = listOfNotNull(endereco.street, endereco.number)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            return listOf(rua, endereco.neighborhood.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { null }
        }
    }

    fun load(
        radiusKm: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        useProfileFallback: Boolean = false,
        deliver: (LocaisResult) -> Unit,
    ): LocaisRequest {
        require(radiusKm == null || (radiusKm.isFinite() && radiusKm > 0 && radiusKm <= 100))
        require((latitude == null) == (longitude == null))
        require(latitude == null || (latitude.isFinite() && latitude in -90.0..90.0))
        require(longitude == null || (longitude.isFinite() && longitude in -180.0..180.0))
        val request = LocaisRequest()
        val session = sessions.snapshot()
        if (session?.profileId == null) {
            deliver(LocaisResult.AuthenticationRequired)
            return request
        }

        fun emit(result: LocaisResult) {
            if (request.isCanceled) return
            if (!sessions.withCurrentSession(session) { if (!request.isCanceled) deliver(result) }) {
                if (!request.isCanceled) deliver(LocaisResult.SessionChanged)
            }
        }

        val authorization = "Bearer ${session.accessToken}"
        // Com a origem do aparelho, o perfil nao precisa ter endereco ou coordenadas.
        if ((latitude != null && longitude != null) || useProfileFallback) {
            query(
                api.nearby(authorization, session, latitude, longitude, radiusKm),
                session,
                request,
                ::emit,
                address = if (useProfileFallback) "" else null,
            ) { stores ->
                val locais = runCatching { stores.map { it.toLocal() } }.getOrNull()
                emit(
                    when {
                        locais == null -> LocaisResult.Failure()
                        locais.isEmpty() -> LocaisResult.Empty("")
                        else -> LocaisResult.Loaded("", locais)
                    },
                )
            }
            return request
        }
        query(api.profile(authorization, session), session, request, ::emit) { profile ->
            if (profile.id != session.profileId) {
                emit(LocaisResult.Failure())
                return@query
            }
            val address = profile.address
            if (address == null) {
                emit(LocaisResult.AddressRequired(AddressProblem.MISSING))
                return@query
            }
            val formatted = address.formattedAddress()
            query(
                api.nearby(authorization, session, radiusKm = radiusKm),
                session,
                request,
                ::emit,
                address = formatted,
            ) { stores ->
                val locais = runCatching { stores.map { it.toLocal() } }.getOrNull()
                emit(
                    when {
                        locais == null -> LocaisResult.Failure()
                        locais.isEmpty() -> LocaisResult.Empty(formatted)
                        else -> LocaisResult.Loaded(formatted, locais)
                    },
                )
            }
        }
        return request
    }

    private fun <T> query(
        call: Call<T>,
        session: SessionSnapshot,
        request: LocaisRequest,
        emit: (LocaisResult) -> Unit,
        address: String? = null,
        success: (T) -> Unit,
    ) {
        request.attach(call)
        if (request.isCanceled) return
        call.enqueue(
            object : Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: Response<T>,
                ) {
                    val error = response.errorBody()?.use { runCatching { ApiError.parse(it.string()) }.getOrNull() }
                    if (request.isCanceled || call.isCanceled) return
                    val body = response.body()
                    when {
                        !sessions.isCurrent(session) -> emit(LocaisResult.SessionChanged)
                        response.code() == 401 -> emit(LocaisResult.AuthenticationRequired)
                        response.code() == 400 && address != null ->
                            emit(
                                when (error?.message) {
                                    "Perfil autenticado nao possui endereco" ->
                                        LocaisResult.AddressRequired(AddressProblem.MISSING, address)
                                    "Endereco do perfil nao possui latitude e longitude" ->
                                        LocaisResult.AddressRequired(AddressProblem.NOT_GEOCODED, address)
                                    else -> LocaisResult.Failure(400)
                                },
                            )
                        !response.isSuccessful -> emit(LocaisResult.Failure(response.code()))
                        body == null -> emit(LocaisResult.Failure())
                        else -> success(body)
                    }
                }

                override fun onFailure(
                    call: Call<T>,
                    t: Throwable,
                ) {
                    if (!call.isCanceled) emit(LocaisResult.Failure())
                }
            },
        )
    }
}
