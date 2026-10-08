package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ShoppingListAPI
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ListaCompra
import com.example.ceris.repository.ShoppingListRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha

// A View implementa Listener e so reage a eventos, como no LoginViewModel.
class ListasViewModel : ViewModel() {

    private val coreURL = BuildConfig.CORE_API_BASE_URL
    private val api = RetrofitClient.getApi(coreURL, ShoppingListAPI::class.java)
    private lateinit var repository: ShoppingListRepository

    interface Listener {
        fun makeText(message: String)

        fun listasCarregadas(listas: List<ListaCompra>)

        fun listaCriada(id: String, nome: String)
    }

    lateinit var listener: Listener

    fun init(sessionManager: SessionManager, localStore: ListaLocalStore) {
        this.repository = ShoppingListRepository(
            api = api,
            sessionManager = sessionManager,
            localStore = localStore
        )
    }

    fun carregar() {
        repository.listar(
            onSuccess = { listas -> listener.listasCarregadas(listas) },
            onError = { statusCode, errorBody ->
                listener.makeText(mensagemDeErro(statusCode, errorBody))
            },
            onFailure = { throwable -> listener.makeText(mensagemDeFalha(throwable)) }
        )
    }

    fun criarLista(nome: String?) {
        val titulo = nome?.trim().orEmpty()

        if (titulo.isEmpty()) {
            listener.makeText("Dê um nome para a lista.")
            return
        }

        repository.criar(
            titulo = titulo,
            onSuccess = { resposta -> listener.listaCriada(resposta.id, resposta.title) },
            onError = { statusCode, errorBody ->
                listener.makeText(mensagemDeErro(statusCode, errorBody))
            },
            onFailure = { throwable -> listener.makeText(mensagemDeFalha(throwable)) }
        )
    }
}
