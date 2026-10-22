package com.example.ceris.api

import com.example.ceris.model.dto.RecipeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

/** Receitas salvas da casa do token, na Core. */
interface RecipeAPI {

    @GET("/api/recipes")
    fun salvas(
        @Header("Authorization") autorizacao: String
    ): Call<List<RecipeResponse>>
}
