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

sealed interface ResultadoBuscaCodigo {
    data class Encontrado(val produto: ProdutoEncontrado) : ResultadoBuscaCodigo
    object NaoEncontrado : ResultadoBuscaCodigo
    object FalhaConsulta : ResultadoBuscaCodigo
}

/**
 * Codigo de barras: pre-preenche pela base aberta (Open Food Facts) e grava na
 * Core. Sem Android: o token vem de fora, e o teste roda com HTTP de verdade.
 */
class CodigoBarrasRepository(
    private val baseAberta: OpenFoodFactsAPI,
    private val estoque: StockAPI,
    private val autorizacao: () -> String?
) {

    /** Compatibilidade para clientes que só precisam do produto ou null. */
    fun buscar(gtin: String, aoTerminar: (ProdutoEncontrado?) -> Unit) {
        consultarProduto(baseAberta, gtin) { resultado ->
            aoTerminar((resultado as? ResultadoBuscaCodigo.Encontrado)?.produto)
        }
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

    companion object {
        /** Distingue código ausente de falha de rede, sem exigir acesso ao estoque. */
        fun consultarProduto(
            baseAberta: OpenFoodFactsAPI,
            gtin: String,
            aoTerminar: (ResultadoBuscaCodigo) -> Unit
        ) {
            baseAberta.produto(gtin).enqueue(object : Callback<OffProdutoResposta> {
                override fun onResponse(call: Call<OffProdutoResposta>, response: Response<OffProdutoResposta>) {
                    val corpo = response.body()
                    val produto = if (response.isSuccessful && corpo?.status == 1) {
                        CadastroPorCodigo.doOpenFoodFacts(corpo.product)
                    } else null

                    val resultado = when {
                        !response.isSuccessful -> if (response.code() == 404) {
                            ResultadoBuscaCodigo.NaoEncontrado
                        } else {
                            ResultadoBuscaCodigo.FalhaConsulta
                        }
                        corpo?.status != 1 || produto == null || produto.nome.isNullOrBlank() || produto.categoria == null ->
                            ResultadoBuscaCodigo.NaoEncontrado
                        else -> ResultadoBuscaCodigo.Encontrado(requireNotNull(produto))
                    }
                    aoTerminar(resultado)
                }

                override fun onFailure(call: Call<OffProdutoResposta>, t: Throwable) {
                    aoTerminar(ResultadoBuscaCodigo.FalhaConsulta)
                }
            })
        }
    }
}
