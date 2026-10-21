package com.example.ceris.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InterruptedIOException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Servidor que recebe o pedido e nao responde, como o ms-ia no meio de uma
 * leitura de nota longa ou de uma receita esperando a cota do Groq.
 *
 * O cliente da IA tem callTimeout e readTimeout iguais (45s); aqui os dois sao
 * de 1s, para o teste rodar rapido com o mesmo comportamento.
 */
class TempoLimiteTest {

    private lateinit var servidor: ServerSocket

    @Before
    fun subir() {
        servidor = ServerSocket(0)
        thread(isDaemon = true) {
            runCatching {
                val conexao = servidor.accept()
                conexao.getInputStream().read(ByteArray(4096))
                Thread.sleep(5_000)
                conexao.close()
            }
        }
    }

    @After
    fun derrubar() = servidor.close()

    private fun chamar(): Throwable {
        val cliente = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .callTimeout(1, TimeUnit.SECONDS)
            .build()

        return runCatching {
            cliente.newCall(Request.Builder().url("http://127.0.0.1:${servidor.localPort}/").build()).execute()
        }.exceptionOrNull() ?: error("a chamada deveria ter falhado")
    }

    @Test
    fun estourar_o_tempo_vira_mensagem_de_demora() {
        val falha = chamar()

        // O OkHttp lanca InterruptedIOException("timeout") no callTimeout, que
        // nao e SocketTimeoutException.
        assertTrue("veio ${falha.javaClass.name}", falha is InterruptedIOException)

        assertEquals(
            "O servidor demorou para responder. Tente de novo.",
            mensagemDeFalha(falha).lineSequence().first()
        )
    }
}
