package com.example.ceris.model.dto

import com.google.gson.Gson

/**
 * Representa o corpo de erro padrão da API de autenticação.
 * Ex.: {"code":"VALIDATION_ERROR","fields":{"address.cep":"must match \"\\d{8}\""}}
 */
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val fields: Map<String, String>? = null
) {
    fun readableMessage(): String? {
        val fieldMessages = fields
            ?.entries
            ?.joinToString("\n") { "${it.key}: ${it.value}" }
            ?.takeIf { it.isNotBlank() }

        return fieldMessages ?: message ?: code
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
