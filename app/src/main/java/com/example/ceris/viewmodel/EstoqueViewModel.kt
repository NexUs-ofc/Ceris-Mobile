package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto
import com.example.ceris.repository.StockRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import java.text.Normalizer

/**
 * Estoque. A tela implementa Listener e so reage a eventos, como nas Listas.
 *
 * Busca e filtro rodam aqui sobre a lista ja carregada: o GET devolve o
 * estoque inteiro da casa, entao filtrar no servidor seria uma ida a rede por
 * letra digitada.
 */
class EstoqueViewModel : ViewModel() {

    sealed class Filtro {
        object Todos : Filtro()

        object Vencendo : Filtro()

        data class Categoria(val nome: String) : Filtro()
    }

    interface Listener {
        /** @param estoqueVazio diferencia "nao tem nada" de "a busca nao achou". */
        fun produtosExibidos(produtos: List<Produto>, estoqueVazio: Boolean)

        /**
         * Categorias que existem no estoque agora, em ordem alfabetica. Os
         * chips sao montados a partir daqui: categoria sem produto nao vira
         * filtro.
         */
        fun categoriasDisponiveis(categorias: List<String>, selecionada: String?)

        fun carregando(carregando: Boolean)

        fun makeText(message: String)
    }

    lateinit var listener: Listener

    // Autenticada: um 401 renova a sessao e repete a chamada.
    private val api = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java)

    private lateinit var repository: StockRepository

    private var todos: List<Produto> = emptyList()
    private var busca = ""
    private var filtro: Filtro = Filtro.Todos

    fun init(sessionManager: SessionManager) {
        repository = StockRepository(api = api, sessionManager = sessionManager)
    }

    fun carregar() {
        listener.carregando(true)

        repository.listar(
            onSuccess = { produtos ->
                listener.carregando(false)
                // Ja vem ordenado do repositorio, onde a validade ainda e data.
                todos = produtos

                val categorias = produtos.mapNotNull { it.categoria }.distinct().sortedBy { normalizar(it) }

                // A categoria escolhida pode ter sumido (o ultimo produto dela
                // foi consumido); filtrar por ela mostraria a tela vazia.
                val atual = filtro
                if (atual is Filtro.Categoria && atual.nome !in categorias) {
                    filtro = Filtro.Todos
                }

                listener.categoriasDisponiveis(categorias, (filtro as? Filtro.Categoria)?.nome)
                exibir()
            },
            onError = { statusCode, errorBody ->
                listener.carregando(false)
                listener.makeText(mensagemDeErro(statusCode, errorBody))
            },
            onFailure = { falha ->
                listener.carregando(false)
                listener.makeText(mensagemDeFalha(falha))
            }
        )
    }

    fun buscar(texto: String) {
        busca = normalizar(texto)
        exibir()
    }

    fun filtrar(novo: Filtro) {
        filtro = novo
        exibir()
    }

    private fun exibir() {
        val visiveis = todos.filter { produto ->
            val passaFiltro = when (filtro) {
                Filtro.Todos -> true
                Filtro.Vencendo -> produto.situacao == SituacaoProduto.VENCENDO ||
                    produto.situacao == SituacaoProduto.VENCIDO
                is Filtro.Categoria -> produto.categoria == (filtro as Filtro.Categoria).nome
            }

            passaFiltro && (busca.isEmpty() || normalizar(produto.nome).contains(busca))
        }

        listener.produtosExibidos(visiveis, estoqueVazio = todos.isEmpty())
    }

    // "acucar" acha "Açúcar": quem busca no celular raramente digita acento.
    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
