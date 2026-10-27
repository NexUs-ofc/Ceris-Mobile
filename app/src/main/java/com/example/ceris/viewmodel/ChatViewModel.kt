package com.example.ceris.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ChatAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ChatRequest
import com.example.ceris.repository.ChatRepository
import com.example.ceris.model.dto.SectionFeedbackRequest
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha

/**
 * Conversa da Cerita. A View implementa Listener e so reage a eventos, como no
 * ListaComprasViewModel.
 *
 * O session_id e o fio da conversa: vem na primeira resposta e volta em toda
 * mensagem seguinte. Fica no ViewModel, e nao na Activity, para sobreviver a
 * giro de tela sem o assunto reiniciar.
 */
class ChatViewModel : ViewModel() {

    // Autenticada: um 401 renova a sessao e repete a chamada, em vez de
    // derrubar a conversa no meio.
    private val api = RetrofitClient.getApiAutenticada(ApiBaseUrl.ia, ChatAPI::class.java)

    private lateinit var repository: ChatRepository

    /**
     * Guardado no SessionManager, e nao so aqui, como rede de seguranca: se o
     * processo morrer com o chat aberto o onCleared nao roda, e e o id gravado
     * que permite a proxima abertura do app encerrar a conversa orfa.
     */
    private var sessionId: String?
        get() = repository.sessionId
        set(valor) {
            repository.sessionId = valor
        }

    /** Null quando a conversa ainda nao comecou: nao ha o que encerrar. */
    val temSessao: Boolean
        get() = sessionId != null

    val estaAguardando: Boolean
        get() = aguardando

    /** Barra envio em cima de envio: a conversa tem de manter a ordem. */
    private var aguardando = false
    private var encerrando = false

    private val relogio = Handler(Looper.getMainLooper())

    private var passoDaEspera = 0

    interface Listener {
        fun respostaRecebida(texto: String)

        fun aguardando(aguardando: Boolean)

        /** Texto do campo de situacao enquanto a Cerita nao responde. */
        fun progressoDaEspera(texto: String)

        fun makeText(message: String)
    }

    lateinit var listener: Listener

    /**
     * Encerra a conversa no servidor e esquece o fio aqui, para a proxima
     * mensagem comecar do zero.
     */
    fun encerrarSessao(aoTerminar: (String?, Boolean) -> Unit) {
        if (encerrando) return
        val id = sessionId

        if (id == null) {
            return aoTerminar(null, true)
        }

        encerrando = true
        repository.encerrar(id) { sucesso ->
            encerrando = false
            aoTerminar(id, sucesso)
        }
    }

    /** Sends the optional score after the server has already closed the section. */
    fun avaliarSessao(id: String, nota: Int, feedback: String?, aoTerminar: () -> Unit) {
        repository.encerrar(
            sessionId = id,
            avaliacao = SectionFeedbackRequest(
                nota,
                feedback?.trim()?.takeIf(String::isNotEmpty)
            ),
            aoTerminar = { _ -> aoTerminar() }
        )
    }

    fun init(sessionManager: SessionManager) {
        repository = ChatRepository(
            api = api,
            sessionManager = sessionManager
        )
    }

    /**
     * @return true quando a mensagem foi aceita e ja pode aparecer na tela.
     *   Em false a tela nao desenha nada e a razao ja foi avisada.
     */
    fun enviar(texto: String): Boolean {
        val mensagem = texto.trim()

        if (mensagem.isEmpty() || aguardando) {
            return false
        }

        if (mensagem.length > ChatRequest.LIMITE_MENSAGEM) {
            listener.makeText(
                "Mensagem longa demais: ${mensagem.length} de " +
                    "${ChatRequest.LIMITE_MENSAGEM} caracteres."
            )
            return false
        }

        trocarEspera(true)

        repository.enviar(
            mensagem = mensagem,
            sessionId = sessionId,
            onSuccess = { resposta ->
                sessionId = resposta.sessionId ?: sessionId
                trocarEspera(false)
                listener.respostaRecebida(resposta.resposta)
            },
            onError = { statusCode, errorBody ->
                trocarEspera(false)
                listener.makeText(mensagemDeErro(statusCode, errorBody))
            },
            onFailure = { falha ->
                trocarEspera(false)
                listener.makeText(mensagemDeFalha(falha))
            }
        )

        return true
    }

    private fun trocarEspera(esperando: Boolean) {
        aguardando = esperando
        listener.aguardando(esperando)

        relogio.removeCallbacksAndMessages(null)

        if (esperando) {
            passoDaEspera = 0
            relogio.postDelayed(::avancarEspera, PRIMEIRO_AVISO_MS)
        }
    }

    /**
     * Depois de alguns segundos parado, o campo de situacao passa a contar o
     * que esta acontecendo.
     *
     * Nao e barra de progresso de verdade: o servidor nao avisa em que no
     * esta. E uma sequencia fixa, cadenciada para caber no teto de 30s que o
     * DisCloud impoe — passou disso, a resposta e cortada de qualquer jeito.
     */
    private fun avancarEspera() {
        if (!aguardando) {
            return
        }

        listener.progressoDaEspera(AVISOS[passoDaEspera])

        if (passoDaEspera < AVISOS.lastIndex) {
            passoDaEspera += 1
            relogio.postDelayed(::avancarEspera, INTERVALO_AVISO_MS)
        }
    }

    /**
     * Sair da tela encerra a conversa.
     *
     * E aqui, e nao no onDestroy da Activity, porque o onCleared nao roda em
     * mudanca de configuracao: girar o celular manteria a conversa, enquanto
     * no onDestroy ela morreria a cada giro — e isso pareceria a Cerita
     * esquecendo tudo sozinha.
     *
     * Uma conversa por assunto tambem rende resumo melhor do lado do servidor,
     * que e quem aproveita o encerramento para guardar as preferencias.
     */
    override fun onCleared() {
        super.onCleared()
        relogio.removeCallbacksAndMessages(null)

        if (this::repository.isInitialized) {
            encerrarSessao { _, _ -> }
        }
    }

    private companion object {
        const val PRIMEIRO_AVISO_MS = 10_000L
        const val INTERVALO_AVISO_MS = 6_000L

        val AVISOS = listOf(
            "consultando seu estoque…",
            "procurando o que combina…",
            "montando a resposta…",
            "essa é mais pesada, quase lá…"
        )
    }
}
