package com.example.ceris.view.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.ceris.R

/** Confirma a exclusão sem executar chamadas enquanto o usuário decide. */
class ConfirmarExclusaoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val content = layoutInflater.inflate(R.layout.dialogo_confirmar_exclusao, null)
        content.findViewById<TextView>(R.id.titulo_exclusao).text = args.getString(TITULO)
        content.findViewById<TextView>(R.id.nome_exclusao).text = args.getString(NOME)
        content.findViewById<TextView>(R.id.mensagem_exclusao).text = args.getString(MENSAGEM)
        content.findViewById<View>(R.id.btn_cancelar_exclusao).setOnClickListener { dismiss() }
        content.findViewById<View>(R.id.btn_confirmar_exclusao).setOnClickListener {
            it.isEnabled = false
            setFragmentResult(args.getString(CHAVE)!!, Bundle().apply { putString(ID, args.getString(ID)) })
            dismiss()
        }
        return Dialog(requireContext()).apply {
            setContentView(content)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt()
                .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    companion object {
        const val TAG = "ConfirmarExclusaoDialog"
        const val ID = "id"
        private const val TITULO = "titulo"
        private const val NOME = "nome"
        private const val MENSAGEM = "mensagem"
        private const val CHAVE = "chave"

        fun novo(titulo: String, nome: String, mensagem: String, chave: String, id: String? = null) =
            ConfirmarExclusaoDialog().apply {
                arguments = Bundle().apply {
                    putString(TITULO, titulo)
                    putString(NOME, nome)
                    putString(MENSAGEM, mensagem)
                    putString(CHAVE, chave)
                    putString(ID, id)
                }
            }
    }
}
