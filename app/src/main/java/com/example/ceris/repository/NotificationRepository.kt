package com.example.ceris.repository

import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.NotifyAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.NotificationResponse
import com.example.ceris.model.dto.SliceResponse
import retrofit2.Call
import java.net.ProtocolException

class NotificationRepository(
    private val api: NotifyAPI,
    private val sessionManager: SessionManager,
    private val configured: Boolean = ApiBaseUrl.configurada(ApiBaseUrl.notify),
) {
    fun listar(
        page: Int = 0,
        size: Int = 20,
        onSuccess: (SliceResponse<NotificationResponse>) -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<SliceResponse<NotificationResponse>>? {
        if (page < 0 || size !in 1..100) {
            onFailure(IllegalArgumentException("Pagina ou tamanho invalidos."))
            return null
        }
        val session = notifySession(sessionManager, configured, onFailure) ?: return null
        return api.listar("Bearer ${session.accessToken}", page, size, session).also { call ->
            call.enqueue(
                NotifyCallback(sessionManager, session, { response ->
                    val body = response.body()
                    if (body != null) onSuccess(body) else onFailure(ProtocolException("Historico sem corpo na resposta."))
                }, onError, onFailure),
            )
        }
    }

    fun marcarComoLida(
        id: String,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<Void>? {
        val session = notifySession(sessionManager, configured, onFailure) ?: return null
        return api.marcarComoLida("Bearer ${session.accessToken}", id, session).also { call ->
            call.enqueue(NotifyCallback(sessionManager, session, { onSuccess() }, onError, onFailure))
        }
    }
}
