package com.example.ceris.local

import com.example.ceris.api.AuthAPI
import com.example.ceris.model.SessionAttribute
import com.example.ceris.repository.AuthRepository
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private val preferences = MemoryPreferences()
    private val manager = SessionManager(preferences)

    @Test
    fun migraSessaoExistenteSemTrocarTokens() {
        preferences.edit().putString(SessionKeys.ACCESS_TOKEN, jwt())
            .putString(SessionKeys.REFRESH_TOKEN, "refresh").apply()
        val session = requireNotNull(manager.snapshot())
        assertEquals(12, session.profileId)
        assertEquals(jwt(), session.accessToken)
        assertEquals(session.generation, manager.snapshot()?.generation)
        assertEquals(12, manager.getInt(SessionKeys.PROFILE_ID))
    }

    @Test
    fun renovacaoPreservaGeracaoETrocaOsDoisTokens() {
        manager.startSession(jwt(), "refresh")
        val session = requireNotNull(manager.snapshot())
        assertTrue(manager.renew(session, jwt(signature = "new"), "new-refresh"))
        assertTrue(manager.isCurrent(session))
        assertEquals("new-refresh", manager.snapshot()?.refreshToken)
        assertFalse(manager.renew(session, jwt(), "refresh-antigo"))
    }

    @Test
    fun respostaAntigaNaoRestauraLogout() {
        manager.startSession(jwt(), "refresh")
        val session = requireNotNull(manager.snapshot())
        manager.clear()
        assertFalse(manager.renew(session, jwt(), "late"))
        assertNull(manager.snapshot())
    }

    @Test
    fun novoLoginNaMesmaContaInvalidaCallbacksAntigos() {
        manager.startSession(jwt(), "refresh")
        val previous = requireNotNull(manager.snapshot())
        manager.startSession(jwt(), "refresh-2")
        assertFalse(manager.isCurrent(previous))
        assertFalse(manager.discard(previous))
        assertEquals("refresh-2", manager.snapshot()?.refreshToken)
    }

    @Test
    fun renovacaoNaoPodeTrocarPerfil() {
        manager.startSession(jwt(), "refresh")
        val session = requireNotNull(manager.snapshot())
        assertFalse(manager.renew(session, jwt("99"), "other"))
        assertEquals(session, manager.snapshot())
    }

    @Test
    fun managersCompartilhamSessaoSemGuardarActivity() {
        manager.startSession(jwt(), "refresh")
        assertEquals(manager.snapshot(), SessionManager(preferences).snapshot())
    }

    @Test
    fun salvarTokensPeloAuthPreservaTicketsDeCadastroERecuperacao() {
        manager.set(
            SessionAttribute(SessionKeys.REGISTRATION_ID, "registration"),
            SessionAttribute(SessionKeys.RESET_ID, "reset"),
            SessionAttribute(SessionKeys.RESET_TICKET, "reset-ticket"),
            SessionAttribute(SessionKeys.GOOGLE_TICKET, "google-ticket"),
            SessionAttribute(SessionKeys.CHAT_SESSION_ID, "old-conversation"),
        )
        val auth = AuthRepository(testApi<AuthAPI> { error("Nao deve chamar a rede") }, manager)
        auth.saveTokens(jwt(), "refresh")
        assertEquals(12, manager.snapshot()?.profileId)
        assertEquals("registration", manager.getString(SessionKeys.REGISTRATION_ID))
        assertEquals("reset", manager.getString(SessionKeys.RESET_ID))
        assertEquals("reset-ticket", manager.getString(SessionKeys.RESET_TICKET))
        assertEquals("google-ticket", manager.getString(SessionKeys.GOOGLE_TICKET))
        assertEquals("", manager.getString(SessionKeys.CHAT_SESSION_ID))
    }

    @Test
    fun renovarTokenPreservaConversaEmAndamento() {
        manager.startSession(jwt(), "refresh")
        manager.set(SessionAttribute(SessionKeys.CHAT_SESSION_ID, "conversation"))
        assertTrue(manager.renew(manager.snapshot()!!, jwt(signature = "new"), "new-refresh"))
        assertEquals("conversation", manager.getString(SessionKeys.CHAT_SESSION_ID))
    }
}
