package com.example.ceris.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.ceris.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AvaliacaoSecaoDialog : DialogFragment() {

    private var notaSelecionada = 0
    private var resultadoPublicado = false
    private var campoFeedback: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notaSelecionada = savedInstanceState?.getInt(STATE_RATING) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val conteudo = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_avaliar_secao, null, false)
        campoFeedback = conteudo.findViewById(R.id.campo_feedback)
        campoFeedback?.setText(savedInstanceState?.getString(STATE_COMMENT).orEmpty())

        val estrelas = (1..5).map { nota ->
            conteudo.findViewById<TextView>(
                resources.getIdentifier("estrela_$nota", "id", requireContext().packageName)
            ).apply {
                setOnClickListener { selecionarNota(nota, estrelasConteudo = conteudo) }
            }
        }
        atualizarEstrelas(estrelas)
        conteudo.findViewById<View>(R.id.btn_cancelar_avaliacao)
            .setOnClickListener {
                publicarCancelamento()
                dismiss()
            }
        conteudo.findViewById<View>(R.id.btn_salvar_avaliacao).setOnClickListener {
            if (notaSelecionada == 0) {
                Toast.makeText(requireContext(), R.string.avaliacao_escolha_nota, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putBoolean(KEY_SAVE, true)
                    putString(KEY_SESSION, requireArguments().getString(ARG_SESSION))
                    putInt(KEY_RATING, notaSelecionada)
                    putString(KEY_COMMENT, campoFeedback?.text?.toString())
                }
            )
            resultadoPublicado = true
            dismiss()
        }

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setView(conteudo)
            .create()

        dialogo.setOnShowListener {
            dialogo.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).toInt().coerceAtMost(
                    (600 * resources.displayMetrics.density).toInt()
                ),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        dialogo.setOnCancelListener { publicarCancelamento() }
        return dialogo
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RATING, notaSelecionada)
        outState.putString(STATE_COMMENT, campoFeedback?.text?.toString())
        super.onSaveInstanceState(outState)
    }

    private fun selecionarNota(nota: Int, estrelasConteudo: View) {
        notaSelecionada = nota
        atualizarEstrelas((1..5).map { indice ->
            estrelasConteudo.findViewById(
                resources.getIdentifier("estrela_$indice", "id", requireContext().packageName)
            )
        })
    }

    private fun atualizarEstrelas(estrelas: List<TextView>) {
        estrelas.forEachIndexed { indice, estrela ->
            val selecionada = indice < notaSelecionada
            estrela.text = if (selecionada) "★" else "☆"
            estrela.setTextColor(
                requireContext().getColor(
                    if (selecionada) R.color.md_theme_primary
                    else R.color.ceris_cinza_claro
                )
            )
        }
    }

    private fun publicarCancelamento() {
        if (resultadoPublicado) return
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putBoolean(KEY_SAVE, false)
                putString(KEY_SESSION, requireArguments().getString(ARG_SESSION))
            }
        )
        resultadoPublicado = true
    }

    companion object {
        const val RESULT_KEY = "avaliacao_secao_resultado"
        const val KEY_SAVE = "salvar"
        const val KEY_SESSION = "session_id"
        const val KEY_RATING = "nota"
        const val KEY_COMMENT = "feedback"

        private const val ARG_SESSION = "arg_session_id"
        private const val STATE_RATING = "state_nota"
        private const val STATE_COMMENT = "state_feedback"

        fun nova(sessionId: String) = AvaliacaoSecaoDialog().apply {
            arguments = Bundle().apply { putString(ARG_SESSION, sessionId) }
        }
    }
}
