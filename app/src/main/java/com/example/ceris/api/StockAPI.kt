package com.example.ceris.api

import com.example.ceris.model.dto.FoodRegistrationRequest
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.dto.StockItemRequest
import com.example.ceris.model.dto.StockItemResponse
import com.example.ceris.model.dto.StockItemUpdateRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
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

    /**
     * Cadastro pelo codigo de barras: reaproveita o alimento se o GTIN ja
     * existe e cria o item no estoque da casa do token.
     */
    @POST("/api/foods")
    fun registrarPorCodigo(
        @Header("Authorization") autorizacao: String,
        @Body cadastro: FoodRegistrationRequest
    ): Call<StockItemResponse>

    /** Cada chamada cria um lote novo: a Core nao soma com o que ja existe. */
    @POST("/api/stock")
    fun adicionar(
        @Header("Authorization") autorizacao: String,
        @Body item: StockItemRequest
    ): Call<StockItemResponse>

    /** Quantidade nova do lote; a Core nao aceita zero, entao zerar e remover(). */
    @PATCH("/api/stock/{id}")
    fun atualizar(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: Int,
        @Body mudanca: StockItemUpdateRequest
    ): Call<StockItemResponse>

    @DELETE("/api/stock/{id}")
    fun remover(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: Int
    ): Call<Unit>
}
