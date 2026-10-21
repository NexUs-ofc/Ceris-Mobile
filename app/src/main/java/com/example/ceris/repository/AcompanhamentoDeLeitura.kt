package com.example.ceris.repository

import com.example.ceris.api.NotaFiscalAPI
import com.example.ceris.model.dto.ReceiptScanResponse
import com.example.ceris.model.dto.ReceiptScanStatus
import com.google.gson.Gson
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * Leitura por pedido: entrega a foto, recebe um numero na hora e pergunta ao
 * ms-ia ate a leitura ficar pronta.
 *
 * Existe porque a leitura numa chamada so segurava a conexao aberta enquanto o
 * OCR rodava (44s medidos), e o cliente desistia em 45s mostrando "A conexao
 * foi interrompida pelo servidor". Aqui cada chamada volta em menos de 1s.
 *
 * Sem Android de proposito: quem agenda a proxima consulta vem de fora, e o
 * teste roda com HTTP de verdade na JVM.
 */
class AcompanhamentoDeLeitura(
    private val api: NotaFiscalAPI,
    private val agendar: (atrasoMs: Long, bloco: () -> Unit) -> Unit,
    private val agora: () -> Long = System::currentTimeMillis,
    private val intervaloMs: Long = INTERVALO_MS,
    private val limiteMs: Long = LIMITE_MS
) {

    /** Passou do limite sem ficar pronta. */
    class LeituraDemorou : IOException(
        "A leitura está demorando mais que o normal. Tente de novo em instantes."
    )

    @Volatile
    private var cancelado = false

    /** A tela fechou: para de perguntar e nao entrega mais nada. */
    fun cancelar() {
        cancelado = true
    }

    fun iniciar(
        autorizacao: String,
        apiKey: String,
        imagem: MultipartBody.Part,
        onSuccess: (ReceiptScanResponse) -> Unit,
        onError: (statusCode: Int, errorBody: String?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        cancelado = false
        val inicio = agora()

        api.enviar(autorizacao, apiKey, imagem).enqueue(object : Callback<ReceiptScanStatus> {
            override fun onResponse(call: Call<ReceiptScanStatus>, response: Response<ReceiptScanStatus>) {
                if (cancelado) return
                val pedido = response.body()

                when {
                    !response.isSuccessful ->
                        onError(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                    pedido == null -> onFailure(IOException("O servidor não devolveu o pedido da leitura."))
                    else -> Consulta(autorizacao, apiKey, pedido.pedidoId, inicio, onSuccess, onError, onFailure)
                        .tratar(pedido)
                }
            }

            override fun onFailure(call: Call<ReceiptScanStatus>, t: Throwable) {
                if (!cancelado) onFailure(t)
            }
        })
    }

    private inner class Consulta(
        private val autorizacao: String,
        private val apiKey: String,
        private val pedido: String,
        private val inicio: Long,
        private val onSuccess: (ReceiptScanResponse) -> Unit,
        private val onError: (Int, String?) -> Unit,
        private val onFailure: (Throwable) -> Unit
    ) {

        fun tratar(status: ReceiptScanStatus) {
            when (status.status) {
                "pronto" -> status.resultado?.let(onSuccess)
                    ?: onFailure(IOException("A leitura terminou sem resultado."))

                "erro" -> onError(status.codigo ?: 500, Gson().toJson(mapOf("detail" to status.detalhe)))

                else -> proxima(falhasSeguidas = 0)
            }
        }

        private fun proxima(falhasSeguidas: Int) {
            if (cancelado) return
            if (agora() - inicio > limiteMs) return onFailure(LeituraDemorou())

            agendar(intervaloMs) { consultar(falhasSeguidas) }
        }

        private fun consultar(falhasSeguidas: Int) {
            if (cancelado) return

            api.consultar(autorizacao, apiKey, pedido).enqueue(object : Callback<ReceiptScanStatus> {
                override fun onResponse(call: Call<ReceiptScanStatus>, response: Response<ReceiptScanStatus>) {
                    if (cancelado) return
                    val corpo = response.body()

                    when {
                        response.isSuccessful && corpo != null -> tratar(corpo)

                        // Servidor reiniciando entre uma consulta e outra: tenta de novo.
                        response.code() >= 500 && falhasSeguidas < FALHAS_TOLERADAS ->
                            proxima(falhasSeguidas + 1)

                        else -> onError(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())
                    }
                }

                override fun onFailure(call: Call<ReceiptScanStatus>, t: Throwable) {
                    if (cancelado) return
                    // Rede oscilando no meio da espera nao perde a leitura.
                    if (falhasSeguidas < FALHAS_TOLERADAS) proxima(falhasSeguidas + 1) else onFailure(t)
                }
            })
        }
    }

    companion object {
        const val INTERVALO_MS = 2_000L

        /** Nota longa num servidor lento leva minuto e pouco; acima disso algo travou. */
        const val LIMITE_MS = 5 * 60_000L

        const val FALHAS_TOLERADAS = 3
    }
}
