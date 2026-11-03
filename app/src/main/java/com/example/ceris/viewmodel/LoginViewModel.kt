package com.example.ceris.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Channel
import com.example.ceris.model.dto.PasswordLoginRequest
import com.example.ceris.repository.AuthRepository
import com.example.ceris.util.mensagemDeFalha

class LoginViewModel : ViewModel() {
    private val authURL = ApiBaseUrl.auth
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
            onError = { statusCode, _ ->
                listener.makeText(errorMessageFor(statusCode))
            },
            onFailure = { throwable ->
                listener.makeText(mensagemDeFalha(throwable))
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

    private fun errorMessageFor(statusCode: Int): String {
        return when (statusCode) {
            400 -> "Confira o e-mail e a senha e tente novamente."
            401 -> "E-mail ou senha inválidos. Confira os dados e tente novamente."
            403 -> "Sua conta não tem acesso ao app. Entre em contato com o suporte."
            else -> "Não foi possível entrar agora. Tente novamente em instantes."
        }
    }

    fun isAllValid(vararg attributes: Any?): Boolean {
        return  attributes.all {
            it != null && it.toString() != ""
        }
    }

}
