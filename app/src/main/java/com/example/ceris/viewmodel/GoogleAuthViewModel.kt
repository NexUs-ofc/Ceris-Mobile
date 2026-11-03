package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Address
import com.example.ceris.model.Channel
import com.example.ceris.model.dto.GoogleAuthenticateRequest
import com.example.ceris.model.dto.GoogleAuthenticateResponse
import com.example.ceris.model.dto.GoogleRegistrationRequest
import com.example.ceris.model.dto.GoogleRegistrationRequiredResponse
import com.example.ceris.repository.AuthRepository
import com.example.ceris.util.mensagemDeFalha
import com.google.gson.Gson

class GoogleAuthViewModel : ViewModel() {

    private val authURL = ApiBaseUrl.auth
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
            listener.makeText("Não foi possível concluir o acesso com o Google. Tente novamente.")
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

                    else -> listener.makeText("O acesso não foi concluído. Tente novamente em instantes.")
                }
            },
            onError = { statusCode, errorBody ->
                listener.googleSignInFinished()
                if (statusCode == 428) {
                    val resposta = try {
                        Gson().fromJson(errorBody, GoogleAuthenticateResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    val cadastro = resposta?.registration
                    if (cadastro != null) {
                        this.authRepository.saveGoogleTicket(cadastro.googleTicket)
                        listener.registrationRequired(cadastro)
                    } else {
                        listener.makeText(errorMessageFor(statusCode))
                    }
                } else {
                    listener.makeText(errorMessageFor(statusCode))
                }
            },
            onFailure = { throwable ->
                listener.googleSignInFinished()
                listener.makeText(mensagemDeFalha(throwable))
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
            listener.makeText("Esta etapa do cadastro expirou. Comece novamente pelo Google.")
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
            onError = { statusCode, _ ->
                listener.googleSignInFinished()
                listener.makeText(errorMessageFor(statusCode))
            },
            onFailure = { throwable ->
                listener.googleSignInFinished()
                listener.makeText(mensagemDeFalha(throwable))
            }
        )
    }

    private fun errorMessageFor(statusCode: Int): String {
        return when (statusCode) {
            400 -> "Confira os dados do cadastro e tente novamente."
            401 -> "Não foi possível validar sua conta Google. Tente novamente."
            403 -> "Sua conta não tem acesso ao app. Entre em contato com o suporte."
            409 -> "Já existe uma conta com esse e-mail. Entre com sua senha ou recupere o acesso."
            else -> "Não foi possível concluir o acesso com o Google. Tente novamente em instantes."
        }
    }
}
