package com.example.ceris.support

import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/** SharedPreferences em memoria para testar sessao sem aparelho ou rede. */
class MemoryPreferences : SharedPreferences {
    private val values = ConcurrentHashMap<String, Any>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(
        key: String?,
        defValue: String?,
    ): String? = values[key] as? String ?: defValue

    override fun getInt(
        key: String?,
        defValue: Int,
    ): Int = values[key] as? Int ?: defValue

    override fun getLong(
        key: String?,
        defValue: Long,
    ): Long = values[key] as? Long ?: defValue

    override fun getFloat(
        key: String?,
        defValue: Float,
    ): Float = values[key] as? Float ?: defValue

    override fun getBoolean(
        key: String?,
        defValue: Boolean,
    ): Boolean = values[key] as? Boolean ?: defValue

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? = defValues

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor =
        object : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private var clear = false

            private fun put(
                key: String?,
                value: Any?,
            ): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }

            override fun putString(
                key: String?,
                value: String?,
            ): SharedPreferences.Editor = put(key, value)

            override fun putInt(
                key: String?,
                value: Int,
            ): SharedPreferences.Editor = put(key, value)

            override fun putLong(
                key: String?,
                value: Long,
            ): SharedPreferences.Editor = put(key, value)

            override fun putFloat(
                key: String?,
                value: Float,
            ): SharedPreferences.Editor = put(key, value)

            override fun putBoolean(
                key: String?,
                value: Boolean,
            ): SharedPreferences.Editor = put(key, value)

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?,
            ): SharedPreferences.Editor = put(key, values)

            override fun remove(key: String?): SharedPreferences.Editor = put(key, null)

            override fun clear(): SharedPreferences.Editor = apply { clear = true }

            override fun commit(): Boolean {
                synchronized(values) {
                    if (clear) values.clear()
                    updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }
}
