package com.example.ceris.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.api.NotaFiscalAPI
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.dto.ReceiptScanResponse
import com.example.ceris.model.dto.StockItemResponse
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * Leitura da nota (ms-ia) e gravacao no estoque (Core).
 *
 * Sao dois servicos porque o OCR mora no ms-ia, mas o estoque e da Core: a
 * nota lida nao vira estoque sozinha, so o que a pessoa confirma na revisao.
 */
class NotaFiscalRepository(
    private val notaApi: NotaFiscalAPI,
    private val estoqueApi: StockAPI,
    private val sessionManager: SessionManager
) {

    /** Proxy na frente do ms-ia respondendo pagina HTML em vez do JSON da leitura. */
    class LeituraCortada : IOException(
        "O servidor respondeu com uma página de erro em vez da leitura. Tente de novo."
    )

    private val principal = Handler(Looper.getMainLooper())

    private val acompanhamento = AcompanhamentoDeLeitura(
        api = notaApi,
        agendar = { atraso, bloco -> principal.postDelayed(bloco, atraso) }
    )

    /**
     * Le por pedido: a foto sobe, o ms-ia devolve um numero na hora e o app
     * pergunta ate ficar pronta. Nenhuma chamada fica aberta durante o OCR.
     */
    fun ler(
        jpeg: ByteArray,
        onSuccess: (ReceiptScanResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        val corpo = RequestBody.create(MediaType.parse("image/jpeg"), jpeg)
        val parte = MultipartBody.Part.createFormData("image", "nota.jpg", corpo)

        acompanhamento.iniciar(
            autorizacao, BuildConfig.CERISIA_API_KEY, parte,
            onSuccess = onSuccess,
            onError = onError,
            onFailure = { t ->
                Log.e(TAG, "leitura falhou", t)
                onFailure(if (respostaNaoEraJson(t)) LeituraCortada() else t)
            }
        )
    }

    /** A tela saiu: para de perguntar pela leitura em andamento. */
    fun cancelarLeitura() {
        acompanhamento.cancelar()
        busca.cancelar()
    }

    fun catalogo(onSuccess: (List<FoodResponse>) -> Unit, onFailure: (Throwable) -> Unit) {
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        estoqueApi.alimentos(autorizacao).enqueue(
            object : Callback<List<FoodResponse>> {
                override fun onResponse(call: Call<List<FoodResponse>>, response: Response<List<FoodResponse>>) {
                    // Erro da Core nao vira lista vazia: sem catalogo a revisao
                    // abre com o campo "Produto" sem nada, e ninguem entende por que.
                    val corpo = response.body()
                    if (response.isSuccessful && corpo != null) {
                        onSuccess(corpo)
                    } else {
                        Log.e(TAG, "catalogo: HTTP ${response.code()}")
                        onFailure(IOException("catalogo respondeu HTTP ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<List<FoodResponse>>, t: Throwable) {
                    Log.e(TAG, "catalogo falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    private val busca = ProdutosNotaRepository(com.example.ceris.api.openfoodfacts.OpenFoodFactsClient.api)

    fun buscarProdutos(
        nomes: List<String>,
        sucesso: (com.example.ceris.model.dto.FoodLookupResponse) -> Unit,
        falha: (Throwable) -> Unit,
    ) {
        val sessao = sessionManager.snapshot() ?: return falha(ShoppingListRepository.SemSessao())
        busca.buscar(nomes) { resposta ->
            if (sessionManager.isCurrent(sessao)) sucesso(resposta)
            else falha(ShoppingListRepository.SemSessao())
        }
    }

    /** Salva somente a selecao confirmada, em lotes de ate 100 produtos. Sem retry de escrita. */
    fun salvar(itens: List<ItemNota>, aoTerminar: (salvos: Int, falhas: Int) -> Unit) {
        val sessao = sessionManager.snapshot() ?: return aoTerminar(0, itens.size)
        val lotes = itens.chunked(100)
        fun proximo(indice: Int, salvos: Int, falhas: Int) {
            if (indice >= lotes.size) return aoTerminar(salvos, falhas)
            if (!sessionManager.isCurrent(sessao)) {
                return aoTerminar(salvos, falhas + lotes.drop(indice).sumOf { it.size })
            }
            val lote = lotes[indice]
            val cadastros = lote.mapNotNull { item ->
                item.produtoEncontrado?.takeIf { it.valido() }?.cadastro(item.quantidade, item.validade)
            }
            if (cadastros.size != lote.size) {
                proximo(indice + 1, salvos, falhas + lote.size)
                return
            }
            estoqueApi.registrarLote(
                "Bearer ${sessao.accessToken}",
                com.example.ceris.model.dto.FoodBatchRegistrationRequest(cadastros),
                sessao,
            ).enqueue(object : Callback<List<StockItemResponse>> {
                override fun onResponse(call: Call<List<StockItemResponse>>, response: Response<List<StockItemResponse>>) {
                    if (!sessionManager.isCurrent(sessao)) {
                        aoTerminar(salvos, falhas + lotes.drop(indice).sumOf { it.size })
                        return
                    }
                    if (response.isSuccessful && response.body()?.size == lote.size) {
                        proximo(indice + 1, salvos + lote.size, falhas)
                    } else {
                        Log.e(TAG, "salvar lote: HTTP ${response.code()}")
                        aoTerminar(salvos, falhas + lotes.drop(indice).sumOf { it.size })
                    }
                }
                override fun onFailure(call: Call<List<StockItemResponse>>, t: Throwable) {
                    Log.e(TAG, "salvar lote falhou", t)
                    aoTerminar(salvos, falhas + lotes.drop(indice).sumOf { it.size })
                }
            })
        }
        proximo(0, 0, 0)
    }

    private fun respostaNaoEraJson(t: Throwable): Boolean =
        t is JsonParseException || t is MalformedJsonException || t.cause is MalformedJsonException

    private fun autorizacao(): String? =
        sessionManager.getString(SessionKeys.ACCESS_TOKEN, "")
            .takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }

    private companion object {
        const val TAG = "NotaFiscalRepo"
    }
}
