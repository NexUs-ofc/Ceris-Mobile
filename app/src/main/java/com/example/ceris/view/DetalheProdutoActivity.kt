package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.StockItemUpdateRequest
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import com.example.ceris.view.fragment.QuantidadeDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.ceris.api.openfoodfacts.OpenFoodFactsClient
import com.example.ceris.model.Nutricao
import com.example.ceris.repository.NutricaoRepository
import java.util.Locale
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto
import com.example.ceris.view.utils.hideNavigationBar

/**
 * Detalhe do Item, conforme o protótipo do Figma.
 *
 * Recebe o que o card ja tem por extras, entao abre na hora, sem chamada de
 * rede. Quem abre e a tela de estoque, com abrir().
 */
class DetalheProdutoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_detalhe_produto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_detalhe_voltar).setOnClickListener { finish() }
        quantidade = intent.getIntExtra(EXTRA_QUANTIDADE, 0)
        preencher()

        findViewById<View>(R.id.btn_consumido).setOnClickListener {
            pedirQuantidade(R.string.detalhe_consumidas_titulo, CHAVE_CONSUMO)
        }
        findViewById<View>(R.id.btn_descartar).setOnClickListener {
            pedirQuantidade(R.string.detalhe_descartadas_titulo, CHAVE_DESCARTE)
        }
        supportFragmentManager.setFragmentResultListener(CHAVE_CONSUMO, this) { _, resultado ->
            darBaixa(resultado.getInt(QuantidadeDialog.RESULTADO), consumido = true)
        }
        supportFragmentManager.setFragmentResultListener(CHAVE_DESCARTE, this) { _, resultado ->
            darBaixa(resultado.getInt(QuantidadeDialog.RESULTADO), consumido = false)
        }
    }

    /** Quantidade do lote agora; muda a cada baixa parcial. */
    private var quantidade = 0

    private val api by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java) }

    private fun pedirQuantidade(titulo: Int, chave: String) {
        if (quantidade <= 0 || intent.getStringExtra(EXTRA_ID).isNullOrBlank()) return
        QuantidadeDialog.novo(titulo, quantidade, chave).show(supportFragmentManager, QuantidadeDialog.TAG)
    }

    /**
     * Baixa no estoque. A Core nao tem "consumir": a baixa e a quantidade nova
     * do lote, ou a remocao dele quando tudo saiu (ela nao aceita zero).
     */
    private fun darBaixa(unidades: Int, consumido: Boolean) {
        val id = intent.getStringExtra(EXTRA_ID)?.toIntOrNull() ?: return
        val token = SessionManager(this).getString(SessionKeys.ACCESS_TOKEN, "")
            .takeIf { it.isNotBlank() }?.let { "Bearer $it" } ?: return
        val restante = quantidade - unidades

        val aviso = getString(
            if (consumido) R.string.detalhe_baixa_consumido else R.string.detalhe_baixa_descartado,
            unidades,
            intent.getStringExtra(EXTRA_UNIDADE).orEmpty()
        )
        val falhou = { mensagem: String -> Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show() }

        if (restante <= 0) {
            api.remover(token, id).enqueue(
                retorno(onSucesso = {
                    Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show()
                    finish()
                }, onFalha = falhou)
            )
        } else {
            api.atualizar(token, id, StockItemUpdateRequest(quantity = restante)).enqueue(
                retorno(onSucesso = {
                    quantidade = restante
                    mostrarQuantidade()
                    Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show()
                }, onFalha = falhou)
            )
        }
    }

    private fun <T> retorno(onSucesso: () -> Unit, onFalha: (String) -> Unit) = object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (isDestroyed) return
            if (response.isSuccessful) {
                onSucesso()
            } else {
                onFalha(mensagemDeErro(response.code(), runCatching { response.errorBody()?.string() }.getOrNull()))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            if (!isDestroyed) onFalha(mensagemDeFalha(t))
        }
    }

    private fun mostrarQuantidade() {
        findViewById<TextView>(R.id.txt_detalhe_quantidade).text = getString(
            R.string.produto_quantidade_unidade,
            quantidade,
            intent.getStringExtra(EXTRA_UNIDADE).orEmpty()
        )
    }

    private fun preencher() {
        findViewById<TextView>(R.id.txt_detalhe_nome).text =
            intent.getStringExtra(EXTRA_NOME).orEmpty()

        val categoria = intent.getStringExtra(EXTRA_CATEGORIA)
        findViewById<TextView>(R.id.txt_detalhe_categoria).text = if (categoria.isNullOrBlank()) {
            getString(R.string.detalhe_produto_sem_categoria)
        } else {
            getString(R.string.detalhe_produto_categoria, categoria)
        }

        // Validade e so a data no chip, sem o "Vence em" do card.
        val validade = intent.getStringExtra(EXTRA_VALIDADE).orEmpty()
        findViewById<TextView>(R.id.txt_detalhe_validade).text =
            validade.ifBlank { getString(R.string.detalhe_produto_sem_validade) }

        mostrarQuantidade()

        intent.getStringExtra(EXTRA_IMAGEM)?.takeIf { it.isNotBlank() }?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ph_produto)
                .error(R.drawable.ph_produto)
                .into(findViewById<ImageView>(R.id.img_detalhe_produto))
        }

        preencherSituacao()
        carregarNutricao()
    }

    /**
     * A tabela vem de fora e pode nao existir: enquanto nao chega, ou se nao
     * houver casamento seguro, o bloco fica oculto.
     */
    private fun carregarNutricao() {
        val nome = intent.getStringExtra(EXTRA_NOME).orEmpty()
        val marca = intent.getStringExtra(EXTRA_MARCA)

        NutricaoRepository(OpenFoodFactsClient.api).buscar(nome, marca) { nutricao ->
            if (isDestroyed || nutricao == null) return@buscar

            if (intent.getStringExtra(EXTRA_IMAGEM).isNullOrBlank() && nutricao.imagemUrl != null) {
                Glide.with(this)
                    .load(nutricao.imagemUrl)
                    .into(findViewById<ImageView>(R.id.img_detalhe_produto))
            }

            mostrarNutricao(nutricao)
        }
    }

    private fun mostrarNutricao(nutricao: Nutricao) {
        val tabela = findViewById<LinearLayout>(R.id.tabela_nutricao)
        tabela.removeAllViews()

        val linhas = listOf(
            R.string.detalhe_nutricao_energia to formatar(nutricao.energiaKcal, " kcal"),
            R.string.detalhe_nutricao_carboidratos to formatar(nutricao.carboidratos, "g"),
            R.string.detalhe_nutricao_proteinas to formatar(nutricao.proteinas, "g"),
            R.string.detalhe_nutricao_gorduras to formatar(nutricao.gorduras, "g"),
            R.string.detalhe_nutricao_gordura_saturada to formatar(nutricao.gordurasSaturadas, "g"),
            R.string.detalhe_nutricao_fibra to formatar(nutricao.fibras, "g"),
            R.string.detalhe_nutricao_sodio to formatar(nutricao.sodioGramas?.times(1000), "mg")
        ).filter { it.second != null }

        if (linhas.isEmpty()) return

        // Porcao sempre 100 g: e a base da fonte, nao um dado do produto.
        val porcao = getString(R.string.detalhe_nutricao_porcao) to getString(R.string.detalhe_nutricao_porcao_valor)
        val todas = listOf(porcao) + linhas.map { (rotulo, valor) -> getString(rotulo) to valor!! }
        todas.forEachIndexed { posicao, (rotulo, valor) ->
            adicionarLinha(tabela, rotulo, valor, par = posicao % 2 == 1)
        }

        findViewById<LinearLayout>(R.id.bloco_nutricao).visibility = View.VISIBLE
    }

    private fun adicionarLinha(tabela: LinearLayout, rotulo: String, valor: String, par: Boolean) {
        val linha = layoutInflater.inflate(R.layout.item_nutricao, tabela, false)
        if (par) linha.setBackgroundColor(ContextCompat.getColor(this, R.color.nutricao_linha_par))
        linha.findViewById<TextView>(R.id.txt_nutriente).text = rotulo
        linha.findViewById<TextView>(R.id.txt_valor_nutriente).text = valor
        tabela.addView(linha)
    }

    /** Sem casas decimais quando inteiro; com uma, com virgula, como no protótipo (2,5g). */
    private fun formatar(valor: Double?, unidade: String): String? {
        if (valor == null) return null
        val numero = if (valor % 1.0 == 0.0) {
            valor.toLong().toString()
        } else {
            String.format(Locale("pt", "BR"), "%.1f", valor)
        }
        return numero + unidade
    }

    /** Chip de status com a borda e o fundo do selo do card. */
    private fun preencherSituacao() {
        val situacao = intent.getStringExtra(EXTRA_SITUACAO)
            ?.let { runCatching { SituacaoProduto.valueOf(it) }.getOrNull() }
            ?: SituacaoProduto.SEM_VALIDADE

        val texto = findViewById<TextView>(R.id.txt_detalhe_situacao)
        val rotulo = findViewById<TextView>(R.id.rotulo_situacao)
        val chip = findViewById<android.view.View>(R.id.chip_situacao)
        val aparencia = aparencia(situacao)

        if (aparencia == null) {
            texto.text = getString(R.string.produto_sem_validade)
            return
        }

        val (rotuloSituacao, fundo, cor) = aparencia
        val corTexto = ContextCompat.getColor(this, cor)
        texto.setText(rotuloSituacao)
        texto.setTextColor(corTexto)
        rotulo.setTextColor(corTexto)

        chip.background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@DetalheProdutoActivity, fundo))
            cornerRadius = resources.getDimension(R.dimen.detalhe_raio)
            setStroke(resources.getDimensionPixelSize(R.dimen.card_stroke_width), corTexto)
        }
    }

    /**
     * Rotulo e cor do status. A cor e a mesma do selo do card, para os dois
     * nunca divergirem.
     */
    private fun aparencia(situacao: SituacaoProduto) = when (situacao) {
        SituacaoProduto.FRESCO -> Triple(
            R.string.produto_fresco, R.color.selo_fresco_fundo, R.color.selo_fresco_texto
        )
        SituacaoProduto.VENCENDO -> Triple(
            R.string.produto_vencendo, R.color.selo_vencendo_fundo, R.color.selo_vencendo_texto
        )
        SituacaoProduto.VENCIDO -> Triple(
            R.string.produto_vencido, R.color.selo_vencido_fundo, R.color.selo_vencido_texto
        )
        SituacaoProduto.SEM_VALIDADE -> null
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_NOME = "nome"
        private const val CHAVE_CONSUMO = "consumo"
        private const val CHAVE_DESCARTE = "descarte"
        private const val EXTRA_CATEGORIA = "categoria"
        private const val EXTRA_QUANTIDADE = "quantidade"
        private const val EXTRA_UNIDADE = "unidade"
        private const val EXTRA_VALIDADE = "validade"
        private const val EXTRA_SITUACAO = "situacao"
        private const val EXTRA_IMAGEM = "imagem"
        private const val EXTRA_MARCA = "marca"

        fun abrir(contexto: Context, produto: Produto) = contexto.startActivity(intent(contexto, produto))

        /** Separado do abrir() para o teste de tela montar a mesma Intent. */
        fun intent(contexto: Context, produto: Produto): Intent =
            Intent(contexto, DetalheProdutoActivity::class.java)
                .putExtra(EXTRA_ID, produto.id)
                .putExtra(EXTRA_NOME, produto.nome)
                .putExtra(EXTRA_CATEGORIA, produto.categoria)
                .putExtra(EXTRA_QUANTIDADE, produto.quantidade)
                .putExtra(EXTRA_UNIDADE, produto.unidade)
                .putExtra(EXTRA_VALIDADE, produto.validade)
                .putExtra(EXTRA_SITUACAO, produto.situacao.name)
                .putExtra(EXTRA_IMAGEM, produto.imagemUrl)
                .putExtra(EXTRA_MARCA, produto.marca)
    }
}
