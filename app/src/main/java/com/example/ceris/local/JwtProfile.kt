package com.example.ceris.local

import com.google.gson.Gson
import com.google.gson.JsonObject
import okio.ByteString

object JwtProfile {
    /** Apenas identidade local. A assinatura e a autorizacao continuam no servidor. */
    fun fromAccessToken(token: String): Int? =
        runCatching {
            val parts = token.split('.')
            if (parts.size != 3 || parts.any { it.isBlank() }) return null

            val payload = ByteString.decodeBase64(parts[1])?.utf8() ?: return null
            val subject =
                Gson().fromJson(payload, JsonObject::class.java)
                    ?.get("sub")?.asString ?: return null

            subject.toIntOrNull()?.takeIf { it > 0 }
        }.getOrNull()
}
