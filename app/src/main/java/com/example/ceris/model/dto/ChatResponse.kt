package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val resposta: String,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName(value = "respostas_recomendadas", alternate = ["recommended_responses", "recommendedReplies"])
    val respostasRecomendadas: List<String>? = null
)
