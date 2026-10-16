package com.example.ceris.api

import com.example.ceris.model.dto.ChatRequest
import com.example.ceris.model.dto.ChatResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Conversa com a Cerita, no ms-ia.
 *
 * Exige os dois cabecalhos ao mesmo tempo, diferente da Core, que se contenta
 * com o Bearer: o ms-ia pede o Bearer para saber de quem e a conversa e a
 * X-API-Key para saber que a chamada veio do app. Faltando qualquer um dos
 * dois a resposta e 401.
 *
 * A barra no fim de "chat/" nao e enfeite: o router do FastAPI registra a rota
 * com ela, e sem a barra a chamada leva um redirect 307 que o OkHttp repete
 * sem o corpo.
 */
interface ChatAPI {

    @POST("chat/")
    fun enviar(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Body corpo: ChatRequest
    ): Call<ChatResponse>

    /**
     * Encerra a sessao. O servidor aproveita para resumir a conversa e guardar
     * as preferencias que apareceram nela — por isso nao basta esquecer o id
     * aqui no app.
     */
    @DELETE("chat/{session_id}")
    fun encerrar(
        @Header("Authorization") autorizacao: String,
        @Header("X-API-Key") apiKey: String,
        @Path("session_id") sessionId: String
    ): Call<Map<String, Object>>
}
