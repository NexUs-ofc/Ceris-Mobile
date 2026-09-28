package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Address
import com.example.ceris.model.Channel
import com.example.ceris.model.dto.ApiError
import com.example.ceris.model.dto.GoogleAuthenticateRequest
import com.example.ceris.model.dto.GoogleRegistrationRequest
import com.example.ceris.model.dto.GoogleRegistrationRequiredResponse
import com.example.ceris.repository.AuthRepository

class GoogleAuthViewModel : ViewModel() {

    private val authURL = BuildConfig.AUTH_API_BASE_URL
    private val api = RetrofitClient.getApi(authURL, AuthAPI::class.java)
    private lateinit var authRepository: AuthRepository

    interface Listener {
        fun makeText(message: String)
        fun googleSignInStarted()
        fun googleSignInFinished()
        fun loggedIn()
        fun registrationRequired(dados: GoogleRegistrationRequiredResponse)
    }

    lateinit var listener: Listener

    fun init(sessionManager: SessionManager) {
        this.authRepository = AuthRepository(
            api = api,
            sessionManager = sessionManager
        )
    }

    /**
     * Recebe o idToken que a View obteve do Credential Manager e decide o fluxo:
     * conta existente entra direto, conta nova precisa completar o cadastro.
     */
    fun authenticateWithGoogle(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            listener.makeText("Não foi possível obter as credenciais do Google.")
            listener.googleSignInFinished()
            return
        }

        listener.googleSignInStarted()

        val req = GoogleAuthenticateRequest(
            idToken = idToken,
            channel = Channel.MOBILE
        )

        this.authRepository.authenticateWithGoogle(
            request = req,
            onSuccess = { response ->
                listener.googleSignInFinished()

                val sessao = response.session
                val cadastro = response.registration

                when {
                    !response.registrationRequired && sessao != null -> {
                        this.authRepository.saveTokens(sessao.accessToken, sessao.refreshToken)
                        listener.loggedIn()
                    }

                    cadastro != null -> {
                        this.authRepository.saveGoogleTicket(cadastro.googleTicket)
                        listener.registrationRequired(cadastro)
                    }

                    else -> listener.makeText("Resposta inesperada do servidor de autenticação.")
                }
            },
            onError = { statusCode, errorBody ->
                listener.googleSignInFinished()
                listener.makeText(errorMessageFor(statusCode, errorBody))
            },
            onFailure = { throwable ->
                listener.googleSignInFinished()
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )
    }

    /**
     * Completa o cadastro de uma conta nova vinda do Google, usando o ticket
     * guardado na etapa anterior.
     */
    fun completeRegistration(
        address: Address,
        phones: List<String>? = null
    ) {
        val googleTicket = this.authRepository.getGoogleTicket()

        if (googleTicket.isBlank()) {
            listener.makeText("Sessão do Google expirada. Entre novamente.")
            return
        }

        listener.googleSignInStarted()

        val req = GoogleRegistrationRequest(
            googleTicket = googleTicket,
            address = address,
            phones = phones
        )

        this.authRepository.registerWithGoogle(
            request = req,
            onSuccess = { sessao ->
                listener.googleSignInFinished()
                this.authRepository.saveTokens(sessao.accessToken, sessao.refreshToken)
                listener.loggedIn()
            },
            onError = { statusCode, errorBody ->
                listener.googleSignInFinished()
                listener.makeText(errorMessageFor(statusCode, errorBody))
            },
            onFailure = { throwable ->
                listener.googleSignInFinished()
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )
    }

    private fun errorMessageFor(statusCode: Int, errorBody: String?): String {
        val serverMessage = ApiError.parse(errorBody)?.readableMessage()
        return when (statusCode) {
            400 -> serverMessage?.let { "Dados inválidos:\n$it" }
                ?: "Dados inválidos. Confira os campos e tente novamente."
            401 -> "Credenciais do Google inválidas ou expiradas."
            403 -> serverMessage ?: "Acesso negado. Conta inativa ou sem permissão para este canal."
            409 -> serverMessage ?: "Já existe uma conta com esse e-mail. Entre com sua senha."
            else -> "Erro: $statusCode Algo deu errado, tente novamente!"
        }
    }
}
