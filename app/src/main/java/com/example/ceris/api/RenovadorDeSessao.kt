package com.example.ceris.api

import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.SessionAttribute
import com.example.ceris.model.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Renova a sessão quando o servidor responde 401.
 *
 * O access token do NexUs-Auth vale 15 minutos e o refresh vale 30 dias. O app
 * guardava os dois e só usava o primeiro: passados os 15 minutos, toda chamada
 * voltava 401 e a pessoa via "Sessão expirada. Entre novamente." sem ter saído
 * de lugar nenhum. O refresh ficava parado no aparelho.
 *
 * Como Authenticator e não interceptor: o OkHttp só chama isto quando a
 * resposta foi 401, então o caminho normal não paga nada, e a requisição
 * original é repetida sozinha com o token novo — a tela nem fica sabendo.
 *
 * Roda na thread do OkHttp, nunca na principal, e por isso a renovação aqui
 * pode ser síncrona.
 */
class RenovadorDeSessao(
    private val sessionManager: SessionManager,
    private val api: AuthAPI
) : Authenticator {

    companion object {
        private const val TAG = "RenovadorDeSessao"

        /** Uma tentativa por requisição: se o token novo também tomar 401, para. */
        private const val LIMITE_DE_TENTATIVAS = 1
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (tentativasJaFeitas(response) >= LIMITE_DE_TENTATIVAS) {
            return null
        }

        val refreshToken = sessionManager.getString(SessionKeys.REFRESH_TOKEN, "")

        if (refreshToken.isBlank()) {
            return null
        }

        val sessao = renovar(refreshToken) ?: return null

        // OkHttp 3.x: request() e priorResponse() sao metodos, nao
        // propriedades — o projeto usa o logging-interceptor 3.14.9.
        return response.request()
            .newBuilder()
            .header("Authorization", "Bearer ${sessao.accessToken}")
            .build()
    }

    private fun renovar(refreshToken: String) = try {
        val resposta = api.refreshSession(
            BuildConfig.API_KEY,
            RefreshRequest(refreshToken)
        ).execute()

        val corpo = resposta.body()

        if (resposta.isSuccessful && corpo != null) {
            // O refresh token também é trocado: guardar só o access deixaria a
            // renovação seguinte com um token já gasto.
            sessionManager.set(
                SessionAttribute(SessionKeys.ACCESS_TOKEN, corpo.accessToken),
                SessionAttribute(SessionKeys.REFRESH_TOKEN, corpo.refreshToken)
            )
            corpo
        } else {
            Log.w(TAG, "Renovação recusada com HTTP ${resposta.code()}")
            descartarSessao()
            null
        }
    } catch (e: IOException) {
        // Rede fora não é sessão inválida: os tokens ficam onde estão para a
        // próxima tentativa.
        Log.w(TAG, "Não foi possível renovar a sessão agora", e)
        null
    }

    /**
     * Refresh recusado é fim de sessão de verdade — os 30 dias acabaram ou o
     * token foi revogado. Apagar os dois faz a próxima abertura do app cair no
     * login, em vez de insistir numa sessão que o servidor não aceita mais.
     */
    private fun descartarSessao() {
        sessionManager.remove(SessionKeys.ACCESS_TOKEN)
        sessionManager.remove(SessionKeys.REFRESH_TOKEN)
    }

    private fun tentativasJaFeitas(response: Response): Int {
        var feitas = 0
        var anterior = response.priorResponse()

        while (anterior != null) {
            feitas++
            anterior = anterior.priorResponse()
        }

        return feitas
    }
}
