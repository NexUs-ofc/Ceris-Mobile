package com.example.ceris.repository

import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.api.ChatAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ChatRequest
import com.example.ceris.model.SessionAttribute
import com.example.ceris.model.dto.ChatResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * Monta os cabecalhos da conversa e entrega a resposta crua para a tela.
 *
 * A X-API-Key aqui e a CERISIA_API_KEY, e nao a API_KEY que vai para o Auth e
 * a Core: cada servico tem a sua lista de chaves aceitas, e mandar a da Core
 * para o ms-ia volta 401 "Invalid API key" — que nao tem nada a ver com login.
 */
class ChatRepository(
    private val api: ChatAPI,
    private val sessionManager: SessionManager
) {

    fun enviar(
        mensagem: String,
        sessionId: String?,
        onSuccess: (ChatResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        // Reaproveita o SemSessao do ShoppingListRepository porque e nele que o
        // mensagemDeFalha olha para dizer "Sessao expirada": um tipo novo aqui
        // cairia no ramo generico e a frase viraria "erro inesperado".
        val autorizacao = autorizacao() ?: return onFailure(ShoppingListRepository.SemSessao())

        api.enviar(
            autorizacao = autorizacao,
            apiKey = BuildConfig.CERISIA_API_KEY,
            corpo = ChatRequest(mensagem = mensagem, sessionId = sessionId)
        ).enqueue(
            object : Callback<ChatResponse> {
                override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                    val corpo = response.body()

                    if (response.isSuccessful && corpo != null) {
                        onSuccess(corpo)
                    } else {
                        onError(response.code(), lerErro(response))
                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    Log.e(TAG, "enviar falhou", t)
                    onFailure(if (respostaNaoEraJson(t)) RespostaCortada() else t)
                }
            }
        )
    }

    var sessionId: String?
        get() = sessionManager.getString(SessionKeys.CHAT_SESSION_ID, "").takeIf { it.isNotBlank() }
        set(valor) {
            if (valor == null) {
                sessionManager.remove(SessionKeys.CHAT_SESSION_ID)
            } else {
                sessionManager.set(SessionAttribute(SessionKeys.CHAT_SESSION_ID, valor))
            }
        }

    /**
     * Encerra a conversa que ficou aberta da ultima vez que o app rodou.
     *
     * O Android nao avisa de forma confiavel que o app esta fechando — o
     * processo pode morrer sem rodar nada —, entao a limpeza acontece na
     * abertura seguinte. O efeito e o mesmo: aquela conversa e encerrada e o
     * servidor grava o resumo dela.
     */
    fun encerrarPendente() {
        val pendente = sessionId ?: return

        sessionId = null
        encerrar(pendente) {}
    }

    /** Melhor esforco: se o encerramento falhar, a tela segue em frente. */
    fun encerrar(sessionId: String, aoTerminar: () -> Unit) {
        val autorizacao = autorizacao() ?: return aoTerminar()

        api.encerrar(autorizacao, BuildConfig.CERISIA_API_KEY, sessionId).enqueue(
            object : Callback<Map<String, Object>> {
                override fun onResponse(
                    call: Call<Map<String, Object>>,
                    response: Response<Map<String, Object>>
                ) = aoTerminar()

                override fun onFailure(call: Call<Map<String, Object>>, t: Throwable) {
                    Log.e(TAG, "encerrar falhou", t)
                    aoTerminar()
                }
            }
        )
    }

    /**
     * O corte de 30s do DisCloud chega como pagina HTML com status 200, entao
     * o Gson quebra no primeiro caractere em vez de a chamada dar timeout.
     * Sem isto a pessoa ve "erro inesperado: Use JsonReader.setLenient".
     */
    private fun respostaNaoEraJson(t: Throwable): Boolean =
        t is com.google.gson.JsonParseException ||
            t is com.google.gson.stream.MalformedJsonException ||
            t.cause is com.google.gson.stream.MalformedJsonException

    class RespostaCortada : IOException(
        "A Cerita demorou mais de 30 segundos e o servidor cortou a resposta. " +
            "Tente uma pergunta mais direta."
    )

    private fun autorizacao(): String? =
        sessionManager.getString(SessionKeys.ACCESS_TOKEN, "")
            .takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }

    private fun lerErro(response: Response<ChatResponse>): String? =
        runCatching { response.errorBody()?.string() }.getOrNull()

    private companion object {
        const val TAG = "ChatRepo"
    }
}
