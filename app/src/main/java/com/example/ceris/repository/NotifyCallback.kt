package com.example.ceris.repository

import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotifyNotConfigured : IllegalStateException("Configure NOTIFY_API_BASE_URL para acessar as notificacoes.")

class NotifySessionChanged : IllegalStateException("A sessao mudou. Atualize as notificacoes.")

class NotifyProfileMissing : IllegalStateException("Nao foi possivel identificar o perfil. Entre novamente.")

internal fun notifySession(
    sessionManager: SessionManager,
    configured: Boolean,
    onFailure: (Throwable) -> Unit,
): SessionSnapshot? {
    if (!configured) {
        onFailure(NotifyNotConfigured())
        return null
    }
    val session = sessionManager.snapshot()
    if (session == null) {
        onFailure(ShoppingListRepository.SemSessao())
        return null
    }
    if (session.profileId == null) {
        onFailure(NotifyProfileMissing())
        return null
    }
    return session
}

/** Mantem os callbacks do projeto sem entregar respostas para outra sessao. */
internal class NotifyCallback<T>(
    private val sessionManager: SessionManager,
    private val session: SessionSnapshot,
    private val onSuccess: (Response<T>) -> Unit,
    private val onError: (Int, String?) -> Unit,
    private val onFailure: (Throwable) -> Unit,
) : Callback<T> {
    override fun onResponse(
        call: Call<T>,
        response: Response<T>,
    ) {
        if (!sessionManager.isCurrent(session)) {
            response.errorBody()?.close()
            onFailure(NotifySessionChanged())
        } else if (response.isSuccessful) {
            onSuccess(response)
        } else {
            val error = runCatching { response.errorBody()?.use { it.string() } }.getOrNull()
            onError(response.code(), error)
        }
    }

    override fun onFailure(
        call: Call<T>,
        t: Throwable,
    ) {
        onFailure(if (sessionManager.isCurrent(session)) t else NotifySessionChanged())
    }
}
