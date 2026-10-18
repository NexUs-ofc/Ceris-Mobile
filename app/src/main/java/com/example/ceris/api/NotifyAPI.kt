package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.NotificationResponse
import com.example.ceris.model.dto.PushInstallationResponse
import com.example.ceris.model.dto.RegisterPushInstallationRequest
import com.example.ceris.model.dto.SliceResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

// O Notify resolve o perfil pelo JWT do Auth. Nao recebe X-API-Key.
interface NotifyAPI {
    @PUT("/api/push/installations")
    fun registrarInstalacao(
        @Header("Authorization") autorizacao: String,
        @Body request: RegisterPushInstallationRequest,
        @Tag session: SessionSnapshot? = null,
    ): Call<PushInstallationResponse>

    @DELETE("/api/push/installations/{id}")
    fun desvincularInstalacao(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Tag session: SessionSnapshot? = null,
    ): Call<Void>

    @GET("/api/notifications")
    fun listar(
        @Header("Authorization") autorizacao: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Tag session: SessionSnapshot? = null,
    ): Call<SliceResponse<NotificationResponse>>

    @PATCH("/api/notifications/{id}/read")
    fun marcarComoLida(
        @Header("Authorization") autorizacao: String,
        @Path("id") id: String,
        @Tag session: SessionSnapshot? = null,
    ): Call<Void>
}
