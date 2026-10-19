package com.example.ceris.api

import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.ChatRequest
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.jwt
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

class SessionRequestInterceptorTest {
    private val manager = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private var proceeded = false

    private fun chain(request: Request): Interceptor.Chain =
        Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java),
        ) { _, method, args ->
            when (method.name) {
                "request" -> request
                "proceed" -> {
                    proceeded = true
                    Response.Builder().request(args!![0] as Request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
                }
                else -> error("Metodo inesperado: ${method.name}")
            }
        } as Interceptor.Chain

    private fun request(session: SessionSnapshot) =
        Request.Builder().url("https://notify.example/")
            .header("Authorization", "Bearer ${session.accessToken}").tag(SessionSnapshot::class.java, session).build()

    @Test
    fun chamadaEnfileiradaUsaTokenJaRenovadoDaMesmaSessao() {
        val session = requireNotNull(manager.snapshot())
        val queued = request(session)
        manager.renew(session, jwt(signature = "new"), "new-refresh")
        val response = SessionRequestInterceptor(manager).intercept(chain(queued))
        assertEquals("Bearer ${jwt(signature = "new")}", response.request().header("Authorization"))
    }

    @Test
    fun chamadaEnfileiradaNaoSaiDepoisDeNovoLoginNaMesmaConta() {
        val queued = request(requireNotNull(manager.snapshot()))
        manager.startSession(jwt(), "other-refresh")
        assertThrows(IOException::class.java) { SessionRequestInterceptor(manager).intercept(chain(queued)) }
        assertFalse(proceeded)
    }

    @Test
    fun listasEChatEnfileiradosUsamTokenRenovadoSemAlterarCorpoOuApiKey() {
        RetrofitClient.configurarRenovacao(manager)
        val token = manager.snapshot()!!.accessToken
        val lists = RetrofitClient.getApiAutenticada("https://core.example/", ShoppingListAPI::class.java)
        val chat = RetrofitClient.getApiAutenticada("https://ia.example/", ChatAPI::class.java)
        val queuedList = lists.listar("Bearer $token").request()
        val queuedChat = chat.enviar("Bearer $token", "chat-key", ChatRequest("ola", "conversation")).request()
        val previous = manager.snapshot()!!
        manager.renew(previous, jwt(signature = "new"), "new-refresh")

        listOf(queuedList, queuedChat).forEach { queued ->
            assertEquals(previous, queued.tag(SessionSnapshot::class.java))
            val sent = SessionRequestInterceptor(manager).intercept(chain(queued)).request()
            assertEquals("Bearer ${jwt(signature = "new")}", sent.header("Authorization"))
            assertEquals(queued.url(), sent.url())
            assertEquals(queued.method(), sent.method())
            assertEquals(queued.body(), sent.body())
            assertEquals(queued.header("X-API-Key"), sent.header("X-API-Key"))
        }
    }

    @Test
    fun chamadaLegadaEnfileiradaNaoSaiDepoisDeLogoutOuTrocaDeConta() {
        RetrofitClient.configurarRenovacao(manager)
        val lists = RetrofitClient.getApiAutenticada("https://core.example/", ShoppingListAPI::class.java)
        val queued = lists.listar("Bearer ${manager.snapshot()!!.accessToken}").request()
        manager.clear()
        assertThrows(IOException::class.java) { SessionRequestInterceptor(manager).intercept(chain(queued)) }
        manager.startSession(jwt("99"), "other-refresh")
        assertThrows(IOException::class.java) { SessionRequestInterceptor(manager).intercept(chain(queued)) }
        assertFalse(proceeded)
    }

    @Test
    fun fabricaPreservaTagOriginalDoNotifyMesmoDepoisDeRenovar() {
        val session = manager.snapshot()!!
        val tagged = request(session)
        manager.renew(session, jwt(signature = "new"), "new-refresh")
        val factory = SessionRequestInterceptor(manager).vincularChamadas(okhttp3.OkHttpClient())
        val queued = factory.newCall(tagged).request()
        assertEquals(session, queued.tag(SessionSnapshot::class.java))
        val sent = SessionRequestInterceptor(manager).intercept(chain(queued)).request()
        assertEquals("Bearer ${jwt(signature = "new")}", sent.header("Authorization"))
    }

    @Test
    fun chamadaPublicaFuncionaSemSessao() {
        manager.clear()
        val interceptor = SessionRequestInterceptor(manager)
        val publicRequest = Request.Builder().url("https://auth.example/status").build()
        val queued = interceptor.vincularChamadas(okhttp3.OkHttpClient()).newCall(publicRequest).request()
        assertEquals(publicRequest, interceptor.intercept(chain(queued)).request())
    }
}
