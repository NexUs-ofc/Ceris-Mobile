package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.dto.StockItemResponse
import com.example.ceris.util.unidadeNaTela
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Busca o estoque na Core e traduz para o card da tela. */
class StockRepository(
    private val api: StockAPI,
    private val sessionManager: SessionManager
) {

    fun listar(
        onSuccess: (List<Produto>) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // O SemSessao do ShoppingListRepository e o que o mensagemDeFalha
        // reconhece como "Sessao expirada".
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        // Primeiro o catalogo, de onde sai a categoria de cada item: o estoque
        // da Core so traz o foodId. Se o catalogo falhar, o estoque aparece
        // assim mesmo, so sem os filtros de categoria.
        categorias(autorizacao) { categoriaPorAlimento ->
            listarEstoque(autorizacao, categoriaPorAlimento, onSuccess, onError, onFailure)
        }
    }

    private fun categorias(autorizacao: String, depois: (Map<Int, String>) -> Unit) {
        api.alimentos(autorizacao).enqueue(
            object : Callback<List<FoodResponse>> {
                override fun onResponse(call: Call<List<FoodResponse>>, response: Response<List<FoodResponse>>) {
                    depois(
                        response.body().orEmpty()
                            .mapNotNull { food -> food.categoryName?.takeIf { it.isNotBlank() }?.let { food.id to it } }
                            .toMap()
                    )
                }

                override fun onFailure(call: Call<List<FoodResponse>>, t: Throwable) {
                    Log.w(TAG, "catalogo indisponivel; seguindo sem categorias", t)
                    depois(emptyMap())
                }
            }
        )
    }

    private fun listarEstoque(
        autorizacao: String,
        categoriaPorAlimento: Map<Int, String>,
        onSuccess: (List<Produto>) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.listar(autorizacao).enqueue(
            object : Callback<List<StockItemResponse>> {
                override fun onResponse(
                    call: Call<List<StockItemResponse>>,
                    response: Response<List<StockItemResponse>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(ordenar(response.body().orEmpty(), categoriaPorAlimento))
                    } else {
                        onError(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                    }
                }

                override fun onFailure(call: Call<List<StockItemResponse>>, t: Throwable) {
                    Log.e(TAG, "listar falhou", t)
                    onFailure(t)
                }
            }
        )
    }

    /**
     * Mais urgentes primeiro: vencidos, vencendo, depois o resto, e dentro de
     * cada grupo pela data. Ordena aqui porque e aqui que a validade ainda e
     * data — no card ela ja virou texto.
     */
    private fun ordenar(itens: List<StockItemResponse>, categoriaPorAlimento: Map<Int, String>): List<Produto> =
        itens
            .map { it to it.expiryDate?.let(::lerData) }
            .sortedWith(
                compareBy<Pair<StockItemResponse, Date?>> { ordem(situacao(it.second)) }
                    .thenBy { it.second?.time ?: Long.MAX_VALUE }
                    .thenBy { it.first.foodName.orEmpty().lowercase() }
            )
            .map { (item, validade) -> paraCard(item, validade, item.foodId?.let(categoriaPorAlimento::get)) }

    private fun paraCard(item: StockItemResponse, validade: Date?, categoria: String?) = Produto(
        id = item.id.toString(),
        nome = listOfNotNull(item.foodName, item.productBrand)
            .joinToString(" ")
            .ifBlank { "Produto sem nome" },
        quantidade = item.quantity,
        validade = validade?.let { SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(it) }.orEmpty(),
        situacao = situacao(validade),
        imagemUrl = item.foodImageUrl?.trim()?.takeIf { it.isNotEmpty() },
        unidade = unidadeNaTela(item.unitOfMeasure) ?: "un",
        categoria = categoria,
        marca = item.productBrand?.takeIf { it.isNotBlank() }
    )

    private fun ordem(situacao: SituacaoProduto) = when (situacao) {
        SituacaoProduto.VENCIDO -> 0
        SituacaoProduto.VENCENDO -> 1
        SituacaoProduto.FRESCO -> 2
        SituacaoProduto.SEM_VALIDADE -> 3
    }

    private fun lerData(texto: String): Date? = try {
        // Locale.US: o formato e da API, nao do aparelho.
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(texto)
    } catch (e: ParseException) {
        Log.w(TAG, "validade em formato inesperado: $texto")
        null
    }

    private fun situacao(validade: Date?): SituacaoProduto {
        if (validade == null) {
            return SituacaoProduto.SEM_VALIDADE
        }

        val dias = TimeUnit.MILLISECONDS.toDays(validade.time - inicioDeHoje())

        return when {
            dias < 0 -> SituacaoProduto.VENCIDO
            dias <= DIAS_PARA_VENCENDO -> SituacaoProduto.VENCENDO
            else -> SituacaoProduto.FRESCO
        }
    }

    // A validade vem sem hora; comparar com "agora" faria um produto que vence
    // hoje aparecer como vencido a partir do meio-dia.
    private fun inicioDeHoje(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun autorizacao(): String? =
        sessionManager.getString(SessionKeys.ACCESS_TOKEN, "")
            .takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }

    private companion object {
        const val TAG = "StockRepo"

        /** Ate quantos dias antes do vencimento o selo vira "Vencendo". */
        const val DIAS_PARA_VENCENDO = 3L
    }
}
