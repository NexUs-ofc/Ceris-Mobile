package com.example.ceris.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.PasswordRegisterDTO
import com.example.ceris.model.dto.PasswordRegisterResponse
import com.example.ceris.model.dto.ViaCEPResponse
import com.example.ceris.model.dto.VerifyRegistrationRequest
import com.example.ceris.repository.AuthRepository
import com.example.ceris.repository.ViaCEPRepository
import com.example.ceris.util.mensagemDeFalha

class RegisterViewModel: ViewModel() {
    private val authURL = ApiBaseUrl.auth
    private val api = RetrofitClient.getApi(authURL, AuthAPI::class.java)
    private val viaCEPURL = ApiBaseUrl.viaCep
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
            listener.makeText("Preencha todos os campos para continuar.")
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
        val campos = mutableListOf<Any?>(familyName, email, neighborhood, street, number, cep, city, state)
        if (password.isNullOrBlank()) {
            // Fluxo Google: senha nao e obrigatoria
        } else {
            campos.add(password)
        }
        if (!isAllValid(*campos.toTypedArray())) {
            listener.makeText("Preencha todos os campos para continuar.")
            return false
        }

        val personalError = validatePersonalData(email, password)
        if (personalError != null) {
            listener.makeText(personalError)
            return false
        }

        if (cep == null || !CEP_REGEX.matches(cep)) {
            listener.makeText("Informe um CEP válido com 8 números.")
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
        if (password != null && password.isNotBlank() && (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH)) {
            return "A senha deve ter entre $MIN_PASSWORD_LENGTH e $MAX_PASSWORD_LENGTH caracteres."
        }
        return null
    }

    fun startRegistrationWithPassword() {
        if (! this.passwordRegisterDTO.isAllNotNull()) {
            listener.makeText("Confira os dados do cadastro e tente novamente.")
            return
        }
        val req = this.passwordRegisterDTO.toRequest()

        this.authRepository.registerWithPassword(
            request = req,
            onSuccess = { response: PasswordRegisterResponse ->
                this.authRepository.saveRegistrationId(response.registrationId)
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

    fun verifyRegistration(otpCode: String?) {
        val registrationId = this.authRepository.getRegistrationId()

        if (!isAllValid(otpCode, registrationId)) {
            listener.makeText("Digite os 6 números do código de verificação.")
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
            onError = { statusCode, _ ->
                val text = when (statusCode) {
                    422 -> "Este código é inválido ou expirou. Confira e tente novamente."
                    401 -> "Não foi possível validar o código. Solicite outro e tente novamente."
                    else -> errorMessageFor(statusCode)
                }
                listener.makeText(text)
            },
            onFailure = { throwable ->
                listener.makeText(mensagemDeFalha(throwable))
            }
        )
    }

    private fun errorMessageFor(statusCode: Int): String {
        return when (statusCode) {
            400, 422 -> "Confira os dados do cadastro e tente novamente."
            401 -> "Não foi possível validar seus dados. Tente novamente."
            409 -> "Este e-mail já está cadastrado. Entre na sua conta ou recupere a senha."
            else -> "Não foi possível concluir o cadastro. Tente novamente em instantes."
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
