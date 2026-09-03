package com.example.ceris.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Channel
import com.example.ceris.model.dto.ApiError
import com.example.ceris.model.dto.PasswordLoginRequest
import com.example.ceris.repository.AuthRepository

class LoginViewModel : ViewModel() {
    private val authURL = BuildConfig.AUTH_API_BASE_URL
    private val api = RetrofitClient.getApi(authURL, AuthAPI::class.java)
    private lateinit var authRepository: AuthRepository

    interface Listener {
        fun makeText(message: String)
        fun operationCompleted()
    }
    lateinit var listener: Listener

    fun init(sessionManager: SessionManager) {
        this.authRepository = AuthRepository(
            api = api,
            sessionManager = sessionManager
        )
    }

    fun verifyCredentials(
        email: String?,
        password: String?
    ) {
        if (!isAllValid(email, password)) {
            listener.makeText("Preencha todos os campos!")
            return
        }

        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            listener.makeText(validationError)
            return
        }

        listener.operationCompleted()
    }

    fun loginWithPassword(
        email: String?,
        password: String?
    ) {
        if (!isAllValid(email, password)) {
            listener.makeText("Preencha todos os campos!")
            return
        }

        val validationError = validateCredentials(email, password)
        if (validationError != null) {
            listener.makeText(validationError)
            return
        }

        val req = PasswordLoginRequest(
            email = email!!,
            password = password!!,
            channel = Channel.MOBILE
        )

        this.authRepository.loginWithPassword(
            request = req,
            onSuccess = { response ->
                this.authRepository.saveTokens(response.accessToken, response.refreshToken)
                listener.operationCompleted()
            },
            onError = { statusCode, errorBody ->
                listener.makeText(errorMessageFor(statusCode, errorBody))
            },
            onFailure = { throwable ->
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )
    }

    private fun validateCredentials(email: String?, password: String?): String? {
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "E-mail inválido."
        }
        if (password.isNullOrEmpty()) {
            return "Digite sua senha."
        }
        return null
    }

    private fun errorMessageFor(statusCode: Int, errorBody: String?): String {
        val serverMessage = ApiError.parse(errorBody)?.readableMessage()
        return when (statusCode) {
            400 -> serverMessage?.let { "Dados inválidos:\n$it" }
                ?: "Dados inválidos. Confira os campos e tente novamente."
            401 -> "E-mail ou senha inválidos."
            403 -> serverMessage ?: "Acesso negado. Conta inativa ou sem permissão para este canal."
            else -> "Erro: $statusCode Algo deu errado, tente novamente!"
        }
    }

    fun isAllValid(vararg attributes: Any?): Boolean {
        return  attributes.all {
            it != null && it.toString() != ""
        }
    }

}
