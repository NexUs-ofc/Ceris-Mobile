package com.example.ceris.api

import com.example.ceris.model.dto.ForgotPasswordRequest
import com.example.ceris.model.dto.ForgotPasswordResponse
import com.example.ceris.model.dto.PasswordLoginRequest
import com.example.ceris.model.dto.PasswordLoginResponse
import com.example.ceris.model.dto.PasswordRegisterRequest
import com.example.ceris.model.dto.PasswordRegisterResponse
import com.example.ceris.model.dto.ResetPasswordRequest
import com.example.ceris.model.dto.VerifyPasswordResetRequest
import com.example.ceris.model.dto.VerifyPasswordResetResponse
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

    @POST("/api/auth/password/forgot")
    fun forgotPassword(
        @Header("X-API-Key") apiKey: String,
        @Body req: ForgotPasswordRequest
    ): Call<ForgotPasswordResponse>

    @POST("/api/auth/password/reset/verify")
    fun verifyPasswordReset(
        @Header("X-API-Key") apiKey: String,
        @Body req: VerifyPasswordResetRequest
    ): Call<VerifyPasswordResetResponse>

    @POST("/api/auth/password/reset")
    fun resetPassword(
        @Header("X-API-Key") apiKey: String,
        @Body req: ResetPasswordRequest
    ): Call<Void>


}