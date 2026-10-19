package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.EventNotificationSource
import com.example.ceris.model.dto.StockNotificationSource
import com.example.ceris.model.dto.StoreNotificationSource
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Tag

interface NotificationSourceAPI {
    // O Core nao oferece GET /stock/{id}. A lista e limitada ao perfil autenticado.
    @GET("/api/stock")
    fun stock(
        @Header("Authorization") authorization: String,
        @Tag session: SessionSnapshot,
    ): Call<List<StockNotificationSource>>

    @GET("/api/stores/{id}")
    fun store(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Tag session: SessionSnapshot,
    ): Call<StoreNotificationSource>

    @GET("/api/events/{id}")
    fun event(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Tag session: SessionSnapshot,
    ): Call<EventNotificationSource>
}
