package com.example.ceris.api

import com.example.ceris.model.ViaCEPResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCEPAPI {
    @GET("ws/{cep}/json/")
    fun getAddressByCEP(@Path("cep") cep: String): Call<ViaCEPResponse>
}
