package com.example.ceris.api

import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.dto.StockItemRequest
import com.example.ceris.model.dto.StockItemResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// Bearer, como as listas: a Core tira a casa dona do estoque do subject do JWT.
interface StockAPI {

    @GET("/api/stock")
    fun listar(
        @Header("Authorization") autorizacao: String
    ): Call<List<StockItemResponse>>

    /** Sem busca devolve o catalogo inteiro; com busca, por nome. */
    @GET("/api/stock/foods")
    fun alimentos(
        @Header("Authorization") autorizacao: String,
        @Query("search") busca: String? = null
    ): Call<List<FoodResponse>>

    /** Cada chamada cria um lote novo: a Core nao soma com o que ja existe. */
    @POST("/api/stock")
    fun adicionar(
        @Header("Authorization") autorizacao: String,
        @Body item: StockItemRequest
    ): Call<StockItemResponse>
}
