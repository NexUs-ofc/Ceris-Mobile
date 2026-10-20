package com.example.ceris.api

import com.example.ceris.model.dto.ReceiptScanResponse
import com.example.ceris.model.dto.ReceiptScanStatus
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Leitura de nota fiscal, no ms-ia.
 *
 * Mesmos dois cabecalhos do chat: Bearer para saber de quem e, X-API-Key para
 * saber que veio do app.
 */
interface NotaFiscalAPI {

    /**
     * Leitura numa chamada so. Nota longa passa do tempo limite do cliente e
     * aparecia como "conexao interrompida"; o app usa enviar/consultar.
     */
    @Multipart
    @POST("receipt/scan")
    fun ler(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Part imagem: MultipartBody.Part
    ): Call<ReceiptScanResponse>

    /** Entrega a foto e recebe o pedido na hora; a leitura segue no servidor. */
    @Multipart
    @POST("receipt/scan/async")
    fun enviar(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Part imagem: MultipartBody.Part
    ): Call<ReceiptScanStatus>

    @GET("receipt/scan/{pedido}")
    fun consultar(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Path("pedido") pedido: String
    ): Call<ReceiptScanStatus>
}
