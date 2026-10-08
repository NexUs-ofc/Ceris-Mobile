package com.example.ceris.api

import com.example.ceris.model.dto.ShoppingListItemRequest
import com.example.ceris.model.dto.ShoppingListItemUpdateRequest
import com.example.ceris.model.dto.ShoppingListRequest
import com.example.ceris.model.dto.ShoppingListResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Autenticacao e Bearer, nao X-API-Key: a Core tira o dono da lista do subject do JWT.
// As operacoes sobre itens devolvem a lista inteira, entao a tela repinta da resposta.
interface ShoppingListAPI {

    @GET("/api/shopping-lists")
    fun listar(
        @Header("Authorization") autorizacao: String
    ): Call<List<ShoppingListResponse>>

    @POST("/api/shopping-lists")
    fun criar(
        @Header("Authorization") autorizacao: String,
        @Body req: ShoppingListRequest
    ): Call<ShoppingListResponse>

    @GET("/api/shopping-lists/{id}")
    fun buscar(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String
    ): Call<ShoppingListResponse>

    @PUT("/api/shopping-lists/{id}")
    fun renomear(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Body req: ShoppingListRequest
    ): Call<ShoppingListResponse>

    @DELETE("/api/shopping-lists/{id}")
    fun remover(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String
    ): Call<Void>

    @POST("/api/shopping-lists/{id}/items")
    fun adicionarItem(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Body req: ShoppingListItemRequest
    ): Call<ShoppingListResponse>

    @PATCH("/api/shopping-lists/{id}/items/{itemId}")
    fun atualizarItem(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Path("itemId") itemId: String,
        @Body req: ShoppingListItemUpdateRequest
    ): Call<ShoppingListResponse>

    @DELETE("/api/shopping-lists/{id}/items/{itemId}")
    fun removerItem(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Path("itemId") itemId: String
    ): Call<ShoppingListResponse>
}
