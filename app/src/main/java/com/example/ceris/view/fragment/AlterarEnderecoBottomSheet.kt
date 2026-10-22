package com.example.ceris.view.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.ceris.BuildConfig
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.api.aoTerminar
import com.example.ceris.api.bearer
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.ProfileUpdateRequest
import com.example.ceris.model.dto.ViaCEPResponse
import com.example.ceris.repository.LocaisRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Alterar endereco (Figma > Protótipos > V1 > Screen - Locais, popup).
 *
 * A Core guarda um endereco so, o do perfil. "Enderecos salvos" sao o atual e
 * os ultimos usados neste aparelho; escolher um deles, buscar um CEP ou usar a
 * localizacao atual grava o novo no perfil (PATCH /api/profile) e a lista de
 * mercados e recalculada.
 */
class AlterarEnderecoBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun enderecoAlterado(novo: AddressDto)
    }

    private val viaCep by lazy { RetrofitClient.getApi(BuildConfig.VIA_CEP_URL, ViaCEPAPI::class.java) }
    private val perfilApi by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java) }

    private var atual: AddressDto? = null

    private val pedirLocalizacao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) usarLocalizacao() else avisar(getString(R.string.endereco_sem_permissao))
    }

    override fun getTheme(): Int = R.style.TemaBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sheet_alterar_endereco, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        atual = arguments?.getString(ARG_ATUAL)?.let { Gson().fromJson(it, AddressDto::class.java) }

        val busca = view.findViewById<EditText>(R.id.campo_busca_endereco)
        busca.doAfterTextChanged { texto ->
            val cep = texto?.toString()?.filter(Char::isDigit).orEmpty()
            if (cep.length == TAMANHO_CEP) buscarCep(cep)
        }
        view.findViewById<View>(R.id.btn_localizacao_atual).setOnClickListener { pedirLocalizacaoAtual() }
        view.findViewById<View>(R.id.btn_novo_endereco).setOnClickListener {
            busca.requestFocus()
            avisar(getString(R.string.endereco_digite_cep))
        }

        mostrarSalvos(view)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    /** O atual primeiro, marcado; depois os ultimos usados, sem repetir. */
    private fun mostrarSalvos(raiz: View) {
        val lista = raiz.findViewById<LinearLayout>(R.id.lista_enderecos)
        lista.removeAllViews()

        val salvos = (listOfNotNull(atual) + lerSalvos(requireContext()))
            .distinctBy { chave(it) }
            .take(MAX_SALVOS)

        raiz.findViewById<View>(R.id.rotulo_salvos).visibility = if (salvos.isEmpty()) View.GONE else View.VISIBLE

        salvos.forEachIndexed { posicao, endereco ->
            val linha = layoutInflater.inflate(R.layout.item_endereco_salvo, lista, false)
            val eAtual = atual != null && chave(endereco) == chave(atual!!)
            linha.findViewById<ImageView>(R.id.icone_endereco)
                .setImageResource(if (posicao == 0) R.drawable.ic_menu_house else R.drawable.ic_menu_briefcase)
            linha.findViewById<TextView>(R.id.txt_titulo_endereco).text =
                if (eAtual) getString(R.string.endereco_atual) else endereco.city.orEmpty()
            linha.findViewById<TextView>(R.id.txt_endereco).text = LocaisRepository.enderecoCurto(endereco)
            linha.findViewById<View>(R.id.check_endereco).visibility = if (eAtual) View.VISIBLE else View.INVISIBLE
            linha.setOnClickListener { if (!eAtual) gravar(endereco) }
            lista.addView(linha)
        }
    }

    private fun buscarCep(cep: String) {
        viaCep.getAddressByCEP(cep).aoTerminar(
            onSucesso = { achado ->
                if (achado == null || !achado.isValid()) avisar(getString(R.string.endereco_cep_invalido)) else pedirNumero(achado)
            },
            onFalha = { avisar(getString(R.string.endereco_cep_invalido)) }
        )
    }

    /** O CEP da rua; o numero so a pessoa sabe. */
    private fun pedirNumero(achado: ViaCEPResponse, numeroSugerido: String = "") {
        val contexto = context ?: return
        val campo = EditText(contexto).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = getString(R.string.endereco_numero)
            setText(numeroSugerido)
        }
        val margem = resources.getDimensionPixelSize(R.dimen.dialogo_padding)
        val caixa = FrameLayout(contexto).apply {
            setPadding(margem, 0, margem, 0)
            addView(campo)
        }

        MaterialAlertDialogBuilder(contexto)
            .setTitle(listOfNotNull(achado.logradouro, achado.bairro).filter { it.isNotBlank() }.joinToString(" - "))
            .setView(caixa)
            .setPositiveButton(R.string.endereco_usar) { _, _ ->
                val numero = campo.text.toString().trim()
                if (numero.isEmpty()) return@setPositiveButton avisar(getString(R.string.endereco_sem_numero))
                gravar(
                    AddressDto(
                        street = achado.logradouro,
                        number = numero,
                        neighborhood = achado.bairro?.ifBlank { null } ?: achado.localidade,
                        cep = achado.cep?.filter(Char::isDigit),
                        city = achado.localidade,
                        state = achado.uf
                    )
                )
            }
            .setNegativeButton(R.string.lista_nome_cancelar, null)
            .show()
    }

    private fun pedirLocalizacaoAtual() {
        val permitido = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permitido) usarLocalizacao() else pedirLocalizacao.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Ultima posicao conhecida do aparelho, virada em CEP pelo Geocoder. O
     * Geocoder bloqueia, entao roda fora da thread principal.
     */
    @SuppressLint("MissingPermission")
    private fun usarLocalizacao() {
        val contexto = context ?: return
        val gerente = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val posicao = gerente.getProviders(true)
            .mapNotNull { runCatching { gerente.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return avisar(getString(R.string.endereco_sem_localizacao))

        Executors.newSingleThreadExecutor().execute {
            @Suppress("DEPRECATION")
            val achado = runCatching {
                Geocoder(contexto, Locale("pt", "BR")).getFromLocation(posicao.latitude, posicao.longitude, 1)
            }.getOrNull()?.firstOrNull()

            view?.post {
                val cep = achado?.postalCode?.filter(Char::isDigit)
                if (cep == null || cep.length != TAMANHO_CEP) {
                    avisar(getString(R.string.endereco_sem_localizacao))
                } else {
                    viaCep.getAddressByCEP(cep).aoTerminar(
                        onSucesso = { via -> if (via != null && via.isValid()) pedirNumero(via, achado.subThoroughfare.orEmpty()) },
                        onFalha = ::avisar
                    )
                }
            }
        }
    }

    private fun gravar(endereco: AddressDto) {
        val contexto = context ?: return
        val token = SessionManager(contexto).bearer() ?: return

        perfilApi.atualizar(token, ProfileUpdateRequest(address = endereco)).aoTerminar(
            onSucesso = { perfil ->
                atual?.let { guardarSalvo(contexto, it) }
                guardarSalvo(contexto, endereco)
                (parentFragment as? Listener)?.enderecoAlterado(perfil?.address ?: endereco)
                if (isAdded) dismiss()
            },
            onFalha = ::avisar
        )
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
    }

    companion object {
        const val TAG = "AlterarEnderecoBottomSheet"

        private const val ARG_ATUAL = "atual"
        private const val TAMANHO_CEP = 8
        private const val MAX_SALVOS = 3
        private const val PREFS = "ceris_enderecos"
        private const val CHAVE_SALVOS = "salvos"

        fun novo(atual: AddressDto?) = AlterarEnderecoBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_ATUAL, atual?.let { Gson().toJson(it) }) }
        }

        private fun chave(endereco: AddressDto) =
            listOf(endereco.cep, endereco.number).joinToString("-") { it.orEmpty().trim().lowercase() }

        private fun lerSalvos(contexto: Context): List<AddressDto> {
            val json = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE_SALVOS, null)
                ?: return emptyList()
            return runCatching {
                Gson().fromJson<List<AddressDto>>(json, object : TypeToken<List<AddressDto>>() {}.type)
            }.getOrNull().orEmpty()
        }

        /** O mais recente primeiro, sem repetir, no maximo MAX_SALVOS. */
        private fun guardarSalvo(contexto: Context, endereco: AddressDto) {
            val lista = (listOf(endereco) + lerSalvos(contexto)).distinctBy(::chave).take(MAX_SALVOS)
            contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(CHAVE_SALVOS, Gson().toJson(lista))
                .apply()
        }
    }
}
