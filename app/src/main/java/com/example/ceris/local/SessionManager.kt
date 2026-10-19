package com.example.ceris.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.ceris.model.SessionAttribute
import java.util.UUID

class SessionManager internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("ceris_preferences", Context.MODE_PRIVATE),
    )

    companion object {
        // Todas as telas compartilham o mesmo arquivo, mesmo com managers distintos.
        private val SESSION_LOCK = Any()
    }

    fun startSession(
        accessToken: String,
        refreshToken: String,
    ) = synchronized(SESSION_LOCK) {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank())
        val profileId = JwtProfile.fromAccessToken(accessToken)
        preferences.edit {
            putString(SessionKeys.ACCESS_TOKEN, accessToken)
            putString(SessionKeys.REFRESH_TOKEN, refreshToken)
            putString(SessionKeys.SESSION_GENERATION, UUID.randomUUID().toString())
            if (profileId != null) putInt(SessionKeys.PROFILE_ID, profileId) else remove(SessionKeys.PROFILE_ID)
            remove(SessionKeys.CHAT_SESSION_ID)
        }
    }

    fun snapshot(): SessionSnapshot? =
        synchronized(SESSION_LOCK) {
            val access = preferences.getString(SessionKeys.ACCESS_TOKEN, "").orEmpty()
            val refresh = preferences.getString(SessionKeys.REFRESH_TOKEN, "").orEmpty()
            if (access.isBlank() || refresh.isBlank()) return null

            // Migra a sessao de quem ja estava autenticado antes desta versao.
            val generation =
                preferences.getString(SessionKeys.SESSION_GENERATION, null)
                    ?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().also {
                    preferences.edit { putString(SessionKeys.SESSION_GENERATION, it) }
                }
            val profileId = JwtProfile.fromAccessToken(access)
            if (preferences.getInt(SessionKeys.PROFILE_ID, 0) != (profileId ?: 0)) {
                preferences.edit {
                    if (profileId != null) putInt(SessionKeys.PROFILE_ID, profileId) else remove(SessionKeys.PROFILE_ID)
                }
            }
            SessionSnapshot(generation, profileId, access, refresh)
        }

    fun isCurrent(session: SessionSnapshot): Boolean =
        synchronized(SESSION_LOCK) {
            snapshot()?.generation == session.generation
        }

    /** Impede login/logout entre a verificacao e a publicacao de um aviso. */
    fun withCurrentSession(
        session: SessionSnapshot,
        action: () -> Unit,
    ): Boolean =
        synchronized(SESSION_LOCK) {
            if (!isCurrent(session)) return false
            action()
            true
        }

    fun renew(
        session: SessionSnapshot,
        accessToken: String,
        refreshToken: String,
    ): Boolean =
        synchronized(SESSION_LOCK) {
            val current = snapshot() ?: return false
            if (current != session || accessToken.isBlank() || refreshToken.isBlank()) return false
            if (JwtProfile.fromAccessToken(accessToken) != current.profileId) return false

            preferences.edit {
                putString(SessionKeys.ACCESS_TOKEN, accessToken)
                putString(SessionKeys.REFRESH_TOKEN, refreshToken)
            }
            true
        }

    fun discard(session: SessionSnapshot): Boolean =
        synchronized(SESSION_LOCK) {
            if (snapshot() != session) return false
            preferences.edit {
                remove(SessionKeys.ACCESS_TOKEN)
                remove(SessionKeys.REFRESH_TOKEN)
                remove(SessionKeys.PROFILE_ID)
                remove(SessionKeys.SESSION_GENERATION)
                remove(SessionKeys.CHAT_SESSION_ID)
            }
            true
        }

    fun saveRegistrationId(registrationId: String) {
        preferences.edit {
            putString("registrationId", registrationId)
        }
    }

    fun getRegistrationId(): String? {
        return preferences.getString("registrationId", null)
    }

    fun clearRegistrationId() {
        preferences.edit {
            remove("registrationId")
        }
    }

    fun set(vararg attributes: SessionAttribute) {
        preferences.edit {
            attributes.forEach { attribute ->
                when (val value = attribute.value) {
                    is String -> putString(attribute.name, value)
                    is Int -> putInt(attribute.name, value)
                    is Boolean -> putBoolean(attribute.name, value)
                    is Long -> putLong(attribute.name, value)
                    is Float -> putFloat(attribute.name, value)
                    null -> remove(attribute.name)
                }
            }
        }
    }

    fun getString(
        name: String,
        defaultValue: String = "",
    ): String {
        return preferences.getString(name, defaultValue).toString()
    }

    fun getInt(
        name: String,
        defaultValue: Int = 0,
    ): Int {
        return preferences.getInt(name, defaultValue)
    }

    fun getBoolean(
        name: String,
        defaultValue: Boolean = false,
    ): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }

    fun remove(name: String) {
        preferences.edit { remove(name) }
    }

    fun clear() =
        synchronized(SESSION_LOCK) {
            preferences.edit { clear() }
        }
}
