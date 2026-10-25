package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Receita
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.DetalheReceitaViewModel

class DetalheReceitaActivity : AppCompatActivity(), DetalheReceitaViewModel.Listener {
    private val viewModel: DetalheReceitaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_detalhe_receita)
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
        val passosExibidos = receita.passos.ifEmpty { listOf(getString(R.string.receita_sem_preparo)) }
        passosExibidos.forEachIndexed { indice, passo ->
            val linha = layoutInflater.inflate(R.layout.item_passo_receita, passos, false)
            linha.findViewById<TextView>(R.id.numero_passo).apply {
                text = (indice + 1).toString()
                isVisible = receita.passos.isNotEmpty()
            }
            linha.findViewById<TextView>(R.id.texto_passo).text = passo
            if (indice == passosExibidos.lastIndex) linha.setPadding(linha.paddingLeft, linha.paddingTop, linha.paddingRight, 0)
            passos.addView(linha)
        }
    }

    override fun onDestroy() {
        viewModel.listener = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RECEITA_ID = "receita_id"

        fun intent(
            context: Context,
            receitaId: String,
        ): Intent = Intent(context, DetalheReceitaActivity::class.java).putExtra(EXTRA_RECEITA_ID, receitaId)
    }
}
