package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.StoreResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Tag

interface LocaisAPI {
    @GET("/api/profile")
    fun profile(
        @Header("Authorization") authorization: String,
        @Tag session: SessionSnapshot,
    ): Call<ProfileResponse>

    // Omitir ambas as coordenadas usa o endereco do perfil autenticado.
    @GET("/api/stores/nearby")
    fun nearby(
        @Header("Authorization") authorization: String,
        @Tag session: SessionSnapshot,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radiusKm") radiusKm: Double? = null,
    ): Call<List<StoreResponse>>
}
