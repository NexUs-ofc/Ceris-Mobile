package com.example.ceris.view.fragment

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.api.openfoodfacts.OpenFoodFactsClient
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.model.CadastroPorCodigo
import com.example.ceris.model.CategoriaAlimento
import com.example.ceris.model.ProdutoEncontrado
import com.example.ceris.model.UnidadeEmbalagem
import com.example.ceris.repository.CodigoBarrasRepository
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Cadastro de um produto lido pelo codigo de barras.
 *
 * Abre na hora com o codigo lido; os dados chegam da base aberta logo depois,
 * se ela tiver o produto. Quem decide e a pessoa: tudo fica editavel.
 */
class CodigoBarrasBottomSheet : BottomSheetDialogFragment() {

    /** Quem abriu a leitura volta a ler quando o cadastro fecha. */
    interface Listener {
        fun aoFecharCadastroCodigo(salvou: Boolean)
    }

    private lateinit var repository: CodigoBarrasRepository
    private lateinit var gtin: String

    private lateinit var situacao: TextView
    private lateinit var campoNome: EditText
    private lateinit var campoMarca: EditText
    private lateinit var campoEmbalagem: EditText
    private lateinit var campoUnidade: MaterialAutoCompleteTextView
    private lateinit var campoCategoria: MaterialAutoCompleteTextView
    private lateinit var txtQuantidade: TextView
    private lateinit var campoValidade: TextView
    private lateinit var confirmar: MaterialButton

    private var quantidade = 1
    private var validade: String? = null
    private var salvou = false

    override fun getTheme(): Int = R.style.TemaBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sheet_codigo_barras, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gtin = requireArguments().getString(ARG_GTIN).orEmpty()
        val sessao = SessionManager(requireContext())
        repository = CodigoBarrasRepository(
            baseAberta = OpenFoodFactsClient.api,
            estoque = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java),
            autorizacao = {
                sessao.getString(SessionKeys.ACCESS_TOKEN, "").takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            }
        )

        situacao = view.findViewById(R.id.txt_situacao_busca)
        campoNome = view.findViewById(R.id.campo_nome_codigo)
        campoMarca = view.findViewById(R.id.campo_marca_codigo)
        campoEmbalagem = view.findViewById(R.id.campo_embalagem_codigo)
        campoUnidade = view.findViewById(R.id.campo_unidade_codigo)
        campoCategoria = view.findViewById(R.id.campo_categoria_codigo)
        txtQuantidade = view.findViewById(R.id.txt_quantidade_codigo)
        campoValidade = view.findViewById(R.id.campo_validade_codigo)
        confirmar = view.findViewById(R.id.btn_confirmar_codigo)

        view.findViewById<TextView>(R.id.txt_codigo).text = getString(R.string.codigo_lido, gtin)

        campoUnidade.setSimpleItems(UnidadeEmbalagem.rotulos().toTypedArray())
        campoUnidade.setText(UnidadeEmbalagem.UNIDADE.rotulo, false)
        campoCategoria.setSimpleItems(CategoriaAlimento.rotulos().toTypedArray())

        mostrarQuantidade()
        view.findViewById<View>(R.id.btn_diminuir_codigo).setOnClickListener {
            if (quantidade > 1) quantidade -= 1
            mostrarQuantidade()
        }
        view.findViewById<View>(R.id.btn_aumentar_codigo).setOnClickListener {
            quantidade += 1
            mostrarQuantidade()
        }

        campoValidade.setOnClickListener { escolherValidade() }
        confirmar.setOnClickListener { salvar() }
        view.findViewById<View>(R.id.btn_cancelar_codigo).setOnClickListener { dismiss() }

        situacao.setText(R.string.codigo_buscando)
        repository.buscar(gtin) { encontrado -> view.post { preencher(encontrado) } }
    }

    private fun preencher(encontrado: ProdutoEncontrado?) {
        if (!isAdded) return

        if (encontrado == null) {
            situacao.setText(R.string.codigo_nao_encontrado)
            return
        }

        situacao.setText(R.string.codigo_encontrado)
        // So preenche o que a pessoa ainda nao digitou.
        if (campoNome.text.isBlank()) encontrado.nome?.let(campoNome::setText)
        if (campoMarca.text.isBlank()) encontrado.marca?.let(campoMarca::setText)
        if (campoEmbalagem.text.isBlank()) {
            encontrado.quantidadeEmbalagem?.let { campoEmbalagem.setText(formatar(it)) }
            encontrado.unidadeEmbalagem?.let { campoUnidade.setText(it.rotulo, false) }
        }
    }

    private fun salvar() {
        val cadastro = CadastroPorCodigo.montar(
            gtin = gtin,
            nome = campoNome.text.toString(),
            marca = campoMarca.text.toString(),
            quantidadeEmbalagem = campoEmbalagem.text.toString(),
            unidade = UnidadeEmbalagem.porRotulo(campoUnidade.text.toString()),
            categoria = CategoriaAlimento.porRotulo(campoCategoria.text.toString()),
            quantidade = quantidade,
            validade = validade
        ).getOrElse { problema ->
            avisar(problema.message.orEmpty())
            return
        }

        confirmar.isEnabled = false
        confirmar.setText(R.string.codigo_salvando)

        repository.registrar(
            cadastro,
            onSuccess = { salvo ->
                salvou = true
                avisar(getString(R.string.codigo_salvo, salvo.foodName ?: cadastro.name))
                if (isAdded) dismiss()
            },
            onError = { codigo, corpo -> falhou(mensagemDeErro(codigo, corpo)) },
            onFailure = { falhou(mensagemDeFalha(it)) }
        )
    }

    private fun falhou(mensagem: String) {
        if (!isAdded) return
        confirmar.isEnabled = true
        confirmar.setText(R.string.codigo_confirmar)
        avisar(mensagem)
    }

    private fun escolherValidade() {
        val inicial = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, ano, mes, dia ->
                val data = Calendar.getInstance().apply { set(ano, mes, dia) }.time
                validade = ISO.format(data)
                campoValidade.text = CURTA.format(data)
            },
            inicial.get(Calendar.YEAR),
            inicial.get(Calendar.MONTH),
            inicial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun mostrarQuantidade() {
        txtQuantidade.text = quantidade.toString()
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? Listener)?.aoFecharCadastroCodigo(salvou)
    }

    companion object {
        const val TAG = "CodigoBarrasBottomSheet"
        private const val ARG_GTIN = "gtin"

        private val ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val CURTA = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

        private fun formatar(valor: Double): String =
            if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString().replace('.', ',')

        fun novo(gtin: String) = CodigoBarrasBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_GTIN, gtin) }
        }
    }
}
