package com.example.ceris.repository

import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ForgotPasswordRequest
import com.example.ceris.model.dto.ForgotPasswordResponse
import com.example.ceris.model.dto.PasswordLoginRequest
import com.example.ceris.model.dto.PasswordLoginResponse
import com.example.ceris.model.dto.PasswordRegisterRequest
import com.example.ceris.model.dto.PasswordRegisterResponse
import com.example.ceris.model.dto.ResetPasswordRequest
import com.example.ceris.model.SessionAttribute
import com.example.ceris.model.dto.VerifyPasswordResetRequest
import com.example.ceris.model.dto.VerifyPasswordResetResponse
import com.example.ceris.model.dto.VerifyRegistrationRequest
import com.example.ceris.model.dto.VerifyRegistrationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AuthRepository (
    private val api: AuthAPI,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val API_KEY = BuildConfig.API_KEY
        private const val TAG = "AuthRepository"
    }

    private fun <T> readError(response: Response<T>): String? {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        Log.e(TAG, "HTTP ${response.code()} - ${raw ?: "sem corpo de erro"}")
        return raw
    }

    fun registerWithPassword(
        request: PasswordRegisterRequest,
        onSuccess: (PasswordRegisterResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
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
                        onError(response.code(), readError(response))
                    }
                }

                override fun onFailure(call: Call<PasswordRegisterResponse?>, t: Throwable) {
                    Log.e(TAG, "startRegistrationWithPassword falhou", t)
                    onFailure(Exception(t.message))
                }
            })
    }

    fun loginWithPassword(
        request: PasswordLoginRequest,
        onSuccess: (PasswordLoginResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.loginWithPassword(API_KEY, request)
            .enqueue(object : Callback<PasswordLoginResponse> {
                override fun onResponse(
                    call: Call<PasswordLoginResponse>,
                    response: Response<PasswordLoginResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(response.code(), readError(response))
                    }
                }

                override fun onFailure(call: Call<PasswordLoginResponse?>, t: Throwable) {
                    Log.e(TAG, "loginWithPassword falhou", t)
                    onFailure(Exception(t.message))
                }
            })
    }

    fun forgotPassword(
        request: ForgotPasswordRequest,
        onSuccess: (ForgotPasswordResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.forgotPassword(API_KEY, request)
            .enqueue(object : Callback<ForgotPasswordResponse> {
                override fun onResponse(
                    call: Call<ForgotPasswordResponse>,
                    response: Response<ForgotPasswordResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(response.code(), readError(response))
                    }
                }

                override fun onFailure(call: Call<ForgotPasswordResponse?>, t: Throwable) {
                    Log.e(TAG, "forgotPassword falhou", t)
                    onFailure(Exception(t.message))
                }
            })
    }

    fun verifyPasswordReset(
        request: VerifyPasswordResetRequest,
        onSuccess: (VerifyPasswordResetResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.verifyPasswordReset(API_KEY, request)
            .enqueue(object : Callback<VerifyPasswordResetResponse> {
                override fun onResponse(
                    call: Call<VerifyPasswordResetResponse>,
                    response: Response<VerifyPasswordResetResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(response.code(), readError(response))
                    }
                }

                override fun onFailure(call: Call<VerifyPasswordResetResponse?>, t: Throwable) {
                    Log.e(TAG, "verifyPasswordReset falhou", t)
                    onFailure(Exception(t.message))
                }
            })
    }

    fun resetPassword(
        request: ResetPasswordRequest,
        onSuccess: () -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.resetPassword(API_KEY, request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(response.code(), readError(response))
                    }
                }

                override fun onFailure(call: Call<Void?>, t: Throwable) {
                    Log.e(TAG, "resetPassword falhou", t)
                    onFailure(Exception(t.message))
                }
            })
    }

    fun saveResetId(resetId: String) {
        sessionManager.set(
            SessionAttribute(SessionKeys.RESET_ID, resetId)
        )
    }

    fun getResetId(): String {
        return sessionManager.getString(SessionKeys.RESET_ID, "")
    }

    fun saveResetTicket(resetTicket: String) {
        sessionManager.set(
            SessionAttribute(SessionKeys.RESET_TICKET, resetTicket)
        )
    }

    fun getResetTicket(): String {
        return sessionManager.getString(SessionKeys.RESET_TICKET, "")
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
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        api.verifyRegistration(API_KEY, request)
            .enqueue(object : Callback<VerifyRegistrationResponse> {
                override fun onFailure(call: Call<VerifyRegistrationResponse?>, t: Throwable) {
                    Log.e(TAG, "verifyRegistration falhou", t)
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
                        onError(response.code(), readError(response))
                    }
                }
            })
    }

}
