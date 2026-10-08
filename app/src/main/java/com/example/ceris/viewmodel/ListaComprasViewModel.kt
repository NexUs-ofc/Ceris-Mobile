package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ShoppingListAPI
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.CategoriaItem
import com.example.ceris.model.UnidadeMedida
import com.example.ceris.repository.ShoppingListRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha

// A View implementa Listener e so reage a eventos, como no LoginViewModel.
class ListaComprasViewModel : ViewModel() {

    private val coreURL = BuildConfig.CORE_API_BASE_URL
    private val api = RetrofitClient.getApi(coreURL, ShoppingListAPI::class.java)
    private lateinit var repository: ShoppingListRepository

    private lateinit var listaId: String

    // Guardado para os extras locais: adicionarItem precisa saber quais ids ja
    // existiam, e remover a lista precisa dos ids antes do DELETE.
    private var detalhe: ShoppingListRepository.Detalhe? = null

    interface Listener {
        fun makeText(message: String)

        fun detalheCarregado(detalhe: ShoppingListRepository.Detalhe)

        fun listaRemovida()
    }

    lateinit var listener: Listener

    fun init(listaId: String, sessionManager: SessionManager, localStore: ListaLocalStore) {
        this.listaId = listaId
        this.repository = ShoppingListRepository(
            api = api,
            sessionManager = sessionManager,
            localStore = localStore
        )
    }

    fun carregar() {
        repository.buscar(listaId, ::aoReceber, ::aoErrar, ::aoFalhar)
    }

    fun renomear(nome: String?) {
        val titulo = nome?.trim().orEmpty()

        if (titulo.isEmpty()) {
            listener.makeText("Dê um nome para a lista.")
            return
        }

        repository.renomear(listaId, titulo, ::aoReceber, ::aoErrar, ::aoFalhar)
    }

    fun adicionarItem(
        nome: String?,
        quantidade: Double,
        unidade: UnidadeMedida,
        categoria: CategoriaItem,
        valor: Double
    ) {
        val item = nome?.trim().orEmpty()

        if (item.isEmpty()) {
            listener.makeText("Informe o nome do item.")
            return
        }

        // A Core valida @Positive: zero volta como 400 depois da ida a rede.
        if (quantidade <= 0.0) {
            listener.makeText("A quantidade precisa ser maior que zero.")
            return
        }

        repository.adicionarItem(
            listaId = listaId,
            idsAnteriores = detalhe?.itens?.map { it.id }?.toSet().orEmpty(),
            nome = item,
            quantidade = quantidade,
            unidade = unidade,
            categoria = categoria,
            valor = valor,
            onSuccess = ::aoReceber,
            onError = ::aoErrar,
            onFailure = ::aoFalhar
        )
    }

    fun alternarMarcado(itemId: String, marcado: Boolean) {
        repository.atualizarItem(
            listaId = listaId,
            itemId = itemId,
            marcado = marcado,
            onSuccess = ::aoReceber,
            onError = ::aoErrar,
            onFailure = ::aoFalhar
        )
    }

    fun alterarQuantidade(itemId: String, quantidade: Double) {
        if (quantidade <= 0.0) {
            removerItem(itemId)
            return
        }

        repository.atualizarItem(
            listaId = listaId,
            itemId = itemId,
            quantidade = quantidade,
            onSuccess = ::aoReceber,
            onError = ::aoErrar,
            onFailure = ::aoFalhar
        )
    }

    fun removerItem(itemId: String) {
        repository.removerItem(listaId, itemId, ::aoReceber, ::aoErrar, ::aoFalhar)
    }

    fun removerLista() {
        repository.remover(
            listaId = listaId,
            itemIds = detalhe?.itens?.map { it.id }.orEmpty(),
            onSuccess = { listener.listaRemovida() },
            onError = ::aoErrar,
            onFailure = ::aoFalhar
        )
    }

    private fun aoReceber(novo: ShoppingListRepository.Detalhe) {
        detalhe = novo
        listener.detalheCarregado(novo)
    }

    private fun aoErrar(statusCode: Int, errorBody: String?) {
        listener.makeText(mensagemDeErro(statusCode, errorBody))
    }

    private fun aoFalhar(throwable: Throwable) {
        listener.makeText(mensagemDeFalha(throwable))
    }
}
