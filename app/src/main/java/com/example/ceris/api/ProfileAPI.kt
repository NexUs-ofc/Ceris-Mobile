package com.example.ceris.api

import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.ProfileUpdateRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH

/** Perfil da casa na Core. Bearer: a Core tira o perfil do subject do JWT. */
interface ProfileAPI {

    @GET("/api/profile")
    fun buscar(
        @Header("Authorization") autorizacao: String
    ): Call<ProfileResponse>

    /** Campo nulo fica como esta; lista de restricoes troca a lista inteira. */
    @PATCH("/api/profile")
    fun atualizar(
        @Header("Authorization") autorizacao: String,
        @Body mudanca: ProfileUpdateRequest
    ): Call<ProfileResponse>
}
