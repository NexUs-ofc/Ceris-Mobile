package com.example.ceris.repository

import com.example.ceris.api.NotificationSourceAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.EventNotificationSource
import com.example.ceris.model.dto.StockNotificationSource
import com.example.ceris.model.dto.StoreNotificationSource
import com.example.ceris.notification.NotificationLink
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class NotificationSourceRepositoryTest {
    private val sessions = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val product = StockNotificationSource(45, "Leite", "Marca", "L", 2, "2026-10-19")

    private fun link(type: String = "PRODUCT_EXPIRING") =
        requireNotNull(NotificationLink.parse("ceris://notification/12/notice?type=$type&sourceId=45"))

    private fun load(
        api: NotificationSourceAPI,
        type: String = "PRODUCT_EXPIRING",
    ): NotificationSourceResult? {
        var result: NotificationSourceResult? = null
        NotificationSourceRepository(api, sessions).load(link(type)) { result = it }
        return result
    }

    @Test
    fun produtoBuscaIdDoItemDeEstoqueEIgnoraOutrosItens() {
        val api =
            testApi<NotificationSourceAPI> { method ->
                assertEquals("stock", method)
                TestCall { Response.success(listOf(product.copy(id = 44), product)) }
            }
        assertEquals(NotificationSourceResult.Loaded(NotificationContent.Product(product)), load(api))
    }

    @Test
    fun produtoRemovidoNaoAbreOutroItem() {
        val api = testApi<NotificationSourceAPI> { TestCall { Response.success(listOf(product.copy(id = 44))) } }
        assertEquals(NotificationSourceResult.Unavailable, load(api))
    }

    @Test
    fun lojaEEventoBuscamRegistroPeloId() {
        val store = StoreNotificationSource(45, "Mercado", null, null, null, null, null, "08:00", "18:00")
        val event = EventNotificationSource("45", "Jantar", "Familia", "2026-10-19T19:00:00", 60, "Casa", 4)
        val api =
            testApi<NotificationSourceAPI> {
                when (it) {
                    "store" -> TestCall { Response.success(store) }
                    "event" -> TestCall { Response.success(event) }
                    else -> error("Endpoint incorreto")
                }
            }
        assertEquals(NotificationSourceResult.Loaded(NotificationContent.Store(store)), load(api, "NEARBY_STORE"))
        assertEquals(NotificationSourceResult.Loaded(NotificationContent.Store(store)), load(api, "NEW_NEARBY_STORE"))
        assertEquals(NotificationSourceResult.Loaded(NotificationContent.Event(event)), load(api, "UPCOMING_EVENT"))
    }

    @Test
    fun naoConsultaCoreSemSessaoOuComOutraConta() {
        val api = testApi<NotificationSourceAPI> { error("Nao pode enviar requisicao") }
        sessions.startSession(jwt("99"), "refresh")
        assertEquals(NotificationSourceResult.WrongAccount, load(api))
        sessions.clear()
        assertEquals(NotificationSourceResult.AuthenticationRequired, load(api))
        assertNull(NotificationSourceRepository(api, sessions).load(link()) {})
    }

    @Test
    fun distingueSessaoConteudoRemovidoEFalhaTemporaria() {
        mapOf(
            401 to NotificationSourceResult.AuthenticationRequired,
            403 to NotificationSourceResult.Unavailable,
            404 to NotificationSourceResult.Unavailable,
            410 to NotificationSourceResult.Unavailable,
            429 to NotificationSourceResult.Failure,
            500 to NotificationSourceResult.Failure,
        ).forEach { (code, expected) ->
            val api =
                testApi<NotificationSourceAPI> {
                    TestCall<List<StockNotificationSource>> { Response.error(code, ResponseBody.create(null, "")) }
                }
            assertEquals(expected, load(api))
        }
    }

    @Test
    fun naoEntregaRespostaDepoisDeLogoutOuTrocaDeConta() {
        val api =
            testApi<NotificationSourceAPI> {
                TestCall {
                    sessions.startSession(jwt("99"), "other-refresh")
                    Response.success(listOf(product))
                }
            }
        assertEquals(NotificationSourceResult.SessionChanged, load(api))
    }

    @Test
    fun respostaInconsistenteNaoAbreRegistroErrado() {
        val api =
            testApi<NotificationSourceAPI> {
                TestCall { Response.success(StoreNotificationSource(99, "Outra loja", null, null, null, null, null, null, null)) }
            }
        assertEquals(NotificationSourceResult.Failure, load(api, "NEARBY_STORE"))
        val empty = testApi<NotificationSourceAPI> { TestCall<List<StockNotificationSource>> { Response.success(null) } }
        assertEquals(NotificationSourceResult.Failure, load(empty))
    }

    @Test
    fun falhaDeRedePermiteNovaTentativaSemConfundirComRemocao() {
        val base = TestCall<List<StockNotificationSource>> { error("Nao deve executar") }
        val failed =
            object : Call<List<StockNotificationSource>> by base {
                override fun enqueue(callback: Callback<List<StockNotificationSource>>) {
                    callback.onFailure(this, IOException("Offline"))
                }
            }
        assertEquals(NotificationSourceResult.Failure, load(testApi { failed }))
        assertEquals(sessions.snapshot()?.profileId, 12)
    }

    @Test
    fun cancelarAoSairDaTelaIgnoraRespostaAtrasada() {
        lateinit var callback: Callback<List<StockNotificationSource>>
        val base = TestCall { Response.success(listOf(product)) }
        val delayed =
            object : Call<List<StockNotificationSource>> by base {
                override fun enqueue(value: Callback<List<StockNotificationSource>>) {
                    callback = value
                }
            }
        var result: NotificationSourceResult? = null
        val call = NotificationSourceRepository(testApi { delayed }, sessions).load(link()) { result = it }
        requireNotNull(call).cancel()
        callback.onResponse(delayed, Response.success(listOf(product)))
        assertNull(result)
    }
}
