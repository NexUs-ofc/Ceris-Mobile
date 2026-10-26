package com.example.ceris.view.utils

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.ceris.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Cartao padrao para informacoes e confirmacoes do app. */
object PopupCeris {
    fun mostrar(
        contexto: Context,
        titulo: String,
        mensagem: String,
        @DrawableRes icone: Int,
        confirmar: String,
        cancelar: String? = null,
        destrutivo: Boolean = false,
        aoConfirmar: () -> Unit = {}
    ) {
        val conteudo = LayoutInflater.from(contexto).inflate(R.layout.dialogo_acao, null)
        conteudo.findViewById<ImageView>(R.id.icone_popup).setImageResource(icone)
        conteudo.findViewById<TextView>(R.id.titulo_popup).text = titulo
        conteudo.findViewById<TextView>(R.id.mensagem_popup).text = mensagem
        val dialogo = MaterialAlertDialogBuilder(contexto).setView(conteudo).create()

        conteudo.findViewById<MaterialButton>(R.id.btn_confirmar_popup).apply {
            text = confirmar
            if (destrutivo) {
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(contexto, R.color.perfil_sair))
            }
            setOnClickListener {
                dialogo.dismiss()
                aoConfirmar()
            }
        }
        conteudo.findViewById<MaterialButton>(R.id.btn_cancelar_popup).apply {
            visibility = if (cancelar == null) View.GONE else View.VISIBLE
            text = cancelar
            setOnClickListener { dialogo.dismiss() }
        }

        dialogo.show()
        dialogo.window?.setLayout(
            (contexto.resources.displayMetrics.widthPixels * 0.9f).toInt()
                .coerceAtMost(contexto.resources.getDimensionPixelSize(R.dimen.content_max_width)),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
