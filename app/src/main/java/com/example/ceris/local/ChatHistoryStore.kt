package com.example.ceris.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Transcrição local mais recente; expira 24 horas após a última mensagem. */
class ChatHistoryStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("ceris_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "chat_history_${preferences.getInt(SessionKeys.PROFILE_ID, 0)}"

    fun load(now: Long = System.currentTimeMillis()): List<ChatHistoryMessage> {
        val savedAt = preferences.getLong("${key}_saved_at", 0L)
        if (savedAt == 0L || now - savedAt >= LIFETIME_MS || now < savedAt) {
            clear()
            return emptyList()
        }
        val json = preferences.getString(key, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ChatHistoryMessage>>(json, MESSAGE_LIST_TYPE).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun save(messages: List<ChatHistoryMessage>, now: Long = System.currentTimeMillis()) {
        val transcript = messages.filter { it.type == "user" || it.type == "assistant" }
        if (transcript.isEmpty()) return
        preferences.edit()
            .putString(key, gson.toJson(transcript))
            .putLong("${key}_saved_at", now)
            .apply()
    }

    private fun clear() {
        preferences.edit().remove(key).remove("${key}_saved_at").apply()
    }

    data class ChatHistoryMessage(val type: String, val text: String)

    private companion object {
        const val LIFETIME_MS = 24 * 60 * 60 * 1000L
        val MESSAGE_LIST_TYPE = object : TypeToken<List<ChatHistoryMessage>>() {}.type
    }
}
