package com.example.ceris.local

import android.content.Context
import androidx.core.content.edit
import com.example.ceris.model.SessionAttribute

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

    fun getString(name: String, defaultValue: String = ""): String {
        return preferences.getString(name, defaultValue).toString()
    }

    fun getInt(name: String, defaultValue: Int = 0): Int {
        return preferences.getInt(name, defaultValue)
    }

    fun getBoolean(name: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }

    fun remove(name: String) {
        preferences.edit { remove(name) }
    }

    fun clear() {
        preferences.edit { clear() }
    }
}