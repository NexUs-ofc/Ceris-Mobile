package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.RecipeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Tag

interface RecipeAPI {
    @GET("/api/recipes")
    fun listar(
        @Header("Authorization") authorization: String,
        @Tag session: SessionSnapshot,
    ): Call<List<RecipeResponse>>

    @GET("/api/recipes/{id}")
    fun detalhe(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Tag session: SessionSnapshot,
    ): Call<RecipeResponse>
}
