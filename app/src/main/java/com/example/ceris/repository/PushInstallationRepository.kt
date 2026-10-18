package com.example.ceris.repository

import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.NotifyAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.PushInstallationResponse
import com.example.ceris.model.dto.RegisterPushInstallationRequest
import retrofit2.Call
import java.net.ProtocolException

class PushInstallationRepository(
    private val api: NotifyAPI,
    private val sessionManager: SessionManager,
    private val configured: Boolean = ApiBaseUrl.configurada(ApiBaseUrl.notify),
) {
    fun registrar(
        installationId: String,
        onSuccess: (PushInstallationResponse) -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<PushInstallationResponse>? {
        val session = notifySession(sessionManager, configured, onFailure) ?: return null
        return api.registrarInstalacao("Bearer ${session.accessToken}", RegisterPushInstallationRequest(installationId), session).also {
                call ->
            call.enqueue(
                NotifyCallback(sessionManager, session, { response ->
                    val body = response.body()
                    if (body != null) onSuccess(body) else onFailure(ProtocolException("Cadastro sem corpo na resposta."))
                }, onError, onFailure),
            )
        }
    }

    fun desvincular(
        id: String,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<Void>? {
        val session = notifySession(sessionManager, configured, onFailure) ?: return null
        return api.desvincularInstalacao("Bearer ${session.accessToken}", id, session).also { call ->
            call.enqueue(NotifyCallback(sessionManager, session, { onSuccess() }, onError, onFailure))
        }
    }
}
