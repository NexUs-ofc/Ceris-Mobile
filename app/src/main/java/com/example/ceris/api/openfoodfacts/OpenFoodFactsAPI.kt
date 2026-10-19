package com.example.ceris.api.openfoodfacts

import com.example.ceris.model.dto.OffBusca
import com.example.ceris.model.dto.OffProdutoResposta
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts: base aberta e gratuita, sem chave. Usada so para a tabela
 * nutricional do detalhe; a Core continua sendo a dona do estoque.
 */
interface OpenFoodFactsAPI {

    @Headers("User-Agent: Ceris-Mobile/1.0")
    @GET("api/v2/search")
    fun buscarPorMarca(
        @Query("brands") marca: String,
        @Query("fields") campos: String = CAMPOS,
        @Query("page_size") tamanho: Int = 30
    ): Call<OffBusca>

    /** Produto pelo codigo de barras. 404 com status 0 quando a base nao tem. */
    @Headers("User-Agent: Ceris-Mobile/1.0")
    @GET("api/v2/product/{gtin}")
    fun produto(
        @Path("gtin") gtin: String,
        @Query("fields") campos: String = CAMPOS_PRODUTO
    ): Call<OffProdutoResposta>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"

        const val CAMPOS = "code,product_name,brands,image_front_url,nutriments"

        const val CAMPOS_PRODUTO = "product_name,brands,product_quantity,product_quantity_unit"
    }
}
