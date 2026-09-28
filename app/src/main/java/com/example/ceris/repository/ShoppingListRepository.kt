package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.ShoppingListAPI
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.CategoriaItem
import com.example.ceris.model.ItemLista
import com.example.ceris.model.ListaCompra
import com.example.ceris.model.UnidadeMedida
import com.example.ceris.model.dto.ShoppingListItemRequest
import com.example.ceris.model.dto.ShoppingListItemUpdateRequest
import com.example.ceris.model.dto.ShoppingListRequest
import com.example.ceris.model.dto.ShoppingListResponse
import com.example.ceris.util.Moeda
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

// Monta o Bearer a partir da sessao e traduz a resposta para o modelo da tela,
// juntando preco/categoria do ListaLocalStore e derivando progresso e total.
class ShoppingListRepository(
    private val api: ShoppingListAPI,
    private val sessionManager: SessionManager,
    private val localStore: ListaLocalStore
) {

    fun listar(
        onSuccess: (List<ListaCompra>) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.listar(autorizacao).enqueue(
            object : Callback<List<ShoppingListResponse>> {
                override fun onResponse(
                    call: Call<List<ShoppingListResponse>>,
                    response: Response<List<ShoppingListResponse>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body().orEmpty().map { paraCard(it) })
                    } else {
                        onError(response.code(), lerErro(response))
                    }
                }

                override fun onFailure(call: Call<List<ShoppingListResponse>>, t: Throwable) {
                    Log.e(TAG, "listar falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    fun criar(
        titulo: String,
        onSuccess: (ShoppingListResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.criar(autorizacao, ShoppingListRequest(title = titulo))
            .enqueue(respostaCrua("criar", onSuccess, onError, onFailure))
    }

    fun buscar(
        listaId: String,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.buscar(autorizacao, listaId)
            .enqueue(respostaDetalhe("buscar", onSuccess, onError, onFailure))
    }

    fun renomear(
        listaId: String,
        titulo: String,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.renomear(autorizacao, listaId, ShoppingListRequest(title = titulo))
            .enqueue(respostaDetalhe("renomear", onSuccess, onError, onFailure))
    }

    // Os ids vao por parametro porque depois do DELETE nao ha de onde tira-los.
    fun remover(
        listaId: String,
        itemIds: Collection<String>,
        onSuccess: () -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.remover(autorizacao, listaId).enqueue(
            object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        localStore.esquecerTodos(itemIds)
                        onSuccess()
                    } else {
                        onError(response.code(), lerErro(response))
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e(TAG, "remover falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    // O id do item so existe na resposta, entao os extras sao gravados no retorno.
    fun adicionarItem(
        listaId: String,
        idsAnteriores: Set<String>,
        nome: String,
        quantidade: Double,
        unidade: UnidadeMedida,
        categoria: CategoriaItem,
        valor: Double,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        val req = ShoppingListItemRequest(
            name = nome,
            quantity = quantidade,
            unitOfMeasure = unidade.api
        )

        api.adicionarItem(autorizacao, listaId, req).enqueue(
            object : Callback<ShoppingListResponse> {
                override fun onResponse(
                    call: Call<ShoppingListResponse>,
                    response: Response<ShoppingListResponse>
                ) {
                    val corpo = response.body()

                    if (!response.isSuccessful || corpo == null) {
                        onError(response.code(), lerErro(response))
                        return
                    }

                    corpo.arrayList
                        .orEmpty()
                        .firstOrNull { it.id !in idsAnteriores }
                        ?.let { localStore.salvar(it.id, valor, categoria) }

                    onSuccess(paraDetalhe(corpo))
                }

                override fun onFailure(call: Call<ShoppingListResponse>, t: Throwable) {
                    Log.e(TAG, "adicionarItem falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    fun atualizarItem(
        listaId: String,
        itemId: String,
        quantidade: Double? = null,
        marcado: Boolean? = null,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        val req = ShoppingListItemUpdateRequest(quantity = quantidade, checked = marcado)

        api.atualizarItem(autorizacao, listaId, itemId, req)
            .enqueue(respostaDetalhe("atualizarItem", onSuccess, onError, onFailure))
    }

    fun removerItem(
        listaId: String,
        itemId: String,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(SemSessao())

        api.removerItem(autorizacao, listaId, itemId).enqueue(
            object : Callback<ShoppingListResponse> {
                override fun onResponse(
                    call: Call<ShoppingListResponse>,
                    response: Response<ShoppingListResponse>
                ) {
                    val corpo = response.body()

                    if (!response.isSuccessful || corpo == null) {
                        onError(response.code(), lerErro(response))
                        return
                    }

                    localStore.esquecer(itemId)
                    onSuccess(paraDetalhe(corpo))
                }

                override fun onFailure(call: Call<ShoppingListResponse>, t: Throwable) {
                    Log.e(TAG, "removerItem falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    data class Detalhe(
        val id: String,
        val nome: String,
        val itens: List<ItemLista>
    ) {
        val concluidos: Int get() = itens.count { it.marcado }

        val total: Int get() = itens.size

        val progresso: Int
            get() = if (total == 0) 0 else (concluidos * 100.0 / total).roundToInt()

        val valorTotal: Double get() = itens.sumOf { it.valor }
    }

    class SemSessao : IllegalStateException("Sessão expirada. Entre novamente.")

    private fun autorizacao(): String? =
        sessionManager.getString(SessionKeys.ACCESS_TOKEN, "")
            .takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }

    // Categoria sai do eventId: o contrato nao tem campo de categoria.
    private fun paraCard(resposta: ShoppingListResponse): ListaCompra {
        val itens = resposta.arrayList.orEmpty()
        val concluidos = itens.count { it.checked }
        val valor = itens.sumOf { localStore.valorDe(it.id) }

        return ListaCompra(
            id = resposta.id,
            nome = resposta.title,
            quantidadeItens = itens.size,
            categoria = if (resposta.eventId.isNullOrBlank()) "Compras" else "Eventos",
            progresso = if (itens.isEmpty()) 0 else (concluidos * 100.0 / itens.size).roundToInt(),
            valorTotal = Moeda.formatar(valor)
        )
    }

    private fun paraDetalhe(resposta: ShoppingListResponse): Detalhe = Detalhe(
        id = resposta.id,
        nome = resposta.title,
        itens = resposta.arrayList.orEmpty().map { item ->
            ItemLista(
                id = item.id,
                nome = item.name,
                quantidade = item.quantity,
                unidade = UnidadeMedida.porApi(item.unitOfMeasure),
                categoria = localStore.categoriaDe(item.id),
                valor = localStore.valorDe(item.id),
                marcado = item.checked
            )
        }
    )

    private fun respostaDetalhe(
        operacao: String,
        onSuccess: (Detalhe) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : Callback<ShoppingListResponse> {
        override fun onResponse(
            call: Call<ShoppingListResponse>,
            response: Response<ShoppingListResponse>
        ) {
            val corpo = response.body()

            if (response.isSuccessful && corpo != null) {
                onSuccess(paraDetalhe(corpo))
            } else {
                onError(response.code(), lerErro(response))
            }
        }

        override fun onFailure(call: Call<ShoppingListResponse>, t: Throwable) {
            Log.e(TAG, "$operacao falhou", t)
            onFailure(t)
        }
    }

    private fun respostaCrua(
        operacao: String,
        onSuccess: (ShoppingListResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : Callback<ShoppingListResponse> {
        override fun onResponse(
            call: Call<ShoppingListResponse>,
            response: Response<ShoppingListResponse>
        ) {
            val corpo = response.body()

            if (response.isSuccessful && corpo != null) {
                onSuccess(corpo)
            } else {
                onError(response.code(), lerErro(response))
            }
        }

        override fun onFailure(call: Call<ShoppingListResponse>, t: Throwable) {
            Log.e(TAG, "$operacao falhou", t)
            onFailure(t)
        }
    }

    private fun <T> lerErro(response: Response<T>): String? {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        Log.e(TAG, "HTTP ${response.code()} - ${raw ?: "sem corpo de erro"}")
        return raw
    }

    private companion object {
        const val TAG = "ShoppingListRepo"
    }
}
