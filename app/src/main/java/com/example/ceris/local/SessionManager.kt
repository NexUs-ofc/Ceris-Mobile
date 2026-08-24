package com.example.ceris.local

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences(
        "ceris_preferences",
        Context.MODE_PRIVATE
    )

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
}