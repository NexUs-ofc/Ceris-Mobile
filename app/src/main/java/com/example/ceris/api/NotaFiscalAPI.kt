package com.example.ceris.api

import com.example.ceris.model.dto.ReceiptScanResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Leitura de nota fiscal, no ms-ia.
 *
 * Mesmos dois cabecalhos do chat: Bearer para saber de quem e, X-API-Key para
 * saber que veio do app.
 */
interface NotaFiscalAPI {

    @Multipart
    @POST("receipt/scan")
    fun ler(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Part imagem: MultipartBody.Part
    ): Call<ReceiptScanResponse>
}
