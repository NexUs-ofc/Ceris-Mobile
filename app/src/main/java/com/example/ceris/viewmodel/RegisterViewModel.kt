package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.PasswordRegisterDTO
import com.example.ceris.model.PasswordRegisterResponse
import com.example.ceris.model.ViaCEPResponse
import com.example.ceris.model.VerifyRegistrationRequest
import com.example.ceris.repository.AuthRepository
import com.example.ceris.repository.ViaCEPRepository

class RegisterViewModel: ViewModel() {
    private val authURL = BuildConfig.AUTH_API_BASE_URL
    private val api = RetrofitClient.getApi(authURL, AuthAPI::class.java)
    private val viaCEPURL = BuildConfig.VIA_CEP_URL
    private val viaCEPApi = RetrofitClient.getApi(viaCEPURL, ViaCEPAPI::class.java)
    private var passwordRegisterDTO = PasswordRegisterDTO()
    private lateinit var authRepository: AuthRepository
    private lateinit var viaCEPRepository: ViaCEPRepository

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
        this.viaCEPRepository = ViaCEPRepository(api = viaCEPApi)
    }

    fun verifyPersonalData(
        familyName: String?,
        email: String?,
        password: String?,

    ) {
        if (!isAllValid(familyName, email, password)) {
            listener.makeText("Preencha todos os campos!")
            return
        }

        passwordRegisterDTO.apply {
            this.familyName = familyName
            this.email = email
            this.password = password
        }
        listener.operationCompleted()
    }
    fun setUserRegistrationData(
        familyName: String?,
        email: String?,
        password: String?,
        neighborhood: String?,
        street: String?,
        number: String?,
        cep: String?,
        city: String?,
        state: String?
    ): Boolean {
        if (!isAllValid(familyName, email, password, neighborhood, street, number, cep, city, state)) {
            listener.makeText("Preencha todos os campos!")
            return false
        }

        passwordRegisterDTO.apply {
            this.familyName = familyName
            this.email = email
            this.password = password
            this.neighborhood = neighborhood
            this.street = street
            this.number = number
            this.cep = cep
            this.city = city
            this.state = state
        }
        return true

    }

    fun startRegistrationWithPassword() {
        if (! this.passwordRegisterDTO.isAllNotNull()) {
            listener.makeText("Preencha todas as informações!")
            return
        }
        val req = this.passwordRegisterDTO.toRequest()

        this.authRepository.registerWithPassword(
            request = req,
            onSuccess = { response: PasswordRegisterResponse ->
                this.authRepository.saveRegistrationId(response.registrationId)
                listener.makeText("Cadastro concluído com sucesso: ${response.registrationId}")
                listener.operationCompleted()
            },
            onError = { statusCode ->
                val text = when(statusCode) {
                    422 -> "Os campos devem ser preenchidos corretamente!"
                    401 ->  "Acesso negado!"
                    else -> "Erro: $statusCode Algo deu errado, tente novamente!"
                }
                listener.makeText(text)
            },
            onFailure = { throwable ->
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )

    }

    fun verifyRegistration(otpCode: String?) {
        val registrationId = this.authRepository.getRegistrationId()

        if (!isAllValid(otpCode, registrationId)) {
            listener.makeText("O código deve ser preenchido corretamente")
            return
        }

        val req = VerifyRegistrationRequest(
            otp = otpCode!!,
            registrationId = registrationId
        )

        this.authRepository.verifyRegistration(
            request = req,
            onSucces = { response ->
                this.authRepository.saveTokens(response.accessToken, response.refreshToken)
                listener.operationCompleted()
            },
            onError = { statusCode ->
                val text = when (statusCode) {
                    422 -> "Código inválido ou expirado!"
                    401 -> "Acesso negado!"
                    else -> "Erro: $statusCode Algo deu errado, tente novamente!"
                }
                listener.makeText(text)
            },
            onFailure = { throwable ->
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )
    }
    fun fetchAddressByCEP(
        cep: String,
        onSuccess: (ViaCEPResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viaCEPRepository.getAddressByCEP(
            cep = cep,
            onSuccess = { response ->
                onSuccess(response)
            },
            onError = { statusCode ->
                onError(Exception("Status: $statusCode | CEP não encontrado"))
            },
            onFailure = { throwable ->
                onError(throwable)
            }
        )
    }

    fun isAllValid(vararg attributes: Any?): Boolean {
        return  attributes.all {
            it != null && it.toString() != ""
        }
    }

}