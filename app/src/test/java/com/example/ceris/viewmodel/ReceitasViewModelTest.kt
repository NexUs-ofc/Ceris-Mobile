package com.example.ceris.viewmodel

import com.example.ceris.api.RecipeAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.RecipeResponse
import com.example.ceris.repository.RecipeRepository
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReceitasViewModelTest {
    private val sessions = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val recipe = RecipeResponse("507f1f77bcf86cd799439011", title = "Frango ao molho")

    @Test
    fun carregarListaVaziaPassaPorCarregamentoEConteudoVazio() {
        val api = testApi<RecipeAPI> { TestCall { Response.success(emptyList<RecipeResponse>()) } }
        val viewModel = ReceitasViewModel(RecipeRepository(api, sessions))
        val states = mutableListOf<ReceitasViewModel.Estado>()
        viewModel.listener = ReceitasViewModel.Listener { states.add(it) }

        viewModel.carregar()

        assertTrue(states[states.lastIndex - 1] is ReceitasViewModel.Estado.Carregando)
        assertTrue((states.last() as ReceitasViewModel.Estado.Conteudo).receitas.isEmpty())
    }

    @Test
    fun retornoDoChatConsultaNovamenteAsReceitasSalvas() {
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                TestCall { Response.success(if (requests == 1) emptyList() else listOf(recipe)) }
            }
        val viewModel = ReceitasViewModel(RecipeRepository(api, sessions))
        var state: ReceitasViewModel.Estado? = null
        viewModel.listener = ReceitasViewModel.Listener { state = it }

        viewModel.carregar()
        viewModel.carregar()

        assertEquals(2, requests)
        assertEquals(recipe.id, (state as ReceitasViewModel.Estado.Conteudo).receitas.single().id)
    }

    @Test
    fun erroNaoApresentaEstadoDeNenhumaReceitaEPermiteTentarNovamente() {
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                TestCall<List<RecipeResponse>> {
                    if (requests == 1) Response.error(503, ResponseBody.create(null, "")) else Response.success(listOf(recipe))
                }
            }
        val viewModel = ReceitasViewModel(RecipeRepository(api, sessions))
        var state: ReceitasViewModel.Estado? = null
        viewModel.listener = ReceitasViewModel.Listener { state = it }

        viewModel.carregar()
        assertTrue(state is ReceitasViewModel.Estado.Erro)
        viewModel.carregar()

        assertTrue(state is ReceitasViewModel.Estado.Conteudo)
    }

    @Test
    fun consultaAnteriorCanceladaNaoSubstituiListaAtualizada() {
        val first = DeferredCall<List<RecipeResponse>>()
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                if (requests == 1) first else TestCall { Response.success(listOf(recipe)) }
            }
        val viewModel = ReceitasViewModel(RecipeRepository(api, sessions))
        var state: ReceitasViewModel.Estado? = null
        viewModel.listener = ReceitasViewModel.Listener { state = it }

        viewModel.carregar()
        viewModel.carregar()
        first.respond(Response.success(emptyList()))

        assertTrue(first.isCanceled)
        assertEquals(recipe.id, (state as ReceitasViewModel.Estado.Conteudo).receitas.single().id)
    }

    @Test
    fun listenerNovoRecebeEstadoAtualSemOutraConsulta() {
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                TestCall { Response.success(listOf(recipe)) }
            }
        val viewModel = ReceitasViewModel(RecipeRepository(api, sessions))
        viewModel.carregar()
        var state: ReceitasViewModel.Estado? = null

        viewModel.listener = ReceitasViewModel.Listener { state = it }

        assertEquals(1, requests)
        assertTrue(state is ReceitasViewModel.Estado.Conteudo)
    }

    @Test
    fun detalheRemovidoExibeMensagemDeReceitaEPermiteNovaTentativa() {
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                TestCall<RecipeResponse> {
                    if (requests == 1) Response.error(404, ResponseBody.create(null, "")) else Response.success(recipe)
                }
            }
        val viewModel = DetalheReceitaViewModel(RecipeRepository(api, sessions))
        var state: DetalheReceitaViewModel.Estado? = null
        viewModel.listener = DetalheReceitaViewModel.Listener { state = it }

        viewModel.carregar(recipe.id)
        assertEquals("Esta receita não está mais disponível.", (state as DetalheReceitaViewModel.Estado.Erro).mensagem)
        viewModel.carregar(recipe.id)

        assertEquals(recipe.id, (state as DetalheReceitaViewModel.Estado.Conteudo).receita.id)
    }

    @Test
    fun detalheCarregadoSobreviveTrocaDeListenerSemRefazerConsulta() {
        var requests = 0
        val api =
            testApi<RecipeAPI> {
                requests++
                TestCall { Response.success(recipe) }
            }
        val viewModel = DetalheReceitaViewModel(RecipeRepository(api, sessions))
        viewModel.carregar(recipe.id)
        viewModel.listener = null
        var state: DetalheReceitaViewModel.Estado? = null
        viewModel.listener = DetalheReceitaViewModel.Listener { state = it }

        viewModel.carregar(recipe.id)

        assertEquals(1, requests)
        assertTrue(state is DetalheReceitaViewModel.Estado.Conteudo)
    }

    @Test
    fun idInvalidoNaoConsultaCore() {
        var consulted = false
        val api =
            testApi<RecipeAPI> {
                consulted = true
                error("Não deveria consultar")
            }
        val viewModel = DetalheReceitaViewModel(RecipeRepository(api, sessions))
        var state: DetalheReceitaViewModel.Estado? = null
        viewModel.listener = DetalheReceitaViewModel.Listener { state = it }

        viewModel.carregar("")

        assertFalse(consulted)
        assertTrue(state is DetalheReceitaViewModel.Estado.Erro)
    }

    private class DeferredCall<T>(private val base: TestCall<T> = TestCall { error("Não deveria executar") }) : Call<T> by base {
        private lateinit var callback: Callback<T>

        override fun enqueue(callback: Callback<T>) {
            this.callback = callback
        }

        fun respond(response: Response<T>) = callback.onResponse(this, response)
    }
}
