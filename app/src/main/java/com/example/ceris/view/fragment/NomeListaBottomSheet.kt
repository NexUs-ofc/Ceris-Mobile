package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.example.ceris.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class NomeListaBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun aoConfirmarNomeDaLista(nome: String)
    }

    private lateinit var campo: EditText

    override fun getTheme(): Int = R.style.TemaBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_nome_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argumentos = requireArguments()

        campo = view.findViewById(R.id.campo_nome_lista)
        campo.setText(argumentos.getString(ARG_NOME).orEmpty())
        campo.setSelection(campo.text.length)

        view.findViewById<TextView>(R.id.titulo_sheet)
            .setText(argumentos.getInt(ARG_TITULO))

        val botaoConfirmar = view.findViewById<MaterialButton>(R.id.btn_confirmar)
        botaoConfirmar.setText(argumentos.getInt(ARG_CONFIRMAR))
        botaoConfirmar.setOnClickListener { confirmar() }

        view.findViewById<TextView>(R.id.btn_cancelar).setOnClickListener { dismiss() }

        campo.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_DONE) {
                confirmar()
                true
            } else {
                false
            }
        }

        // O sheet abre só para digitar um nome: o teclado vem junto.
        campo.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun confirmar() {
        val ouvinte = parentFragment as? Listener ?: activity as? Listener ?: return
        ouvinte.aoConfirmarNomeDaLista(campo.text.toString())
        dismiss()
    }

    companion object {
        const val TAG = "NomeListaBottomSheet"

        private const val ARG_TITULO = "titulo"
        private const val ARG_CONFIRMAR = "confirmar"
        private const val ARG_NOME = "nome"

        fun criar(): NomeListaBottomSheet = montar(
            titulo = R.string.lista_nome_criar_titulo,
            confirmar = R.string.lista_nome_criar_confirmar,
            nome = ""
        )

        fun renomear(nomeAtual: String): NomeListaBottomSheet = montar(
            titulo = R.string.lista_nome_renomear_titulo,
            confirmar = R.string.lista_nome_renomear_confirmar,
            nome = nomeAtual
        )

        private fun montar(titulo: Int, confirmar: Int, nome: String) =
            NomeListaBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITULO, titulo)
                    putInt(ARG_CONFIRMAR, confirmar)
                    putString(ARG_NOME, nome)
                }
            }
    }
}
