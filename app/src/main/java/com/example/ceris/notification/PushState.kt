package com.example.ceris.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.ceris.local.SessionSnapshot

class PushState internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    data class Binding(val id: String, val fid: String, val generation: String, val profileId: Int)

    val installationId: String? get() = preferences.getString("fid", null)
    val registrationPending: Boolean get() = preferences.getBoolean("registration_pending", true)
    val permissionRequested: Boolean get() = preferences.getBoolean("permission_requested", false)

    fun requestPermission() = preferences.edit { putBoolean("permission_requested", true) }

    fun requestRegistration() = preferences.edit { putBoolean("registration_pending", true) }

    fun registered(fid: String): Boolean {
        if (!fid.matches(Regex("[A-Za-z0-9_-]{1,256}"))) return false
        preferences.edit {
            putString("fid", fid)
            putBoolean("registration_pending", false)
        }
        return true
    }

    fun binding(): Binding? {
        val id = preferences.getString("binding_id", null) ?: return null
        val fid = preferences.getString("binding_fid", null) ?: return null
        val generation = preferences.getString("binding_generation", null) ?: return null
        val profile = preferences.getInt("binding_profile", 0).takeIf { it > 0 } ?: return null
        return Binding(id, fid, generation, profile)
    }

    fun bind(
        session: SessionSnapshot,
        fid: String,
        id: String,
    ) = preferences.edit {
        putString("binding_id", id)
        putString("binding_fid", fid)
        putString("binding_generation", session.generation)
        putInt("binding_profile", requireNotNull(session.profileId))
    }

    fun wasPublished(
        session: SessionSnapshot,
        id: String,
    ): Boolean = preferences.contains(key(session, id))

    internal fun publicationKeys(): List<String> = preferences.all.keys.filter { it.startsWith(SHOWN) }.map { it.removePrefix(SHOWN) }

    fun published(
        session: SessionSnapshot,
        id: String,
        now: Long = System.currentTimeMillis(),
    ) {
        val previous = preferences.all.filterKeys { it.startsWith(SHOWN) }.toList()
        preferences.edit {
            previous.sortedBy { (it.second as? Long) ?: 0L }.take((previous.size - MAX_SHOWN + 1).coerceAtLeast(0))
                .forEach { remove(it.first) }
            putLong(key(session, id), now)
        }
    }

    fun clearSession(keepGeneration: String? = null) {
        val keepBinding = keepGeneration != null && binding()?.generation == keepGeneration
        val keys =
            preferences.all.keys.filter {
                (it.startsWith("binding_") && !keepBinding) ||
                    (it.startsWith(SHOWN) && (keepGeneration == null || !it.startsWith("$SHOWN$keepGeneration:")))
            }
        preferences.edit { keys.forEach { remove(it) } }
    }

    private fun key(
        session: SessionSnapshot,
        id: String,
    ) = "$SHOWN${session.generation}:${session.profileId}:$id"

    companion object {
        const val FILE = "ceris_push"
        private const val SHOWN = "shown:"
        internal const val MAX_SHOWN = 500
    }
}
