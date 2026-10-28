package com.example.ceris.view.fragment

import android.content.DialogInterface
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import com.example.ceris.R
import com.example.ceris.view.utils.CalendarioValidade
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class NomeListaBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun aoConfirmarNomeDaLista(nome: String, data: String?)
    }

    private lateinit var campo: EditText
    private var dataSelecionada: String? = null
    private var campoData: TextView? = null

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
        dataSelecionada = argumentos.getString(ARG_DATA)
        if (savedInstanceState != null) {
            dataSelecionada = savedInstanceState.getString(ESTADO_DATA, dataSelecionada)
        }
        campoData = view.findViewById(R.id.campo_data_lista)
        atualizarCampoData()
        campoData?.setOnClickListener {
            esconderTeclado()
            campo.clearFocus()
            CalendarioValidade.mostrar(requireContext(), dataSelecionada) { escolhida ->
                dataSelecionada = escolhida
                atualizarCampoData()
            }
        }

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

        // Mantém o teclado fora do sheet por padrão; tocar no campo de nome abre-o.
        campo.showSoftInputOnFocus = true
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        view.post { dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ESTADO_DATA, dataSelecionada)
        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        esconderTeclado()
        super.onDismiss(dialog)
    }

    private fun esconderTeclado() {
        val im = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        im?.hideSoftInputFromWindow(campo.windowToken, 0)
    }

    private fun confirmar() {
        val ouvinte = parentFragment as? Listener ?: activity as? Listener ?: return
        ouvinte.aoConfirmarNomeDaLista(campo.text.toString(), dataSelecionada)
        dismiss()
    }

    private fun atualizarCampoData() {
        campoData?.text = dataSelecionada?.let {
            CalendarioValidade.formatar(it, "dd/MM/yyyy")
        } ?: getString(R.string.lista_data_sem_data)
    }

    companion object {
        const val TAG = "NomeListaBottomSheet"

        private const val ARG_TITULO = "titulo"
        private const val ARG_CONFIRMAR = "confirmar"
        private const val ARG_NOME = "nome"
        private const val ARG_DATA = "data"
        private const val ESTADO_DATA = "estado_data"

        fun criar(): NomeListaBottomSheet = montar(
            titulo = R.string.lista_nome_criar_titulo,
            confirmar = R.string.lista_nome_criar_confirmar,
            nome = "",
            data = null
        )

        fun renomear(nomeAtual: String, dataAtual: String?): NomeListaBottomSheet = montar(
            titulo = R.string.lista_nome_renomear_titulo,
            confirmar = R.string.lista_nome_renomear_confirmar,
            nome = nomeAtual,
            data = dataAtual
        )

        private fun montar(titulo: Int, confirmar: Int, nome: String, data: String?) =
            NomeListaBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITULO, titulo)
                    putInt(ARG_CONFIRMAR, confirmar)
                    putString(ARG_NOME, nome)
                    putString(ARG_DATA, data)
                }
            }
    }
}
