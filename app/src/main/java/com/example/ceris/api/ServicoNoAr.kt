package com.example.ceris.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ceris.util.mensagemDeFalha
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ping de serviço: "tem alguém atendendo neste endereço?".
 *
 * A pergunta aqui não é se a chamada deu certo, é se o serviço existe. Um 401
 * é resposta: significa que a API está de pé e recusou o pedido. Por isso
 * qualquer código HTTP conta como no ar, e só falha de rede — DNS que não
 * resolve, conexão recusada, servidor que desliga sem responder — conta como
 * fora.
 *
 * Tem cliente próprio, com tempos curtos. O cliente do app espera até 60s na
 * leitura, o que faria a splash ficar um minuto parada num serviço morto; aqui
 * cinco segundos já respondem a pergunta.
 */
object ServicoNoAr {

    private const val TAG = "ServicoNoAr"

    private const val SEGUNDOS = 5L

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(SEGUNDOS, TimeUnit.SECONDS)
            .callTimeout(SEGUNDOS * 2, TimeUnit.SECONDS)
            .build()
    }

    private val principal = Handler(Looper.getMainLooper())

    /**
     * @param resposta recebe `null` quando o serviço respondeu, ou a frase de
     *  erro pronta para a tela quando não respondeu. Sempre na thread da UI.
     */
    fun verificar(baseUrl: String, caminho: String, resposta: (String?) -> Unit) {
        if (!ApiBaseUrl.configurada(baseUrl)) {
            principal.post { resposta("Endereço da API não configurado nesta versão.") }
            return
        }

        val pedido = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/" + caminho.trimStart('/'))
            .get()
            .build()

        cliente.newCall(pedido).enqueue(
            object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                    Log.d(TAG, "${pedido.url()} respondeu ${response.code()}")
                    principal.post { resposta(null) }
                }

                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.w(TAG, "${pedido.url()} nao respondeu", e)
                    principal.post { resposta(mensagemDeFalha(e)) }
                }
            }
        )
    }

    /** Health check da Auth. */
    fun verificarAuth(resposta: (String?) -> Unit) =
        verificar(ApiBaseUrl.auth, "actuator/health", resposta)

    /**
     * Core. Sem token a resposta é 401, e 401 já prova que o serviço está de
     * pé — que é tudo o que o ping precisa saber.
     */
    fun verificarCore(resposta: (String?) -> Unit) =
        verificar(ApiBaseUrl.core, "api/shopping-lists", resposta)
}
