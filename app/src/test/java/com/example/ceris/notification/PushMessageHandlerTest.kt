package com.example.ceris.notification

import com.example.ceris.local.SessionManager
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.jwt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PushMessageHandlerTest {
    private val manager = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val preferences = MemoryPreferences()
    private val state = PushState(preferences)
    private var allowed = true
    private var displayed = 0
    private val payload =
        mapOf(
            "notificationId" to "670c00000000000000000001",
            "profileId" to "12",
            "type" to "PRODUCT_EXPIRING",
            "sourceId" to "45",
            "title" to "Produto vence hoje",
            "message" to "O leite vence hoje.",
        )

    private fun handler(store: PushState = state) =
        PushMessageHandler(manager, store, { allowed }, {
            displayed++
            true
        })

    @Test
    fun reconheceOsQuatroTiposESeusCanais() {
        mapOf(
            "PRODUCT_EXPIRING" to NotificationChannels.PRODUTOS,
            "UPCOMING_EVENT" to NotificationChannels.EVENTOS,
            "NEARBY_STORE" to NotificationChannels.LOJAS,
            "NEW_NEARBY_STORE" to NotificationChannels.LOJAS,
        ).forEach { (type, channel) -> assertEquals(channel, PushMessage.from(payload + ("type" to type))?.channel) }
    }

    @Test
    fun descartaPayloadIncompletoInvalidoOuTipoDesconhecido() {
        payload.keys.forEach { assertNull(PushMessage.from(payload - it)) }
        listOf("", "0", "-1", "abc", "2147483648").forEach {
            assertNull(PushMessage.from(payload + ("profileId" to it)))
        }
        assertNull(PushMessage.from(payload + ("type" to "FUTURE_TYPE")))
        assertNull(PushMessage.from(payload + ("title" to "x".repeat(257))))
    }

    @Test
    fun semSessaoOuComOutroPerfilNaoPublica() {
        assertFalse(handler().receive(payload + ("profileId" to "99")))
        manager.clear()
        assertFalse(handler().receive(payload))
        assertEquals(0, displayed)
    }

    @Test
    fun semPermissaoNaoMarcaComoPublicado() {
        allowed = false
        assertFalse(handler().receive(payload))
        allowed = true
        assertTrue(handler().receive(payload))
        assertEquals(1, displayed)
    }

    @Test
    fun duplicacaoContinuaBloqueadaDepoisDeReabrirAsPreferencias() {
        assertTrue(handler().receive(payload))
        assertFalse(handler(PushState(preferences)).receive(payload))
        assertEquals(1, displayed)
    }

    @Test
    fun falhaNaPublicacaoNaoImpedeNovaTentativa() {
        val failed = PushMessageHandler(manager, state, { true }, { false })
        assertFalse(failed.receive(payload))
        assertTrue(handler().receive(payload))
    }

    @Test
    fun trocaDeContaNaoReaproveitaDeduplicacaoDaContaAnterior() {
        assertTrue(handler().receive(payload))
        manager.startSession(jwt("99"), "other-refresh")
        assertFalse(handler().receive(payload))
        assertTrue(handler().receive(payload + ("profileId" to "99")))
    }

    @Test
    fun logoutConcorrenteNaoEntraEntreValidacaoEPublicacao() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val logoutStarted = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val receive =
                pool.submit<Boolean> {
                    PushMessageHandler(manager, state, { true }, {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                        assertNotNull(manager.snapshot())
                        true
                    }).receive(payload)
                }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val logout =
                pool.submit {
                    logoutStarted.countDown()
                    manager.clear()
                }
            assertTrue(logoutStarted.await(5, TimeUnit.SECONDS))
            release.countDown()
            assertTrue(receive.get(5, TimeUnit.SECONDS))
            logout.get(5, TimeUnit.SECONDS)
            assertNull(manager.snapshot())
            assertFalse(handler().receive(payload))
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun registroDoFirebaseValidaFidEPreservaPendenciaAteCallback() {
        assertTrue(state.registrationPending)
        assertFalse(state.registered("fid com espaco"))
        assertTrue(state.registrationPending)
        assertTrue(state.registered("firebase-fid_1"))
        assertEquals("firebase-fid_1", state.installationId)
        assertFalse(state.registrationPending)
        state.requestRegistration()
        assertTrue(PushState(preferences).registrationPending)
    }

    @Test
    fun limpezaNaoApagaEscolhaDePermissaoNemIdentificadorDaInstalacao() {
        state.registered("fid")
        state.requestPermission()
        state.bind(manager.snapshot()!!, "fid", "mongo-id")
        handler().receive(payload)
        state.clearSession()
        assertNull(state.binding())
        assertFalse(state.wasPublished(manager.snapshot()!!, payload.getValue("notificationId")))
        assertEquals("fid", state.installationId)
        assertTrue(state.permissionRequested)
        assertFalse(preferences.all.values.contains(manager.snapshot()!!.accessToken))
    }

    @Test
    fun limpezaAtrasadaDaSessaoAntigaPreservaVinculoEAvisosDaSessaoNova() {
        handler().receive(payload)
        manager.startSession(jwt("99"), "new-refresh")
        val current = manager.snapshot()!!
        state.bind(current, "fid", "mongo-id")
        handler().receive(payload + ("profileId" to "99"))
        state.clearSession(current.generation)
        assertEquals(current.generation, state.binding()?.generation)
        assertTrue(state.wasPublished(current, payload.getValue("notificationId")))
    }

    @Test
    fun deduplicacaoTemLimitePersistente() {
        val session = manager.snapshot()!!
        repeat(PushState.MAX_SHOWN + 2) { state.published(session, it.toString(), it.toLong()) }
        assertFalse(state.wasPublished(session, "0"))
        assertFalse(state.wasPublished(session, "1"))
        assertTrue(state.wasPublished(session, (PushState.MAX_SHOWN + 1).toString()))
        assertEquals(PushState.MAX_SHOWN, preferences.all.keys.count { it.startsWith("shown:") })
    }
}
