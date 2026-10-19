package com.example.ceris.notification

/** Contrato de dados enviado pelo Notify; nao contem credenciais. */
data class PushMessage(
    val notificationId: String,
    val profileId: Int,
    val type: String,
    val sourceId: String,
    val title: String,
    val message: String,
) {
    val channel: String
        get() =
            when (type) {
                "PRODUCT_EXPIRING" -> NotificationChannels.PRODUTOS
                "UPCOMING_EVENT" -> NotificationChannels.EVENTOS
                else -> NotificationChannels.LOJAS
            }

    companion object {
        private val TYPES = setOf("PRODUCT_EXPIRING", "UPCOMING_EVENT", "NEARBY_STORE", "NEW_NEARBY_STORE")

        fun from(data: Map<String, String>): PushMessage? {
            fun field(
                name: String,
                limit: Int,
            ): String? = data[name]?.takeIf { it.isNotBlank() && it.length <= limit }
            val profile = data["profileId"]?.toIntOrNull()?.takeIf { it > 0 } ?: return null
            return PushMessage(
                field("notificationId", 256) ?: return null,
                profile,
                field("type", 64)?.takeIf { it in TYPES } ?: return null,
                field("sourceId", 256) ?: return null,
                field("title", 256) ?: return null,
                field("message", 4096) ?: return null,
            )
        }
    }
}
