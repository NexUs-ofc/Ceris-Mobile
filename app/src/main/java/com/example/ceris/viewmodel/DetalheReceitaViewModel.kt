package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RecipeAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Receita
import com.example.ceris.repository.RecipeRepository
import com.example.ceris.util.mensagemDeErroReceita
import com.example.ceris.util.mensagemDeFalha
import retrofit2.Call

class DetalheReceitaViewModel(private var repository: RecipeRepository? = null) : ViewModel() {
    sealed interface Estado {
        data object Carregando : Estado

        data class Conteudo(val receita: Receita) : Estado

        data class Erro(val mensagem: String) : Estado
    }

    fun interface Listener {
        fun estadoAtualizado(estado: Estado)
    }

    var listener: Listener? = null
        set(value) {
            field = value
            value?.estadoAtualizado(estado)
        }

    private var estado: Estado = Estado.Carregando
    private var chamada: Call<*>? = null
    private var receitaId: String? = null
    private var solicitacao = 0

    fun init(sessionManager: SessionManager) {
        if (repository == null) {
            repository =
                RecipeRepository(
                    RetrofitClient.getApiAutenticada(ApiBaseUrl.core, RecipeAPI::class.java),
                    sessionManager,
                )
        }
    }

    fun carregar(id: String) {
        if (receitaId == id && estado is Estado.Conteudo) return
        cancelar()
        receitaId = id
        val atual = solicitacao
        atualizar(Estado.Carregando)
        chamada =
            requireNotNull(repository).detalhe(
                id = id,
                onSuccess = { if (atual == solicitacao) atualizar(Estado.Conteudo(it)) },
                onError = { codigo, corpo ->
                    if (atual == solicitacao) atualizar(Estado.Erro(mensagemDeErroReceita(codigo, corpo)))
                },
                onFailure = { if (atual == solicitacao) atualizar(Estado.Erro(mensagemDeFalha(it))) },
            )
    }

    private fun cancelar() {
        solicitacao++
        chamada?.cancel()
        chamada = null
    }

    private fun atualizar(novo: Estado) {
        estado = novo
        listener?.estadoAtualizado(novo)
    }

    override fun onCleared() {
        cancelar()
        listener = null
    }
}
