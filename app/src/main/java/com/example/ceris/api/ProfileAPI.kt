package com.example.ceris.api

import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.ProfileUpdateRequest
import com.example.ceris.model.dto.ProfileImageResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PUT

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

    @Multipart
    @PUT("/api/profile/image")
    fun enviarFoto(
        @Header("Authorization") autorizacao: String,
        @Part arquivo: MultipartBody.Part
    ): Call<ProfileImageResponse>
}
