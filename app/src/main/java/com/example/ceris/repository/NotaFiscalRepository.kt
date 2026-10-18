package com.example.ceris.repository

import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.api.NotaFiscalAPI
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.dto.ReceiptScanResponse
import com.example.ceris.model.dto.StockItemRequest
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

    /** O corte de 30s do DisCloud chega como pagina HTML com status 200. */
    class LeituraCortada : IOException(
        "A leitura da nota passou de 30 segundos e o servidor cortou. " +
            "Tente com a nota mais perto e bem iluminada."
    )

    fun ler(
        jpeg: ByteArray,
        onSuccess: (ReceiptScanResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        val corpo = RequestBody.create(MediaType.parse("image/jpeg"), jpeg)
        val parte = MultipartBody.Part.createFormData("image", "nota.jpg", corpo)

        notaApi.ler(autorizacao, BuildConfig.CERISIA_API_KEY, parte).enqueue(
            object : Callback<ReceiptScanResponse> {
                override fun onResponse(call: Call<ReceiptScanResponse>, response: Response<ReceiptScanResponse>) {
                    val lida = response.body()

                    if (response.isSuccessful && lida != null) {
                        onSuccess(lida)
                    } else {
                        onError(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                    }
                }

                override fun onFailure(call: Call<ReceiptScanResponse>, t: Throwable) {
                    Log.e(TAG, "leitura falhou", t)
                    onFailure(if (respostaNaoEraJson(t)) LeituraCortada() else t)
                }
            }
        )
    }

    fun catalogo(onSuccess: (List<FoodResponse>) -> Unit, onFailure: (Throwable) -> Unit) {
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        estoqueApi.alimentos(autorizacao).enqueue(
            object : Callback<List<FoodResponse>> {
                override fun onResponse(call: Call<List<FoodResponse>>, response: Response<List<FoodResponse>>) {
                    onSuccess(response.body().orEmpty())
                }

                override fun onFailure(call: Call<List<FoodResponse>>, t: Throwable) {
                    Log.e(TAG, "catalogo falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    /**
     * Grava um por um, em sequencia. A Core nao tem inclusao em lote, e
     * disparar tudo junto faria a contagem de falhas depender da ordem em que
     * as respostas voltam.
     */
    fun salvar(itens: List<ItemNota>, aoTerminar: (salvos: Int, falhas: Int) -> Unit) {
        val autorizacao = autorizacao() ?: return aoTerminar(0, itens.size)

        fun proximo(indice: Int, salvos: Int, falhas: Int) {
            if (indice == itens.size) {
                return aoTerminar(salvos, falhas)
            }

            val item = itens[indice]
            val alimento = item.alimentoId ?: return proximo(indice + 1, salvos, falhas + 1)

            estoqueApi.adicionar(
                autorizacao,
                StockItemRequest(foodId = alimento, quantity = item.quantidade, expiryDate = item.validade)
            ).enqueue(
                object : Callback<StockItemResponse> {
                    override fun onResponse(call: Call<StockItemResponse>, response: Response<StockItemResponse>) {
                        if (response.isSuccessful) {
                            proximo(indice + 1, salvos + 1, falhas)
                        } else {
                            Log.e(TAG, "salvar ${item.nomeNaNota}: HTTP ${response.code()}")
                            proximo(indice + 1, salvos, falhas + 1)
                        }
                    }

                    override fun onFailure(call: Call<StockItemResponse>, t: Throwable) {
                        Log.e(TAG, "salvar ${item.nomeNaNota} falhou", t)
                        proximo(indice + 1, salvos, falhas + 1)
                    }
                }
            )
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
