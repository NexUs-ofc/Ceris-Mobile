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
        return sequenceOf(message, detail, detalhe, erro)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
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
