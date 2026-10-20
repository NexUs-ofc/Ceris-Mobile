package com.example.ceris.notification

import java.net.URI

/** Um destino interno, sem tokens nem texto privado no endereco. */
@ConsistentCopyVisibility
data class NotificationLink private constructor(
    val profileId: Int,
    val notificationId: String,
    val type: String,
    val sourceId: String,
) {
    enum class Destination { PRODUCT, STORE, EVENT }

    val destination: Destination
        get() =
            when (type) {
                "PRODUCT_EXPIRING" -> Destination.PRODUCT
                "UPCOMING_EVENT" -> Destination.EVENT
                else -> Destination.STORE
            }

    fun belongsTo(profileId: Int?): Boolean = this.profileId == profileId

    fun toUri(): String = "ceris://notification/$profileId/$notificationId?type=$type&sourceId=$sourceId"

    companion object {
        private val identifier = Regex("[A-Za-z0-9_-]{1,256}")
        private val types = setOf("PRODUCT_EXPIRING", "UPCOMING_EVENT", "NEARBY_STORE", "NEW_NEARBY_STORE")

        fun from(message: PushMessage): NotificationLink? =
            create(message.profileId, message.notificationId, message.type, message.sourceId)

        fun parse(value: String?): NotificationLink? =
            runCatching {
                if (value == null || value.length > 1024) return null
                val uri = URI(value)
                if (uri.scheme != "ceris" || uri.rawAuthority != "notification" || uri.rawFragment != null) return null
                val path = uri.rawPath.orEmpty().split('/')
                if (path.size != 3 || path[0].isNotEmpty()) return null
                val profile = path[1].toIntOrNull() ?: return null
                val parts = uri.rawQuery?.split('&') ?: return null
                if (parts.size != 2) return null
                val query = parts.map { it.split('=', limit = 2) }
                if (query.any { it.size != 2 } || query.map { it[0] }.toSet() != setOf("type", "sourceId")) return null
                val fields = query.associate { it[0] to it[1] }
                create(profile, path[2], fields.getValue("type"), fields.getValue("sourceId"))
            }.getOrNull()

        private fun create(
            profile: Int,
            id: String,
            type: String,
            source: String,
        ): NotificationLink? {
            if (profile <= 0 || !identifier.matches(id) || type !in types || !identifier.matches(source)) return null
            if (type != "UPCOMING_EVENT" && (source.toIntOrNull() ?: 0) <= 0) return null
            return NotificationLink(profile, id, type, source)
        }
    }
}
