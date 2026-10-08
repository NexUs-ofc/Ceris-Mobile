package com.example.ceris.util

import com.example.ceris.model.dto.ApiError
import com.example.ceris.repository.ShoppingListRepository
import java.io.IOException
import java.net.SocketTimeoutException

// Repetir este `when` em cada ViewModel faria as mensagens divergirem.
fun mensagemDeErro(statusCode: Int, errorBody: String?): String {
    val doServidor = ApiError.parse(errorBody)?.readableMessage()

    return when (statusCode) {
        400 -> doServidor?.let { "Dados inválidos:\n$it" }
            ?: "Dados inválidos. Confira os campos e tente novamente."
        401 -> "Sessão expirada. Entre novamente."
        403 -> doServidor ?: "Você não tem acesso a esta lista."
        404 -> "Essa lista não existe mais."
        else -> doServidor ?: "Erro $statusCode. Algo deu errado, tente novamente!"
    }
}

// SocketTimeout tem frase propria: a Core e serverless e a primeira chamada demora.
fun mensagemDeFalha(throwable: Throwable): String = when (throwable) {
    is ShoppingListRepository.SemSessao -> "Sessão expirada. Entre novamente."
    is SocketTimeoutException -> "O servidor demorou para responder. Tente de novo."
    is IOException -> "Sem conexão. Verifique a internet e tente de novo."
    else -> "Um erro inesperado aconteceu: ${throwable.message}"
}
