package com.example.ceris.api

import com.example.ceris.model.dto.StoreResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

/** Mercados da Core. Sem coordenadas, o "perto" e o endereco do perfil do token. */
interface StoreAPI {

    @GET("/api/stores/nearby")
    fun proximos(
        @Header("Authorization") autorizacao: String
    ): Call<List<StoreResponse>>
}
