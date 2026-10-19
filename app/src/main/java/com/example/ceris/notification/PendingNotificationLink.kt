package com.example.ceris.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Guarda somente o destino enquanto o usuario autentica; consome uma unica vez. */
class PendingNotificationLink internal constructor(
    private val preferences: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences("ceris_notification_link", Context.MODE_PRIVATE))

    fun save(link: NotificationLink) =
        preferences.edit {
            putString("uri", link.toUri())
            putLong("created_at", now())
        }

    fun take(): NotificationLink? {
        val uri = preferences.getString("uri", null)
        val age = now() - preferences.getLong("created_at", 0)
        clear()
        return if (age in 0..MAX_AGE_MS) NotificationLink.parse(uri) else null
    }

    fun clear() = preferences.edit { clear() }

    companion object {
        internal const val MAX_AGE_MS = 15 * 60 * 1000L
    }
}
