package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Receita
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.DetalheReceitaViewModel
import com.google.android.material.card.MaterialCardView

class DetalheReceitaActivity : AppCompatActivity(), DetalheReceitaViewModel.Listener {
    private val viewModel: DetalheReceitaViewModel by viewModels()
    private val cardsPassos = mutableListOf<MaterialCardView>()
    private var receitaExibidaId: String? = null
    private var passoAberto: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_detalhe_receita)
        receitaExibidaId = savedInstanceState?.getString(ESTADO_RECEITA_ID)
        passoAberto = savedInstanceState?.getInt(ESTADO_PASSO_ABERTO, -1)?.takeIf { it >= 0 }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        findViewById<View>(R.id.btn_receita_voltar).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_tentar_detalhe_receita).setOnClickListener { carregar() }
        viewModel.init(SessionManager(this))
        viewModel.listener = this
        carregar()
    }

    private fun carregar() {
        viewModel.carregar(intent.getStringExtra(EXTRA_RECEITA_ID).orEmpty())
    }

    override fun estadoAtualizado(estado: DetalheReceitaViewModel.Estado) {
        findViewById<View>(R.id.progresso_detalhe_receita).isVisible = estado is DetalheReceitaViewModel.Estado.Carregando
        findViewById<View>(R.id.conteudo_receita).isVisible = estado is DetalheReceitaViewModel.Estado.Conteudo
        findViewById<View>(R.id.estado_detalhe_receita_erro).isVisible = estado is DetalheReceitaViewModel.Estado.Erro
        when (estado) {
            is DetalheReceitaViewModel.Estado.Conteudo -> preencher(estado.receita)
            is DetalheReceitaViewModel.Estado.Erro -> findViewById<TextView>(R.id.txt_detalhe_receita_erro).text = estado.mensagem
            DetalheReceitaViewModel.Estado.Carregando -> Unit
        }
    }

    private fun preencher(receita: Receita) {
        if (receitaExibidaId != receita.id) passoAberto = null
        receitaExibidaId = receita.id
        passoAberto = passoAberto?.takeIf { it in receita.passos.indices }
        findViewById<TextView>(R.id.titulo_receita).text = receita.nome
        findViewById<View>(R.id.grupo_tempo_receita).isVisible = !receita.tempoPreparo.isNullOrBlank()
        findViewById<TextView>(R.id.txt_detalhe_tempo).text = receita.tempoPreparo
        findViewById<View>(R.id.grupo_porcoes_receita).isVisible = receita.porcoes != null
        findViewById<TextView>(R.id.txt_detalhe_porcoes).text = receita.porcoes?.let { getString(R.string.receita_porcoes, it) }
        findViewById<View>(R.id.grupo_dificuldade_receita).isVisible = receita.dificuldade != null
        findViewById<TextView>(R.id.txt_detalhe_dificuldade).text = receita.dificuldade?.rotulo
        findViewById<View>(R.id.card_resumo_receita).isVisible =
            !receita.tempoPreparo.isNullOrBlank() || receita.porcoes != null || receita.dificuldade != null
        val ingredientes = findViewById<LinearLayout>(R.id.ingredientes_receita)
        ingredientes.removeAllViews()
        val ingredientesExibidos = receita.ingredientes.ifEmpty { listOf(getString(R.string.receita_sem_ingredientes)) }
        ingredientesExibidos.forEachIndexed { indice, ingrediente ->
            val linha = layoutInflater.inflate(R.layout.item_ingrediente_receita, ingredientes, false)
            linha.findViewById<TextView>(R.id.txt_ingrediente).text = ingrediente
            if (indice == ingredientesExibidos.lastIndex) linha.setPadding(linha.paddingLeft, linha.paddingTop, linha.paddingRight, 0)
            ingredientes.addView(linha)
        }
        val passos = findViewById<LinearLayout>(R.id.passos_receita)
        passos.removeAllViews()
        cardsPassos.clear()
        passos.isVisible = receita.passos.isNotEmpty()
        findViewById<View>(R.id.subtitulo_preparo_receita).isVisible = receita.passos.isNotEmpty()
        findViewById<View>(R.id.card_preparo_vazio).isVisible = receita.passos.isEmpty()
        receita.passos.forEachIndexed { indice, passo ->
            val card = layoutInflater.inflate(R.layout.item_passo_receita, passos, false) as MaterialCardView
            val titulo = passo.titulo.ifBlank { getString(R.string.receita_passo_titulo, indice + 1) }
            card.findViewById<TextView>(R.id.numero_passo).text = (indice + 1).toString()
            card.findViewById<TextView>(R.id.titulo_passo).text = titulo
            card.findViewById<TextView>(R.id.texto_passo).text =
                passo.descricao.ifBlank { getString(R.string.receita_passo_sem_detalhes) }
            val cabecalho = card.findViewById<View>(R.id.cabecalho_passo)
            cabecalho.contentDescription = getString(R.string.receita_passo_acessibilidade, indice + 1, titulo)
            ViewCompat.setAccessibilityDelegate(
                cabecalho,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat,
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = Button::class.java.name
                    }
                },
            )
            val alternar =
                View.OnClickListener {
                    passoAberto = if (passoAberto == indice) null else indice
                    atualizarPassos()
                }
            card.setOnClickListener(alternar)
            cabecalho.setOnClickListener(alternar)
            if (indice == receita.passos.lastIndex) {
                val parametros = card.layoutParams as ViewGroup.MarginLayoutParams
                parametros.bottomMargin = 0
                card.layoutParams = parametros
            }
            cardsPassos.add(card)
            passos.addView(card)
        }
        atualizarPassos()
    }

    private fun atualizarPassos() {
        val roxo = ContextCompat.getColor(this, R.color.md_theme_primary)
        val branco = ContextCompat.getColor(this, R.color.ceris_white)
        val titulo = ContextCompat.getColor(this, R.color.card_titulo)
        val borda = ContextCompat.getColor(this, R.color.ceris_cinza_claro)
        val secundario = ContextCompat.getColor(this, R.color.receita_passo_texto_secundario)
        val secundarioExpandido = ContextCompat.getColor(this, R.color.receita_passo_texto_secundario_expandido)
        cardsPassos.forEachIndexed { indice, card ->
            val expandido = passoAberto == indice
            card.setCardBackgroundColor(if (expandido) roxo else branco)
            card.strokeColor = if (expandido) roxo else borda
            card.findViewById<TextView>(R.id.titulo_passo).setTextColor(if (expandido) branco else titulo)
            card.findViewById<TextView>(R.id.dica_passo).apply {
                setText(if (expandido) R.string.receita_passo_detalhes else R.string.receita_passo_abrir)
                setTextColor(if (expandido) secundarioExpandido else secundario)
            }
            card.findViewById<View>(R.id.texto_passo).isVisible = expandido
            card.findViewById<ImageView>(R.id.icone_passo).apply {
                rotation = if (expandido) -90f else 90f
                imageTintList = ColorStateList.valueOf(if (expandido) branco else roxo)
            }
            val cabecalho = card.findViewById<View>(R.id.cabecalho_passo)
            ViewCompat.setStateDescription(
                cabecalho,
                getString(if (expandido) R.string.receita_passo_expandido else R.string.receita_passo_recolhido),
            )
            val expandir = AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND
            val recolher = AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE
            ViewCompat.removeAccessibilityAction(cabecalho, if (expandido) expandir.id else recolher.id)
            ViewCompat.replaceAccessibilityAction(cabecalho, if (expandido) recolher else expandir, null) { _, _ ->
                cabecalho.performClick()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ESTADO_RECEITA_ID, receitaExibidaId)
        outState.putInt(ESTADO_PASSO_ABERTO, passoAberto ?: -1)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        viewModel.listener = null
        super.onDestroy()
    }

    companion object {
        private const val ESTADO_RECEITA_ID = "receita_exibida_id"
        private const val ESTADO_PASSO_ABERTO = "passo_receita_aberto"
        const val EXTRA_RECEITA_ID = "receita_id"

        fun intent(
            context: Context,
            receitaId: String,
        ): Intent = Intent(context, DetalheReceitaActivity::class.java).putExtra(EXTRA_RECEITA_ID, receitaId)
    }
}
