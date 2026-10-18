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
import com.example.ceris.model.dto.ReceiptItem
import com.example.ceris.repository.NotaFiscalRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import com.example.ceris.util.unidadeNaTela
import java.text.Normalizer
import kotlin.math.ceil

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

        fun makeText(message: String)
    }

    lateinit var listener: Listener

    private val notaApi = RetrofitClient.getApiAutenticada(ApiBaseUrl.ia, NotaFiscalAPI::class.java)
    private val estoqueApi = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java)

    private lateinit var repository: NotaFiscalRepository

    private var catalogo: List<FoodResponse> = emptyList()

    var estado = Estado.CAMERA
        private set

    private val relogio = Handler(Looper.getMainLooper())
    private var passo = 0

    fun init(sessionManager: SessionManager) {
        if (this::repository.isInitialized) return

        repository = NotaFiscalRepository(notaApi, estoqueApi, sessionManager)
        carregarCatalogo()
    }

    private fun carregarCatalogo(depois: () -> Unit = {}) {
        repository.catalogo(
            onSuccess = {
                catalogo = it
                depois()
            },
            onFailure = { depois() }
        )
    }

    fun ler(jpeg: ByteArray) {
        if (estado != Estado.CAMERA) return

        mudar(Estado.LENDO)
        iniciarAndamento()

        repository.ler(
            jpeg,
            onSuccess = { nota ->
                pararAndamento()

                if (nota.items.isEmpty()) {
                    mudar(Estado.CAMERA)
                    listener.makeText("Nenhum produto foi reconhecido. Tente de novo com a nota inteira na moldura.")
                    return@ler
                }

                // Sem catalogo nao ha como casar os itens; tenta mais uma vez
                // antes de abrir a revisao.
                val abrir = {
                    mudar(Estado.REVISAO)
                    listener.itensLidos(nota.store, nota.items.map(::paraRevisao), catalogo)
                }
                if (catalogo.isEmpty()) carregarCatalogo(abrir) else abrir()
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

        val semProduto = escolhidos.count { it.alimentoId == null }
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

    fun descartar() = mudar(Estado.CAMERA)

    private fun mudar(novo: Estado) {
        estado = novo
        listener.estado(novo)
    }

    private fun paraRevisao(item: ReceiptItem): ItemNota {
        val alimento = item.foodId?.let { id -> catalogo.firstOrNull { it.id == id } } ?: casar(item.name)

        return ItemNota(
            nomeNaNota = item.name,
            alimentoId = alimento?.id,
            alimentoNome = alimento?.name,
            // A Core so aceita inteiro positivo: 0,5 kg vira 1. Fica editavel.
            quantidade = ceil(item.quantity).toInt().coerceAtLeast(1),
            unidade = unidadeNaTela(alimento?.unitOfMeasure) ?: unidadeNaTela(item.unit) ?: "un",
            validade = item.expirationDate,
            // So vem marcado o que ja tem produto: o resto a pessoa decide.
            incluir = alimento != null,
            conferir = item.needsReview || alimento == null
        )
    }

    /**
     * Acha o produto do catalogo pelo nome impresso. A nota abrevia e grita
     * ("ALFACE CRESPA UN"), entao vale o nome do catalogo contido no da nota;
     * entre varios, o nome mais longo, que e o mais especifico.
     */
    private fun casar(nomeNaNota: String): FoodResponse? {
        val nota = normalizar(nomeNaNota)
        val palavrasNota = nota.split(' ').filter { it.length >= 3 }.toSet()

        return catalogo
            .mapNotNull { food ->
                val nome = normalizar(food.name)
                val pontos = when {
                    nome == nota -> 3
                    nota.contains(nome) -> 2
                    nome.split(' ').any { it.length >= 3 && it in palavrasNota } -> 1
                    else -> 0
                }
                if (pontos == 0) null else Triple(food, pontos, nome.length)
            }
            .maxWithOrNull(compareBy<Triple<FoodResponse, Int, Int>> { it.second }.thenBy { it.third })
            ?.first
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

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
        super.onCleared()
        pararAndamento()
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
