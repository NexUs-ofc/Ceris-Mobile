package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Local
import com.example.ceris.view.utils.hideNavigationBar

/**
 * Detalhe do mercado (Figma > Protótipos > V1 > Screen - Detalhe Local).
 *
 * Recebe o Local inteiro da lista, entao abre sem chamada de rede. "Como
 * chegar" abre o app de mapas do aparelho nas coordenadas da Core.
 */
class DetalheMercadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_detalhe_mercado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        val local = intent.getSerializableExtra(EXTRA_LOCAL) as? Local ?: return finish()

        findViewById<View>(R.id.btn_voltar_mercado).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txt_nome_mercado).text = local.nome

        val foto = findViewById<ImageView>(R.id.img_mercado)
        local.imagemUrl?.let { Glide.with(this).load(it).placeholder(R.drawable.ic_menu_store).centerCrop().into(foto) }

        linha(R.id.linha_endereco, R.drawable.ic_menu_map_pin, R.string.mercado_endereco, local.enderecoCompleto)
        linha(R.id.linha_horario, R.drawable.ic_menu_clock, R.string.mercado_horario, local.horario)
        linha(
            R.id.linha_distancia,
            R.drawable.ic_menu_navigation,
            R.string.mercado_distancia,
            local.distancia.takeIf { it.isNotBlank() }?.let { getString(R.string.mercado_distancia_valor, it) }
        )

        findViewById<View>(R.id.btn_como_chegar).setOnClickListener { abrirMapa(local) }
    }

    /** Linha sem valor some inteira: a Core nem sempre tem horario. */
    private fun linha(id: Int, icone: Int, titulo: Int, valor: String?) {
        val linha = findViewById<View>(id)
        if (valor.isNullOrBlank()) {
            linha.visibility = View.GONE
            return
        }
        linha.findViewById<ImageView>(R.id.icone_info).setImageResource(icone)
        linha.findViewById<TextView>(R.id.titulo_info).setText(titulo)
        linha.findViewById<TextView>(R.id.valor_info).text = valor
    }

    private fun abrirMapa(local: Local) {
        val destino = if (local.latitude != null && local.longitude != null) {
            "geo:${local.latitude},${local.longitude}?q=${local.latitude},${local.longitude}(${Uri.encode(local.nome)})"
        } else {
            "geo:0,0?q=" + Uri.encode("${local.nome}, ${local.enderecoCompleto}")
        }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destino))) }
            .onFailure { Toast.makeText(this, R.string.mercado_sem_mapa, Toast.LENGTH_LONG).show() }
    }

    companion object {
        private const val EXTRA_LOCAL = "local"

        fun abrir(contexto: Context, local: Local) =
            contexto.startActivity(Intent(contexto, DetalheMercadoActivity::class.java).putExtra(EXTRA_LOCAL, local))
    }
}
