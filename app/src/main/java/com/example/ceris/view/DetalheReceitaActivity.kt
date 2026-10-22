package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Receita
import com.example.ceris.view.utils.hideNavigationBar

/**
 * Detalhe da Receita (Figma > Protótipos > V1 > Screen - Detalhe Receita):
 * foto, tempo/porcoes/dificuldade, ingredientes e modo de preparo numerado.
 *
 * Recebe a receita inteira da lista. O que a Core nao manda (foto, tempo,
 * dificuldade) fica escondido.
 */
class DetalheReceitaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_detalhe_receita)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        val receita = intent.getSerializableExtra(EXTRA_RECEITA) as? Receita ?: return finish()

        findViewById<View>(R.id.btn_voltar_receita).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txt_titulo_receita).text = receita.nome

        val foto = findViewById<ImageView>(R.id.img_detalhe_receita)
        receita.imagemUrl?.let { Glide.with(this).load(it).centerCrop().into(foto) }

        dado(R.id.dado_tempo, receita.tempoPreparo)
        dado(R.id.dado_porcoes, receita.porcoes?.let { getString(R.string.receita_serve, it) })
        dado(R.id.dado_dificuldade, null)
        findViewById<View>(R.id.faixa_dados).visibility =
            if (receita.tempoPreparo == null && receita.porcoes == null) View.GONE else View.VISIBLE

        val ingredientes = findViewById<LinearLayout>(R.id.lista_ingredientes)
        receita.ingredientes.forEach { texto ->
            val linha = layoutInflater.inflate(R.layout.item_ingrediente, ingredientes, false) as TextView
            linha.text = texto
            ingredientes.addView(linha)
        }
        findViewById<View>(R.id.bloco_ingredientes).visibility =
            if (receita.ingredientes.isEmpty()) View.GONE else View.VISIBLE

        val passos = findViewById<LinearLayout>(R.id.lista_passos)
        receita.passos.forEachIndexed { posicao, texto ->
            val linha = layoutInflater.inflate(R.layout.item_passo, passos, false)
            linha.findViewById<TextView>(R.id.numero_passo).text = (posicao + 1).toString()
            linha.findViewById<TextView>(R.id.texto_passo).text = texto
            passos.addView(linha)
        }
        findViewById<View>(R.id.bloco_passos).visibility = if (receita.passos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun dado(id: Int, texto: String?) {
        val campo = findViewById<TextView>(id)
        campo.text = texto
        campo.visibility = if (texto.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    companion object {
        private const val EXTRA_RECEITA = "receita"

        fun abrir(contexto: Context, receita: Receita) =
            contexto.startActivity(Intent(contexto, DetalheReceitaActivity::class.java).putExtra(EXTRA_RECEITA, receita))
    }
}
