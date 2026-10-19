package com.example.ceris.repository

import com.example.ceris.api.NotifyAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.NotificationResponse
import com.example.ceris.model.dto.PushInstallationResponse
import com.example.ceris.model.dto.SliceResponse
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class NotifyRepositoryTest {
    private val manager = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }

    @Test
    fun naoTentaLocalhostQuandoNotifyNaoEstaConfigurado() {
        val api = testApi<NotifyAPI> { error("Nao pode chamar a API") }
        var failure: Throwable? = null
        val call =
            NotificationRepository(api, manager, false).listar(
                onSuccess = { fail("Nao pode ter sucesso") },
                onError = { _, _ -> fail() },
                onFailure = { failure = it },
            )
        assertNull(call)
        assertTrue(failure is NotifyNotConfigured)
    }

    @Test
    fun naoEnviaCadastroSemSessao() {
        manager.clear()
        val api = testApi<NotifyAPI> { error("Nao pode chamar a API") }
        var failure: Throwable? = null
        PushInstallationRepository(api, manager, true).registrar(
            "fid",
            { fail() },
            { _, _ -> fail() },
            { failure = it },
        )
        assertTrue(failure is ShoppingListRepository.SemSessao)
    }

    @Test
    fun leituraEDesvinculacaoAceitam204SemCorpo() {
        val api = testApi<NotifyAPI> { TestCall<Void> { Response.success<Void>(204, null) } }
        var successes = 0
        val read =
            NotificationRepository(api, manager, true).marcarComoLida(
                "aviso",
                { successes++ },
                { _, _ -> fail() },
                { fail(it.toString()) },
            )
        val revoke =
            PushInstallationRepository(api, manager, true).desvincular(
                "cadastro",
                { successes++ },
                { _, _ -> fail() },
                { fail(it.toString()) },
            )
        assertEquals(2, successes)
        assertNotNull(read)
        assertNotNull(revoke)
    }

    @Test
    fun preservaHasNextETipoDesconhecido() {
        val item = NotificationResponse("aviso", "TIPO_FUTURO", "45", "Titulo", "Mensagem", "2026-10-17T14:00:00Z", null, false)
        val api = testApi<NotifyAPI> { TestCall { Response.success(SliceResponse(listOf(item), true)) } }
        var result: SliceResponse<NotificationResponse>? = null
        NotificationRepository(api, manager, true).listar(
            onSuccess = { result = it },
            onError = { _, _ -> fail() },
            onFailure = { fail(it.toString()) },
        )
        assertTrue(result!!.hasNext)
        assertEquals("TIPO_FUTURO", result!!.content.single().type)
    }

    @Test
    fun respostaDeCadastroNaoVaiParaOutraSessao() {
        val api =
            testApi<NotifyAPI> {
                TestCall {
                    manager.startSession(jwt("99"), "other-refresh")
                    Response.success(PushInstallationResponse("cadastro", "ACTIVE"))
                }
            }
        var failure: Throwable? = null
        PushInstallationRepository(api, manager, true).registrar(
            "fid",
            { fail("Resposta antiga nao pode ser aplicada") },
            { _, _ -> fail() },
            { failure = it },
        )
        assertTrue(failure is NotifySessionChanged)
    }

    @Test
    fun respostaSemCorpoNaoSePassaPorHistoricoVazio() {
        val api =
            testApi<NotifyAPI> {
                TestCall<SliceResponse<NotificationResponse>> {
                    Response.success<SliceResponse<NotificationResponse>>(
                        204,
                        null,
                    )
                }
            }
        var failure: Throwable? = null
        NotificationRepository(api, manager, true).listar(
            onSuccess = { fail() },
            onError = { _, _ -> fail() },
            onFailure = { failure = it },
        )
        assertTrue(failure is java.net.ProtocolException)
    }
}
