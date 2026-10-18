package com.example.ceris.api

import android.util.Log
import com.example.ceris.BuildConfig
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
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
    private val api: AuthAPI,
    private val avisar: (String) -> Unit = { Log.w(TAG, it) },
) : Authenticator {
    companion object {
        private const val TAG = "RenovadorDeSessao"

        /** Uma tentativa por requisição: se o token novo também tomar 401, para. */
        private const val LIMITE_DE_TENTATIVAS = 1
        private val RENOVACAO_LOCK = Any()
    }

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (tentativasJaFeitas(response) >= LIMITE_DE_TENTATIVAS) {
            return null
        }

        // 401 nem sempre fala do token. O ms-ia recusa a X-API-Key com 401 e
        // `WWW-Authenticate: ApiKey`; renovar ali gasta o refresh, troca os
        // dois tokens por novos e toma 401 de novo pelo mesmo motivo. Só o
        // desafio Bearer é sobre a sessão.
        if (!desafioEhBearer(response)) {
            return null
        }

        val origem = response.request().tag(SessionSnapshot::class.java) ?: return null

        // O lock da renovacao nao bloqueia o logout. O compare-and-set do
        // SessionManager descarta uma resposta que chegue depois dele.
        return synchronized(RENOVACAO_LOCK) {
            val atual = sessionManager.snapshot() ?: return null
            if (atual.generation != origem.generation) return null

            // Outra chamada ja renovou enquanto esta aguardava o lock.
            val token =
                if (response.request().header("Authorization") != "Bearer ${atual.accessToken}") {
                    atual.accessToken
                } else {
                    renovar(atual) ?: return null
                }

            if (!sessionManager.isCurrent(origem)) return null
            response.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .tag(SessionSnapshot::class.java, sessionManager.snapshot())
                .build()
        }
    }

    /** Sem cabecalho de desafio, assume Bearer, como os servicos atuais. */
    private fun desafioEhBearer(response: Response): Boolean {
        val desafio = response.header("WWW-Authenticate") ?: return true

        return desafio.trim().startsWith("Bearer", ignoreCase = true)
    }

    private fun renovar(sessao: SessionSnapshot): String? =
        try {
            val resposta =
                api.refreshSession(
                    BuildConfig.API_KEY,
                    RefreshRequest(sessao.refreshToken),
                ).execute()

            val corpo = resposta.body()

            if (resposta.isSuccessful && corpo != null) {
                if (sessionManager.renew(sessao, corpo.accessToken, corpo.refreshToken)) corpo.accessToken else null
            } else {
                resposta.errorBody()?.close()
                avisar("Renovacao recusada com HTTP ${resposta.code()}")
                // 429, 5xx e erros de configuracao nao comprovam token revogado.
                if (resposta.code() == 401) sessionManager.discard(sessao)
                null
            }
        } catch (e: IOException) {
            // Rede fora não é sessão inválida: os tokens ficam onde estão para a
            // próxima tentativa.
            avisar("Nao foi possivel renovar a sessao agora")
            null
        } catch (e: RuntimeException) {
            avisar("Resposta de renovacao invalida")
            null
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
