package com.example.ceris.repository

import android.util.Log
import androidx.resourceinspection.annotation.Attribute
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.PasswordRegisterRequest
import com.example.ceris.model.PasswordRegisterResponse
import com.example.ceris.model.SessionAttribute
import com.example.ceris.model.VerifyRegistrationRequest
import com.example.ceris.model.VerifyRegistrationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthRepository (
    private val api: AuthAPI,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val API_KEY = BuildConfig.API_KEY
    }
    fun registerWithPassword(
        request: PasswordRegisterRequest,
        onSuccess: (PasswordRegisterResponse) -> Unit,
        onError: (statusCode: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.startRegistrationWithPassword(API_KEY, request)
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
                    onFailure(Exception(t.message))
                }
            })
    }

    fun saveRegistrationId(registrationId: String) {
        sessionManager.set(
            SessionAttribute(SessionKeys.REGISTRATION_ID, registrationId)
        )
    }
    fun saveTokens(accessToken: String, refreshToken: String) {
        sessionManager.set(
            SessionAttribute(SessionKeys.ACCESS_TOKEN, accessToken),
            SessionAttribute(SessionKeys.REFRESH_TOKEN, refreshToken)
        )
    }

    fun getRegistrationId(): String {
        return sessionManager.getString(SessionKeys.REGISTRATION_ID, "")
    }

    fun verifyRegistration(
        request: VerifyRegistrationRequest,
        onSucces: (VerifyRegistrationResponse) -> Unit,
        onError: (statusCode: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.verifyRegistration(API_KEY, request)
            .enqueue(object : Callback<VerifyRegistrationResponse> {
                override fun onFailure(call: Call<VerifyRegistrationResponse?>, t: Throwable) {
                    onFailure(t)
                }

                override fun onResponse(
                    call: Call<VerifyRegistrationResponse?>,
                    response: Response<VerifyRegistrationResponse?>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            onSucces(it)
                        }
                    } else {

                        onError(response.code())
                    }
                }
            })
    }
}