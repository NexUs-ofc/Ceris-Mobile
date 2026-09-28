package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.ceris.R
import com.example.ceris.model.CategoriaItem
import com.example.ceris.model.UnidadeMedida
import com.example.ceris.util.Moeda
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlin.math.max

class AdicionarItemBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun aoConfirmarItem(
            nome: String,
            quantidade: Double,
            unidade: UnidadeMedida,
            categoria: CategoriaItem,
            valor: Double
        )
    }

    private var quantidade = 1.0

    private lateinit var campoNome: EditText
    private lateinit var campoQuantidade: EditText
    private lateinit var campoUnidade: MaterialAutoCompleteTextView
    private lateinit var campoCategoria: MaterialAutoCompleteTextView
    private lateinit var campoValor: EditText

    override fun getTheme(): Int = R.style.TemaBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_adicionar_item, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        campoNome = view.findViewById(R.id.campo_nome)
        campoQuantidade = view.findViewById(R.id.campo_quantidade)
        campoUnidade = view.findViewById(R.id.campo_unidade)
        campoCategoria = view.findViewById(R.id.campo_categoria)
        campoValor = view.findViewById(R.id.campo_valor)

        campoUnidade.setSimpleItems(UnidadeMedida.rotulos().toTypedArray())
        campoUnidade.setText(UnidadeMedida.UNIDADE.rotulo(), false)

        campoCategoria.setSimpleItems(CategoriaItem.rotulos().toTypedArray())
        campoCategoria.setText(CategoriaItem.PADRAO.rotulo, false)

        mostrarQuantidade()

        view.findViewById<ImageButton>(R.id.btn_diminuir).setOnClickListener {
            quantidade = max(1.0, lerQuantidade() - 1)
            mostrarQuantidade()
        }

        view.findViewById<ImageButton>(R.id.btn_aumentar).setOnClickListener {
            quantidade = lerQuantidade() + 1
            mostrarQuantidade()
        }

        view.findViewById<MaterialButton>(R.id.btn_confirmar).setOnClickListener { confirmar() }
        view.findViewById<TextView>(R.id.btn_cancelar).setOnClickListener { dismiss() }
    }

    private fun confirmar() {
        val ouvinte = activity as? Listener ?: return

        ouvinte.aoConfirmarItem(
            nome = campoNome.text.toString(),
            quantidade = lerQuantidade(),
            unidade = UnidadeMedida.porRotulo(campoUnidade.text.toString()),
            categoria = CategoriaItem.porRotulo(campoCategoria.text.toString()),
            valor = Moeda.interpretar(campoValor.text.toString())
        )

        dismiss()
    }

    // O campo é editável, então o valor exibido manda - não o último passo dado.
    private fun lerQuantidade(): Double =
        campoQuantidade.text.toString().replace(',', '.').toDoubleOrNull() ?: quantidade

    private fun mostrarQuantidade() {
        val texto = if (quantidade % 1.0 == 0.0) {
            quantidade.toLong().toString()
        } else {
            quantidade.toString()
        }
        campoQuantidade.setText(texto)
    }

    companion object {
        const val TAG = "AdicionarItemBottomSheet"
    }
}
