package com.example.ceris.api

import com.example.ceris.model.PasswordRegisterRequest
import com.example.ceris.model.PasswordRegisterResponse
import com.example.ceris.model.VerifyRegistrationRequest
import com.example.ceris.model.VerifyRegistrationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


interface AuthAPI {
    @POST("/api/auth/registrations/password/start")
    fun startRegistrationWithPassword(
        @Header("X-API-Key") apiKey: String,
        @Body req: PasswordRegisterRequest
    ): Call<PasswordRegisterResponse>

    @POST("/api/auth/registrations/verify")
    fun verifyRegistration(
        @Header("X-API-Key") apiKey: String,
        @Body req: VerifyRegistrationRequest
    ): Call<VerifyRegistrationResponse>
}