package com.example.ceris.view.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.ceris.R
import com.example.ceris.view.utils.Seletor

/**
 * Popup de quantidade no meio da tela, do Detalhe do Item: "Quantas unidades
 * foram consumidas" ou "descartadas". Devolve a escolha por fragment result,
 * na chave que quem abriu passou.
 */
class QuantidadeDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val argumentos = requireArguments()
        val conteudo = layoutInflater.inflate(R.layout.dialogo_quantidade, null)

        conteudo.findViewById<TextView>(R.id.titulo_dialogo).setText(argumentos.getInt(ARG_TITULO))
        val seletor = Seletor(
            conteudo.findViewById(R.id.stepper_dialogo),
            inicial = 1,
            maximo = argumentos.getInt(ARG_MAXIMO).coerceAtLeast(1)
        )

        conteudo.findViewById<View>(R.id.btn_salvar_dialogo).setOnClickListener {
            setFragmentResult(
                argumentos.getString(ARG_CHAVE).orEmpty(),
                Bundle().apply { putInt(RESULTADO, seletor.valor) }
            )
            dismiss()
        }
        conteudo.findViewById<View>(R.id.btn_cancelar_dialogo).setOnClickListener { dismiss() }

        return Dialog(requireContext()).apply {
            setContentView(conteudo)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setDimAmount(0.4f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(
                (resources.displayMetrics.widthPixels * FRACAO_LARGURA).toInt()
                    .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    companion object {
        const val TAG = "QuantidadeDialog"
        const val RESULTADO = "quantidade"

        private const val ARG_TITULO = "titulo"
        private const val ARG_MAXIMO = "maximo"
        private const val ARG_CHAVE = "chave"

        /** Como no Figma: o cartao ocupa quase toda a largura do celular. */
        private const val FRACAO_LARGURA = 0.9f

        fun novo(titulo: Int, maximo: Int, chave: String) = QuantidadeDialog().apply {
            arguments = Bundle().apply {
                putInt(ARG_TITULO, titulo)
                putInt(ARG_MAXIMO, maximo)
                putString(ARG_CHAVE, chave)
            }
        }
    }
}
