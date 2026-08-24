package com.example.ceris.repository

import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.PasswordRegisterRequest
import com.example.ceris.model.PasswordRegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthRepository (
    private val api: AuthAPI,
    private val sessionManager: SessionManager
) {
    private val apiKey = BuildConfig.API_KEY
    fun registerWithPassword(
        request: PasswordRegisterRequest,
        onSuccess: (PasswordRegisterResponse) -> Unit,
        onError: (statusCode: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.startRegistrationWithPassword(apiKey, request)
            .enqueue(object: Callback<PasswordRegisterResponse> {
                override fun onResponse(
                    call: Call<PasswordRegisterResponse>,
                    response: Response<PasswordRegisterResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(response.code())
                    }
                }

                override fun onFailure(call: Call<PasswordRegisterResponse?>, t: Throwable) {
                    onFailure(Exception("Algo inesperado aconteceu!"))
                }
            })
    }

    fun saveRegistrationId(registrationId: String) {
        sessionManager.saveRegistrationId(registrationId)
    }
}