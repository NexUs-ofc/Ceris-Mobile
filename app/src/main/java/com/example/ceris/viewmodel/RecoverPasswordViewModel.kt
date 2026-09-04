package com.example.ceris.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.example.ceris.BuildConfig
import com.example.ceris.api.AuthAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ApiError
import com.example.ceris.model.dto.ForgotPasswordRequest
import com.example.ceris.model.dto.ResetPasswordRequest
import com.example.ceris.model.dto.VerifyPasswordResetRequest
import com.example.ceris.repository.AuthRepository


class RecoverPasswordViewModel : ViewModel() {
    private val authURL = BuildConfig.AUTH_API_BASE_URL
    private val api = RetrofitClient.getApi(authURL, AuthAPI::class.java)
    private lateinit var authRepository: AuthRepository

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 72
        private val OTP_REGEX = Regex("^\\d{6}$")
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
    }

    fun verifyEmail(email: String?) {
        val error = validateEmail(email)
        if (error != null) {
            listener.makeText(error)
            return
        }
        listener.operationCompleted()
    }

    fun verifyOtp(otp: String?) {
        val error = validateOtp(otp)
        if (error != null) {
            listener.makeText(error)
            return
        }
        listener.operationCompleted()
    }

    fun verifyNewPassword(newPassword: String?, confirmPassword: String?) {
        val error = validatePasswords(newPassword, confirmPassword)
        if (error != null) {
            listener.makeText(error)
            return
        }
        listener.operationCompleted()
    }

    fun forgotPassword(email: String?) {
        val error = validateEmail(email)
        if (error != null) {
            listener.makeText(error)
            return
        }

        this.authRepository.forgotPassword(
            request = ForgotPasswordRequest(email = email!!),
            onSuccess = { response ->
                this.authRepository.saveResetId(response.resetId)
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

    fun verifyPasswordReset(otp: String?) {
        val error = validateOtp(otp)
        if (error != null) {
            listener.makeText(error)
            return
        }

        val resetId = this.authRepository.getResetId()
        if (resetId.isEmpty()) {
            listener.makeText("Sessão de recuperação expirada. Solicite um novo código.")
            return
        }

        this.authRepository.verifyPasswordReset(
            request = VerifyPasswordResetRequest(resetId = resetId, otp = otp!!),
            onSuccess = { response ->
                this.authRepository.saveResetTicket(response.resetTicket)
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

    fun resetPassword(newPassword: String?, confirmPassword: String?) {
        val error = validatePasswords(newPassword, confirmPassword)
        if (error != null) {
            listener.makeText(error)
            return
        }

        val resetTicket = this.authRepository.getResetTicket()
        if (resetTicket.isEmpty()) {
            listener.makeText("Sessão de recuperação expirada. Solicite um novo código.")
            return
        }

        this.authRepository.resetPassword(
            request = ResetPasswordRequest(
                resetTicket = resetTicket,
                newPassword = newPassword!!
            ),
            onSuccess = {
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

    private fun validateEmail(email: String?): String? {
        if (email.isNullOrBlank()) return "Preencha o e-mail."
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "E-mail inválido."
        return null
    }

    private fun validateOtp(otp: String?): String? {
        if (otp == null || !OTP_REGEX.matches(otp)) {
            return "Digite os 6 dígitos do código."
        }
        return null
    }

    private fun validatePasswords(newPassword: String?, confirmPassword: String?): String? {
        if (newPassword == null || newPassword.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) {
            return "A senha deve ter entre $MIN_PASSWORD_LENGTH e $MAX_PASSWORD_LENGTH caracteres."
        }
        if (newPassword != confirmPassword) {
            return "As senhas não conferem."
        }
        return null
    }

    private fun errorMessageFor(statusCode: Int, errorBody: String?): String {
        val serverMessage = ApiError.parse(errorBody)?.readableMessage()
        return when (statusCode) {
            400 -> serverMessage?.let { "Dados inválidos:\n$it" }
                ?: "Dados inválidos. Confira os campos e tente novamente."
            401 -> "Código inválido ou expirado. Verifique e tente novamente."
            else -> "Erro: $statusCode Algo deu errado, tente novamente!"
        }
    }
}
