package com.example.ceris.model.dto

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * O Auth e a Core, em Spring, mandam code/message/fields. O ms-ia, em
 * FastAPI, manda {"detail": "..."} — sem esse campo aqui a mensagem do
 * servidor saía nula e a tela caía no texto genérico do código HTTP.
 *
 * Formato do Auth:
 * Ex.: {"code":"VALIDATION_ERROR","fields":{"address.cep":"must match \"\\d{8}\""}}
 */
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null,
    val detalhe: String? = null,
    val erro: String? = null,
    @SerializedName("mensagem_do_provedor")
    val mensagemDoProvedor: String? = null,
    val fields: Map<String, String>? = null
) {
    fun readableMessage(): String? {
        val fieldMessages = fields
            ?.entries
            ?.joinToString("\n") { "${it.key}: ${it.value}" }
            ?.takeIf { it.isNotBlank() }

        // O ms-ia manda a frase pronta em `detalhe`/`erro` e o texto cru do
        // provedor em `mensagem_do_provedor`. Sem os tres, um 429 de cota do
        // Groq chegava na tela como "Erro 429. Algo deu errado".
        val doProvedor = mensagemDoProvedor?.takeIf { it.isNotBlank() }
        val principal = fieldMessages ?: message ?: detail ?: detalhe ?: erro ?: code

        if (principal == null) {
            return doProvedor
        }

        return if (doProvedor == null) principal else principal + "\n" + doProvedor
    }

    companion object {
        fun parse(raw: String?): ApiError? {
            if (raw.isNullOrBlank()) return null
            return try {
                Gson().fromJson(raw, ApiError::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
