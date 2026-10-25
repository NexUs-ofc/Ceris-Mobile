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
import com.example.ceris.view.utils.Seletor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Confirmar dados do produto, aberto quando a camera le um codigo de barras.
 *
 * Abre na hora com o que a pessoa precisa conferir: nome, validade e
 * quantidade. O resto do cadastro (marca, embalagem, categoria, foto) vem da
 * base aberta logo depois e segue para a Core sem aparecer na tela, como no
 * Figma. Sem a base, vale embalagem de 1 unidade e categoria "Outros".
 */
class CodigoBarrasBottomSheet : BottomSheetDialogFragment() {

    /** Quem abriu a leitura volta a ler quando o cadastro fecha. */
    interface Listener {
        fun aoFecharCadastroCodigo(salvou: Boolean)
    }

    private lateinit var repository: CodigoBarrasRepository
    private lateinit var gtin: String

    private lateinit var campoNome: EditText
    private lateinit var campoValidade: TextView
    private lateinit var quantidade: Seletor
    private lateinit var confirmar: MaterialButton

    private var encontrado: ProdutoEncontrado? = null
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

        campoNome = view.findViewById(R.id.campo_nome_codigo)
        campoValidade = view.findViewById(R.id.campo_validade_codigo)
        confirmar = view.findViewById(R.id.btn_confirmar_codigo)
        quantidade = Seletor(view.findViewById(R.id.stepper_codigo))

        campoValidade.setOnClickListener { escolherValidade() }
        confirmar.setOnClickListener { salvar() }
        view.findViewById<View>(R.id.btn_cancelar_codigo).setOnClickListener { dismiss() }

        // Ate a base responder, o botao espera: salvar antes mandaria categoria
        // e embalagem genericas para um produto que a base conhece.
        confirmar.isEnabled = false
        confirmar.setText(R.string.codigo_buscando)
        repository.buscar(gtin) { achado -> view.post { preencher(achado) } }
    }

    override fun onStart() {
        super.onStart()
        // Aberto inteiro: com o sheet pela metade os botoes ficavam fora da tela.
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun preencher(achado: ProdutoEncontrado?) {
        if (!isAdded) return
        encontrado = achado
        confirmar.isEnabled = true
        confirmar.setText(R.string.codigo_confirmar)

        // O nome que veio da base fica como esta, sem edicao. Sem o produto na
        // base, o campo continua livre para a pessoa digitar.
        val nome = achado?.nome ?: return
        campoNome.setText(nome)
        travar(campoNome)
    }

    private fun travar(campo: EditText) {
        campo.keyListener = null
        campo.isFocusable = false
        campo.isCursorVisible = false
        campo.isLongClickable = false
        campo.setTextIsSelectable(false)
    }

    private fun salvar() {
        val base = encontrado
        val cadastro = CadastroPorCodigo.montar(
            gtin = gtin,
            nome = campoNome.text.toString(),
            marca = base?.marca.orEmpty(),
            quantidadeEmbalagem = base?.quantidadeEmbalagem?.let(::formatar) ?: "1",
            unidade = base?.unidadeEmbalagem ?: UnidadeEmbalagem.UNIDADE,
            categoria = base?.categoria ?: CategoriaAlimento.OUTROS,
            quantidade = quantidade.valor,
            validade = validade,
            imagemUrl = base?.imagemUrl
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
