package com.example.ceris.notification

import com.example.ceris.local.SessionManager

/** Publicacao e verificacao de sessao acontecem na mesma secao critica. */
class PushMessageHandler(
    private val sessionManager: SessionManager,
    private val state: PushState,
    private val permitted: () -> Boolean,
    private val publish: (PushMessage) -> Boolean,
) {
    fun receive(data: Map<String, String>): Boolean {
        val message = PushMessage.from(data) ?: return false
        val session = sessionManager.snapshot() ?: return false
        if (message.profileId != session.profileId) return false
        var displayed = false
        sessionManager.withCurrentSession(session) {
            if (permitted() && !state.wasPublished(session, message.notificationId) && publish(message)) {
                state.published(session, message.notificationId)
                displayed = true
            }
        }
        return displayed
    }
}
