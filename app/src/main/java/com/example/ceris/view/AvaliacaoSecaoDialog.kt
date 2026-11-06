package com.example.ceris.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
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
            conteudo.findViewById<ImageView>(
                resources.getIdentifier("estrela_$nota", "id", requireContext().packageName)
            ).apply {
                setOnClickListener { selecionarNota(nota, estrelasConteudo = conteudo) }
            }
        }
        atualizarEstrelas(estrelas)
        atualizarResumoNota(conteudo)
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
            val densidade = resources.displayMetrics.density
            val larguraDisponivel = resources.displayMetrics.widthPixels - (32 * densidade).toInt()
            dialogo.window?.setLayout(
                larguraDisponivel.coerceAtMost((440 * densidade).toInt()),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialogo.window?.setBackgroundDrawableResource(R.drawable.bg_dialogo)
            dialogo.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
            estrelasConteudo.findViewById<ImageView>(
                resources.getIdentifier("estrela_$indice", "id", requireContext().packageName)
            )
        })
        atualizarResumoNota(estrelasConteudo)
    }

    private fun atualizarResumoNota(conteudo: View) {
        val rotulo = conteudo.findViewById<TextView>(R.id.rotulo_nota_avaliacao)
        if (notaSelecionada == 0) {
            rotulo.visibility = View.GONE
            return
        }

        val descricao = when (notaSelecionada) {
            1 -> getString(R.string.avaliacao_muito_ruim)
            2 -> getString(R.string.avaliacao_ruim)
            3 -> getString(R.string.avaliacao_regular)
            4 -> getString(R.string.avaliacao_bom)
            else -> getString(R.string.avaliacao_excelente)
        }
        rotulo.text = getString(R.string.avaliacao_nota_resumo, notaSelecionada, descricao)
        rotulo.visibility = View.VISIBLE
    }

    private fun atualizarEstrelas(estrelas: List<ImageView>) {
        estrelas.forEachIndexed { indice, estrela ->
            val selecionada = indice < notaSelecionada
            estrela.setImageResource(
                if (selecionada) R.drawable.ic_avaliacao_estrela_cheia
                else R.drawable.ic_avaliacao_estrela_vazia
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

        fun nova(sessionId: String?) = AvaliacaoSecaoDialog().apply {
            arguments = Bundle().apply { putString(ARG_SESSION, sessionId) }
        }
    }
}
