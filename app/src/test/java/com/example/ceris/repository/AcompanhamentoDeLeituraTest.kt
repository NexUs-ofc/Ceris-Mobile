package com.example.ceris.repository

import com.example.ceris.api.NotaFiscalAPI
import com.example.ceris.model.dto.ReceiptScanResponse
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Acompanhamento contra um servidor HTTP de verdade, que responde como o ms-ia.
 * Intervalo curto (20ms) so para o teste rodar rapido.
 */
class AcompanhamentoDeLeituraTest {

    private lateinit var servidor: HttpServer
    private val agenda = Executors.newSingleThreadScheduledExecutor()

    /** Resposta de cada GET, na ordem; a ultima se repete. */
    private var consultas: List<Pair<Int, String>> = emptyList()
    private var envio: Pair<Int, String> = 202 to """{"pedido_id":"p1","status":"processando"}"""
    private val gets = AtomicInteger()
    private var cabecalhos: Map<String, String> = emptyMap()

    private lateinit var resultado: ReceiptScanResponse
    private var erro: Pair<Int, String?>? = null
    private var falha: Throwable? = null
    private val fim = CountDownLatch(1)

    @Before
    fun subir() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/receipt/scan/async") { troca ->
            cabecalhos = mapOf(
                "Authorization" to troca.requestHeaders.getFirst("Authorization"),
                "X-API-Key" to troca.requestHeaders.getFirst("X-API-Key")
            )
            troca.requestBody.readBytes()
            responder(troca, envio)
        }
        servidor.createContext("/receipt/scan/p1") { troca ->
            val vez = gets.getAndIncrement()
            responder(troca, consultas[minOf(vez, consultas.lastIndex)])
        }
        servidor.start()
    }

    @After
    fun derrubar() {
        servidor.stop(0)
        agenda.shutdownNow()
    }

    private fun responder(troca: HttpExchange, resposta: Pair<Int, String>) {
        val bytes = resposta.second.toByteArray()
        troca.responseHeaders.add("Content-Type", "application/json")
        troca.sendResponseHeaders(resposta.first, bytes.size.toLong())
        troca.responseBody.use { it.write(bytes) }
    }

    private fun acompanhamento(limiteMs: Long = 5_000) = AcompanhamentoDeLeitura(
        api = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${servidor.address.port}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotaFiscalAPI::class.java),
        agendar = { atraso, bloco -> agenda.schedule(bloco, atraso, TimeUnit.MILLISECONDS) },
        intervaloMs = 20,
        limiteMs = limiteMs
    )

    private fun rodar(alvo: AcompanhamentoDeLeitura = acompanhamento(), esperarMs: Long = 5_000): Boolean {
        val parte = MultipartBody.Part.createFormData(
            "image", "nota.jpg", RequestBody.create(MediaType.parse("image/jpeg"), byteArrayOf(1, 2, 3))
        )
        alvo.iniciar(
            "Bearer token", "chave", parte,
            onSuccess = { resultado = it; fim.countDown() },
            onError = { codigo, corpo -> erro = codigo to corpo; fim.countDown() },
            onFailure = { falha = it; fim.countDown() }
        )
        return fim.await(esperarMs, TimeUnit.MILLISECONDS)
    }

    private val pronto = 200 to """{"pedido_id":"p1","status":"pronto","resultado":{"store":"Mercado","items":[{"name":"Leite","quantity":2,"unit":"un","unit_price":5.49}]}}"""
    private val processando = 200 to """{"pedido_id":"p1","status":"processando"}"""

    @Test
    fun consulta_ate_ficar_pronta_e_entrega_a_leitura() {
        consultas = listOf(processando, processando, pronto)

        assertTrue(rodar())

        assertEquals("Mercado", resultado.store)
        assertEquals("Leite", resultado.items.single().name)
        assertEquals(3, gets.get())
        assertEquals(mapOf("Authorization" to "Bearer token", "X-API-Key" to "chave"), cabecalhos)
    }

    @Test
    fun erro_da_leitura_chega_com_a_frase_do_servidor() {
        consultas = listOf(200 to """{"pedido_id":"p1","status":"erro","codigo":422,"detalhe":"A imagem não parece ser uma nota fiscal."}""")

        assertTrue(rodar())

        assertEquals(422, erro!!.first)
        assertEquals("A imagem não parece ser uma nota fiscal.", mensagemDeErro(erro!!.first, erro!!.second))
    }

    @Test
    fun servidor_oscilando_no_meio_nao_perde_a_leitura() {
        consultas = listOf(503 to """{"detail":"reiniciando"}""", 503 to """{"detail":"reiniciando"}""", pronto)

        assertTrue(rodar())

        assertEquals("Mercado", resultado.store)
    }

    @Test
    fun pedido_perdido_pede_para_enviar_de_novo() {
        consultas = listOf(404 to """{"detail":"Leitura não encontrada. Envie a nota de novo."}""")

        assertTrue(rodar())

        assertEquals("Leitura não encontrada. Envie a nota de novo.", mensagemDeErro(404, erro!!.second))
    }

    @Test
    fun passar_do_limite_vira_mensagem_de_demora() {
        consultas = listOf(processando)

        assertTrue(rodar(acompanhamento(limiteMs = 200)))

        assertTrue(falha is AcompanhamentoDeLeitura.LeituraDemorou)
        assertEquals(
            "A leitura está demorando mais que o normal. Tente de novo em instantes.",
            mensagemDeFalha(falha!!).lineSequence().first()
        )
    }

    @Test
    fun cancelar_para_de_consultar_e_nao_entrega_nada() {
        consultas = listOf(processando)
        val alvo = acompanhamento()

        rodar(alvo, esperarMs = 100)
        alvo.cancelar()
        val depois = gets.get()
        Thread.sleep(300)

        assertTrue("ainda consultou depois de cancelar", gets.get() <= depois + 1)
        assertNull(erro)
        assertNull(falha)
    }

    @Test
    fun envio_recusado_nao_cria_consulta() {
        envio = 401 to """{"detail":"Token expirado"}"""

        assertTrue(rodar())

        assertEquals(401, erro!!.first)
        assertEquals(0, gets.get())
    }
}
