package com.example.ceris.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.NotaFiscalAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.model.revisarNota
import com.example.ceris.repository.NotaFiscalRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha

/**
 * Leitura de nota fiscal: camera, leitura, revisao e gravacao, tudo no mesmo
 * fragment. A tela implementa Listener e so reage, como nas demais.
 */
class NotaFiscalViewModel : ViewModel() {

    enum class Estado { CAMERA, LENDO, REVISAO, SALVANDO }

    interface Listener {
        fun estado(estado: Estado)

        /** Texto unico de andamento enquanto a nota esta sendo lida. */
        fun progresso(texto: String)

        fun itensLidos(loja: String?, itens: List<ItemNota>, catalogo: List<FoodResponse>)

        fun salvo(salvos: Int, falhas: Int)

        fun produtosNaoEncontrados(quantidade: Int, indisponiveis: Int)

        fun buscaIndisponivel()

        fun makeText(message: String)
    }

    lateinit var listener: Listener

    private val notaApi = RetrofitClient.getApiAutenticada(ApiBaseUrl.ia, NotaFiscalAPI::class.java)
    private val estoqueApi = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java)

    private lateinit var repository: NotaFiscalRepository

    private var leitura = 0L

    var estado = Estado.CAMERA
        private set

    private val relogio = Handler(Looper.getMainLooper())
    private var passo = 0

    fun init(sessionManager: SessionManager) {
        if (this::repository.isInitialized) return

        repository = NotaFiscalRepository(notaApi, estoqueApi, sessionManager)
    }

    fun ler(jpeg: ByteArray) {
        if (estado != Estado.CAMERA) return

        mudar(Estado.LENDO)
        iniciarAndamento()
        val atual = ++leitura

        repository.ler(
            jpeg,
            onSuccess = { nota ->
                if (atual != leitura) return@ler
                if (nota.items.isEmpty()) {
                    pararAndamento()
                    mudar(Estado.CAMERA)
                    listener.makeText("Nenhum produto foi reconhecido. Tente novamente com a nota inteira.")
                    return@ler
                }
                repository.buscarProdutos(
                    nota.items.map { it.name },
                    sucesso = { resposta ->
                        if (atual == leitura) {
                            pararAndamento()
                            val revisao = revisarNota(nota.items, resposta)
                            if (revisao.itens.isEmpty()) mudar(Estado.CAMERA) else {
                                mudar(Estado.REVISAO)
                                listener.itensLidos(nota.store, revisao.itens, emptyList())
                            }
                            if (revisao.ignorados > 0) {
                                listener.produtosNaoEncontrados(revisao.ignorados, revisao.indisponiveis)
                            }
                        }
                    },
                    falha = {
                        if (atual == leitura) {
                            pararAndamento()
                            mudar(Estado.CAMERA)
                            listener.buscaIndisponivel()
                        }
                    },
                )
            },
            onError = { status, corpo ->
                pararAndamento()
                mudar(Estado.CAMERA)
                listener.makeText(mensagemDeErro(status, corpo))
            },
            onFailure = { falha ->
                pararAndamento()
                mudar(Estado.CAMERA)
                listener.makeText(mensagemDeFalha(falha))
            }
        )
    }

    fun salvar(itens: List<ItemNota>) {
        val escolhidos = itens.filter { it.incluir }

        if (escolhidos.isEmpty()) {
            listener.makeText("Marque ao menos um produto para adicionar.")
            return
        }

        val semProduto = escolhidos.count { it.alimentoId == null && it.produtoEncontrado?.valido() != true }
        if (semProduto > 0) {
            listener.makeText(
                if (semProduto == 1) "Escolha o produto de 1 item marcado, ou desmarque-o."
                else "Escolha o produto de $semProduto itens marcados, ou desmarque-os."
            )
            return
        }

        mudar(Estado.SALVANDO)
        repository.salvar(escolhidos) { salvos, falhas ->
            mudar(Estado.CAMERA)
            listener.salvo(salvos, falhas)
        }
    }

    fun descartar() {
        leitura++
        repository.cancelarLeitura()
        pararAndamento()
        mudar(Estado.CAMERA)
    }

    private fun mudar(novo: Estado) {
        estado = novo
        listener.estado(novo)
    }

    private fun iniciarAndamento() {
        passo = 0
        avancar()
    }

    private fun avancar() {
        if (estado != Estado.LENDO) return

        listener.progresso(ANDAMENTO[passo])
        if (passo < ANDAMENTO.lastIndex) {
            passo += 1
            relogio.postDelayed(::avancar, INTERVALO_MS)
        }
    }

    private fun pararAndamento() = relogio.removeCallbacksAndMessages(null)

    override fun onCleared() {
        leitura++
        super.onCleared()
        pararAndamento()
        if (this::repository.isInitialized) repository.cancelarLeitura()
    }

    private companion object {
        const val INTERVALO_MS = 5_000L

        val ANDAMENTO = listOf(
            "Lendo a nota…",
            "Reconhecendo os produtos…",
            "Estimando as validades…",
            "Quase lá, notas longas demoram mais…",
            "Ainda lendo, falta pouco…"
        )
    }
}
