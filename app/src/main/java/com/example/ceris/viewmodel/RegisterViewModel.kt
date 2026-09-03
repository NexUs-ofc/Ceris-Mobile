package com.example.ceris.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ApiError
import com.example.ceris.model.dto.PasswordRegisterDTO
import com.example.ceris.model.dto.PasswordRegisterResponse
import com.example.ceris.model.dto.ViaCEPResponse
import com.example.ceris.model.dto.VerifyRegistrationRequest
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

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 72
        private val CEP_REGEX = Regex("^\\d{8}$")
    }

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

        val validationError = validatePersonalData(email, password)
        if (validationError != null) {
            listener.makeText(validationError)
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

        val personalError = validatePersonalData(email, password)
        if (personalError != null) {
            listener.makeText(personalError)
            return false
        }

        if (cep == null || !CEP_REGEX.matches(cep)) {
            listener.makeText("CEP inválido. Digite os 8 números do CEP.")
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

    private fun validatePersonalData(email: String?, password: String?): String? {
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "E-mail inválido."
        }
        if (password == null || password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
            return "A senha deve ter entre $MIN_PASSWORD_LENGTH e $MAX_PASSWORD_LENGTH caracteres."
        }
        return null
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
            onError = { statusCode, errorBody ->
                listener.makeText(errorMessageFor(statusCode, errorBody))
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
            onError = { statusCode, errorBody ->
                val text = when (statusCode) {
                    422 -> "Código inválido ou expirado!"
                    401 -> "Acesso negado!"
                    else -> errorMessageFor(statusCode, errorBody)
                }
                listener.makeText(text)
            },
            onFailure = { throwable ->
                listener.makeText("Um erro inesperado aconteceu: ${throwable.message}")
            }
        )
    }

    private fun errorMessageFor(statusCode: Int, errorBody: String?): String {
        val serverMessage = ApiError.parse(errorBody)?.readableMessage()
        return when (statusCode) {
            400 -> serverMessage?.let { "Dados inválidos:\n$it" }
                ?: "Dados inválidos. Confira os campos e tente novamente."
            401 -> "Acesso negado!"
            409 -> serverMessage ?: "E-mail já cadastrado. Faça login."
            422 -> serverMessage?.let { "Os campos devem ser preenchidos corretamente!\n$it" }
                ?: "Os campos devem ser preenchidos corretamente!"
            else -> "Erro: $statusCode Algo deu errado, tente novamente!"
        }
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
