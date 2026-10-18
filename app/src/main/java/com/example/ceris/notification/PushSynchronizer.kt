package com.example.ceris.notification

import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.repository.PushInstallationRepository
import java.io.IOException

/** Executado em serie fora da UI. true indica falha temporaria e nova tentativa. */
class PushSynchronizer(
    private val repository: PushInstallationRepository,
    private val sessionManager: SessionManager,
    private val state: PushState,
    private val permitted: () -> Boolean,
    private val report: (String) -> Unit,
) {
    fun synchronize(): Boolean {
        val session = sessionManager.snapshot()?.takeIf { it.profileId != null } ?: return false
        val fid = state.installationId ?: return true
        if (!permitted()) return false
        val previous = state.binding()
        return try {
            val response = repository.registrarImediato(fid, session)
            val body = response.body()
            response.errorBody()?.close()
            if (!response.isSuccessful) {
                report("Cadastro push recusado com HTTP ${response.code()}")
                response.code() == 408 || response.code() == 429 || response.code() >= 500
            } else if (body == null || body.id.isNullOrBlank() || body.status != "ACTIVE") {
                report("Resposta invalida no cadastro push")
                false
            } else {
                var applied = false
                sessionManager.withCurrentSession(session) {
                    if (permitted() && state.installationId == fid) {
                        state.bind(session, fid, body.id)
                        applied = true
                    }
                }
                if (!applied) {
                    revoke(PushState.Binding(body.id, fid, session.generation, session.profileId!!), session)
                } else if (previous != null && previous.id != body.id && previous.generation == session.generation) {
                    revoke(previous, sessionManager.snapshot() ?: session)
                }
                !applied && sessionManager.snapshot() != null && permitted()
            }
        } catch (_: IOException) {
            report("Nao foi possivel sincronizar o cadastro push agora")
            sessionManager.isCurrent(session) && permitted()
        }
    }

    fun revoke(
        binding: PushState.Binding,
        session: SessionSnapshot,
    ) {
        if (binding.profileId != session.profileId) return
        val currentBinding = state.binding()
        // Um novo login da mesma conta pode reutilizar o mesmo documento/FID.
        if (currentBinding?.id == binding.id && currentBinding.generation != binding.generation) return
        try {
            val response = repository.desvincularImediato(binding.id, session)
            response.errorBody()?.close()
            if (!response.isSuccessful) report("Desvinculacao push recusada com HTTP ${response.code()}")
        } catch (_: IOException) {
            // Logout local termina mesmo sem rede; nunca persiste tokens antigos para retry.
            report("Nao foi possivel desvincular o cadastro push agora")
        }
    }
}
