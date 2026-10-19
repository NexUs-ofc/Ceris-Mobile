package com.example.ceris.api

import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.RefreshSessionResponse
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RenovadorDeSessaoTest {
    private val manager = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val refreshed get() = RefreshSessionResponse(jwt(signature = "new"), "", "new-refresh", "")

    private fun unauthorized(challenge: String = "Bearer"): okhttp3.Response =
        okhttp3.Response.Builder()
            .request(
                Request.Builder().url("https://notify.example/api/notifications")
                    .header("Authorization", "Bearer ${manager.snapshot()!!.accessToken}")
                    .tag(SessionSnapshot::class.java, manager.snapshot()).build(),
            )
            .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized")
            .header("WWW-Authenticate", challenge).build()

    private fun authenticator(respond: () -> Response<RefreshSessionResponse>): RenovadorDeSessao =
        RenovadorDeSessao(manager, testApi<AuthAPI> { TestCall(respond) }, {})

    @Test
    fun dois401UsamUmaSoRenovacao() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val auth =
            authenticator {
                calls.incrementAndGet()
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                Response.success(refreshed)
            }
        val response = unauthorized()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit<Request?> { auth.authenticate(null, response) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val second = pool.submit<Request?> { auth.authenticate(null, response) }
            release.countDown()
            assertEquals("Bearer ${refreshed.accessToken}", first.get(5, TimeUnit.SECONDS)?.header("Authorization"))
            assertEquals("Bearer ${refreshed.accessToken}", second.get(5, TimeUnit.SECONDS)?.header("Authorization"))
            assertEquals(1, calls.get())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun errosTemporariosNaoApagamSessao() {
        listOf(429, 500, 503).forEach { code ->
            val before = manager.snapshot()
            val auth = authenticator { Response.error(code, ResponseBody.create(MediaType.parse("application/json"), "{}")) }
            assertNull(auth.authenticate(null, unauthorized()))
            assertEquals(before, manager.snapshot())
        }
    }

    @Test
    fun refresh401DescartaSomenteASessaoAtual() {
        val auth = authenticator { Response.error(401, ResponseBody.create(MediaType.parse("application/json"), "{}")) }
        assertNull(auth.authenticate(null, unauthorized()))
        assertNull(manager.snapshot())
    }

    @Test
    fun logoutDuranteRefreshNaoRestauraTokens() {
        val response = unauthorized()
        val auth =
            authenticator {
                manager.clear()
                Response.success(refreshed)
            }
        assertNull(auth.authenticate(null, response))
        assertNull(manager.snapshot())
    }

    @Test
    fun respostaDaContaAntigaNaoAlteraNovaConta() {
        val response = unauthorized()
        val auth =
            authenticator {
                manager.startSession(jwt("99"), "other-refresh")
                Response.success(refreshed)
            }
        assertNull(auth.authenticate(null, response))
        assertEquals(99, manager.snapshot()?.profileId)
        assertEquals("other-refresh", manager.snapshot()?.refreshToken)
    }

    @Test
    fun desafioApiKeyNaoGastaRefresh() {
        val auth = authenticator { error("Nao deve renovar") }
        assertNull(auth.authenticate(null, unauthorized("ApiKey")))
        assertNotNull(manager.snapshot())
    }
}
