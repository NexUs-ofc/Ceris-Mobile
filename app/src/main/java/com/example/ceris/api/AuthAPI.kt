package com.example.ceris.api

import com.example.ceris.model.dto.PasswordLoginRequest
import com.example.ceris.model.dto.PasswordLoginResponse
import com.example.ceris.model.dto.PasswordRegisterRequest
import com.example.ceris.model.dto.PasswordRegisterResponse
import com.example.ceris.model.dto.VerifyRegistrationRequest
import com.example.ceris.model.dto.VerifyRegistrationResponse
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

    @POST("/api/auth/login/password")
    fun loginWithPassword(
        @Header("X-API-Key") apiKey: String,
        @Body req: PasswordLoginRequest
    ): Call<PasswordLoginResponse>


}