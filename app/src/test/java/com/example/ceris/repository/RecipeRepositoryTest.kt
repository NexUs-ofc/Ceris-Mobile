package com.example.ceris.repository

import com.example.ceris.api.RecipeAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.Receita
import com.example.ceris.model.dto.RecipeResponse
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy

class RecipeRepositoryTest {
    private val sessions = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val recipe = RecipeResponse(id = "507f1f77bcf86cd799439011", title = "Frango ao molho", servingSize = 4)

    @Test
    fun listaUsaBearerESnapshotDaSessaoAtualEPreservaOrdemDoCore() {
        val expectedSession = requireNotNull(sessions.snapshot())
        val older = recipe.copy(id = "older", title = "Sopa")
        val api =
            api { method, args ->
                assertEquals("listar", method)
                assertEquals("Bearer ${expectedSession.accessToken}", args[0])
                assertEquals(expectedSession, args[1])
                TestCall { Response.success(listOf(recipe, older)) }
            }
        var result: List<Receita>? = null

        RecipeRepository(api, sessions).listar({ result = it }, { _, _ -> error("HTTP inesperado") }, { throw it })

        assertEquals(listOf(recipe.id, older.id), result?.map { it.id })
        assertEquals(listOf("Frango ao molho", "Sopa"), result?.map { it.nome })
    }

    @Test
    fun listaVaziaDoCoreEUmSucessoSemReceitas() {
        val api = testApi<RecipeAPI> { TestCall { Response.success(emptyList<RecipeResponse>()) } }
        var result: List<Receita>? = null

        RecipeRepository(api, sessions).listar({ result = it }, { _, _ -> error("HTTP inesperado") }, { throw it })

        assertNotNull(result)
        assertTrue(requireNotNull(result).isEmpty())
    }

    @Test
    fun detalheConsultaIdSelecionadoESessaoCapturada() {
        val expectedSession = requireNotNull(sessions.snapshot())
        val api =
            api { method, args ->
                assertEquals("detalhe", method)
                assertEquals("Bearer ${expectedSession.accessToken}", args[0])
                assertEquals(recipe.id, args[1])
                assertEquals(expectedSession, args[2])
                TestCall { Response.success(recipe) }
            }
        var result: Receita? = null

        RecipeRepository(api, sessions).detalhe(recipe.id, { result = it }, { _, _ -> error("HTTP inesperado") }, { throw it })

        assertEquals(recipe.id, result?.id)
        assertEquals(4, result?.porcoes)
    }

    @Test
    fun semSessaoNaoConsultaCoreNemApresentaListaVazia() {
        sessions.clear()
        val api = testApi<RecipeAPI> { error("Nao pode consultar sem sessao") }
        var success = false
        var failure: Throwable? = null
        val repository = RecipeRepository(api, sessions)

        val call = repository.listar({ success = true }, { _, _ -> error("HTTP inesperado") }, { failure = it })

        assertNull(call)
        assertFalse(success)
        assertNotNull(failure)
        failure = null
        val detail = repository.detalhe(recipe.id, { success = true }, { _, _ -> error("HTTP inesperado") }, { failure = it })
        assertNull(detail)
        assertFalse(success)
        assertNotNull(failure)
    }

    @Test
    fun falhaHttpDaListaNaoEConfundidaComReceitasVazias() {
        val body = "{\"message\":\"Servico indisponivel\"}"
        val api =
            testApi<RecipeAPI> {
                TestCall<List<RecipeResponse>> { Response.error(503, ResponseBody.create(null, body)) }
            }
        var code: Int? = null
        var errorBody: String? = null
        var success = false

        RecipeRepository(api, sessions).listar({ success = true }, { status, error ->
            code = status
            errorBody = error
        }, { throw it })

        assertFalse(success)
        assertEquals(503, code)
        assertEquals(body, errorBody)
    }

    @Test
    fun detalheRemovidoPreservaStatus404() {
        val api =
            testApi<RecipeAPI> {
                TestCall<RecipeResponse> { Response.error(404, ResponseBody.create(null, "Receita nao encontrada")) }
            }
        var code: Int? = null

        RecipeRepository(api, sessions).detalhe(recipe.id, {
            error("Receita removida nao pode abrir")
        }, { status, _ -> code = status }, { throw it })

        assertEquals(404, code)
    }

    @Test
    fun falhaDeRedeEPreservadaParaNovaTentativa() {
        val failure = IOException("Offline")
        val base = TestCall<List<RecipeResponse>> { error("Nao deve executar") }
        val failed =
            object : Call<List<RecipeResponse>> by base {
                override fun enqueue(callback: Callback<List<RecipeResponse>>) {
                    callback.onFailure(this, failure)
                }
            }
        var received: Throwable? = null

        RecipeRepository(
            testApi {
                failed
            },
            sessions,
        ).listar({ error("Sucesso inesperado") }, { _, _ -> error("HTTP inesperado") }, { received = it })

        assertSame(failure, received)
    }

    @Test
    fun corpoAusenteNaoEConfundidoComUmaListaVazia() {
        val api = testApi<RecipeAPI> { TestCall<List<RecipeResponse>> { Response.success(null) } }
        var successful = false
        var failed = false

        RecipeRepository(api, sessions).listar({ successful = true }, { _, _ -> failed = true }, { failed = true })

        assertFalse(successful)
        assertTrue(failed)
    }

    @Test
    fun respostaAntigaNaoEntregaReceitasAposTrocaDeConta() {
        val delayed = DelayedRecipeCall<List<RecipeResponse>>()
        var delivered = false
        RecipeRepository(testApi { delayed }, sessions).listar({ delivered = true }, { _, _ -> }, {})
        sessions.startSession(jwt("99"), "other-refresh")

        delayed.respond(Response.success(listOf(recipe)))

        assertFalse(delivered)
    }

    @Test
    fun logoutDescartaDetalheQueAindaEstavaCarregando() {
        val delayed = DelayedRecipeCall<RecipeResponse>()
        var delivered = false
        RecipeRepository(testApi { delayed }, sessions).detalhe(recipe.id, { delivered = true }, { _, _ -> }, {})
        sessions.clear()

        delayed.respond(Response.success(recipe))

        assertFalse(delivered)
    }

    @Test
    fun cancelarConsultaImpedePublicacaoDaRespostaAtrasada() {
        val delayed = DelayedRecipeCall<List<RecipeResponse>>()
        var delivered = false
        val call =
            RecipeRepository(
                testApi { delayed },
                sessions,
            ).listar({ delivered = true }, { _, _ -> delivered = true }, { delivered = true })
        requireNotNull(call).cancel()

        delayed.respond(Response.success(listOf(recipe)))

        assertTrue(delayed.isCanceled)
        assertFalse(delivered)
    }

    @Test
    fun renovacaoDoTokenNaMesmaSessaoPreservaRespostaEmAndamento() {
        val delayed = DelayedRecipeCall<List<RecipeResponse>>()
        val session: SessionSnapshot = requireNotNull(sessions.snapshot())
        var delivered = false
        RecipeRepository(testApi { delayed }, sessions).listar({ delivered = true }, { _, _ -> error("HTTP inesperado") }, { throw it })
        assertTrue(sessions.renew(session, jwt(signature = "renewed"), "renewed-refresh"))

        delayed.respond(Response.success(listOf(recipe)))

        assertTrue(delivered)
    }

    private fun api(respond: (String, Array<out Any?>) -> Any): RecipeAPI =
        Proxy.newProxyInstance(RecipeAPI::class.java.classLoader, arrayOf(RecipeAPI::class.java)) { _, method, args ->
            respond(method.name, args.orEmpty())
        } as RecipeAPI

    private class DelayedRecipeCall<T> private constructor(
        private val base: TestCall<T>,
    ) : Call<T> by base {
        constructor() : this(TestCall { error("Nao deve executar") })

        private lateinit var callback: Callback<T>

        override fun enqueue(callback: Callback<T>) {
            this.callback = callback
        }

        fun respond(response: Response<T>) {
            callback.onResponse(this, response)
        }
    }
}
