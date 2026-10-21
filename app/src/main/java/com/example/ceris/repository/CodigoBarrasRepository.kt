package com.example.ceris.repository

import com.example.ceris.api.StockAPI
import com.example.ceris.api.openfoodfacts.OpenFoodFactsAPI
import com.example.ceris.model.CadastroPorCodigo
import com.example.ceris.model.ProdutoEncontrado
import com.example.ceris.model.dto.FoodRegistrationRequest
import com.example.ceris.model.dto.OffProdutoResposta
import com.example.ceris.model.dto.StockItemResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Codigo de barras: pre-preenche pela base aberta (Open Food Facts) e grava na
 * Core. Sem Android: o token vem de fora, e o teste roda com HTTP de verdade.
 */
class CodigoBarrasRepository(
    private val baseAberta: OpenFoodFactsAPI,
    private val estoque: StockAPI,
    private val autorizacao: () -> String?
) {

    /** Nulo quando a base nao tem o produto ou nao respondeu: a pessoa preenche. */
    fun buscar(gtin: String, aoTerminar: (ProdutoEncontrado?) -> Unit) {
        baseAberta.produto(gtin).enqueue(object : Callback<OffProdutoResposta> {
            override fun onResponse(call: Call<OffProdutoResposta>, response: Response<OffProdutoResposta>) {
                val corpo = response.body()
                aoTerminar(
                    if (response.isSuccessful && corpo?.status == 1) CadastroPorCodigo.doOpenFoodFacts(corpo.product)
                    else null
                )
            }

            override fun onFailure(call: Call<OffProdutoResposta>, t: Throwable) = aoTerminar(null)
        })
    }

    fun registrar(
        cadastro: FoodRegistrationRequest,
        onSuccess: (StockItemResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val token = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        estoque.registrarPorCodigo(token, cadastro).enqueue(object : Callback<StockItemResponse> {
            override fun onResponse(call: Call<StockItemResponse>, response: Response<StockItemResponse>) {
                val salvo = response.body()
                if (response.isSuccessful && salvo != null) {
                    onSuccess(salvo)
                } else {
                    onError(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                }
            }

            override fun onFailure(call: Call<StockItemResponse>, t: Throwable) = onFailure(t)
        })
    }
}
