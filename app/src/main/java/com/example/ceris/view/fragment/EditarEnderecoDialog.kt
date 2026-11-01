package com.example.ceris.view.fragment

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.ProfileUpdateRequest
import com.example.ceris.view.utils.MaskTextWatcher
import com.example.ceris.view.utils.Masks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Edita o endereço usado pela busca de mercados próximos. */
class EditarEnderecoDialog : DialogFragment() {
    private var chamada: Call<ProfileResponse>? = null

    @Suppress("DEPRECATION") // Keep the scrollable form resizing above the keyboard on older Android versions.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val conteudo = layoutInflater.inflate(R.layout.dialog_editar_endereco, FrameLayout(requireContext()), false)
        val cep = conteudo.findViewById<EditText>(R.id.campo_cep_endereco)
        val rua = conteudo.findViewById<EditText>(R.id.campo_rua_endereco)
        val numero = conteudo.findViewById<EditText>(R.id.campo_numero_endereco)
        val bairro = conteudo.findViewById<EditText>(R.id.campo_bairro_endereco)
        val cidade = conteudo.findViewById<EditText>(R.id.campo_cidade_endereco)
        val estado = conteudo.findViewById<EditText>(R.id.campo_estado_endereco)
        val salvar = conteudo.findViewById<MaterialButton>(R.id.btn_salvar_endereco)

        arguments?.let(::enderecoDosArgumentos)?.let { address ->
            cep.setText(address.cep.orEmpty().filter(Char::isDigit))
            rua.setText(address.street.orEmpty())
            numero.setText(address.number.orEmpty())
            bairro.setText(address.neighborhood.orEmpty())
            cidade.setText(address.city.orEmpty())
            estado.setText(address.state.orEmpty())
        }
        cep.addTextChangedListener(MaskTextWatcher(Masks.CEP))

        salvar.setOnClickListener {
            listOf(
                R.id.box_cep_endereco,
                R.id.box_rua_endereco,
                R.id.box_numero_endereco,
                R.id.box_bairro_endereco,
                R.id.box_cidade_endereco,
                R.id.box_estado_endereco
            ).forEach { conteudo.findViewById<TextInputLayout>(it).error = null }

            val cepLimpo = cep.text.toString().filter { it in '0'..'9' }
            val campos = listOf(
                cepLimpo to R.id.box_cep_endereco,
                rua.text.toString().trim() to R.id.box_rua_endereco,
                numero.text.toString().trim() to R.id.box_numero_endereco,
                bairro.text.toString().trim() to R.id.box_bairro_endereco,
                cidade.text.toString().trim() to R.id.box_cidade_endereco,
                estado.text.toString().trim() to R.id.box_estado_endereco
            )
            val vazio = campos.firstOrNull { it.first.isBlank() }
            if (vazio != null) {
                conteudo.findViewById<TextInputLayout>(vazio.second).error = getString(R.string.endereco_campo_obrigatorio)
                return@setOnClickListener
            }
            if (cepLimpo.length != 8) {
                conteudo.findViewById<TextInputLayout>(R.id.box_cep_endereco).error = getString(R.string.endereco_cep_invalido)
                return@setOnClickListener
            }
            if (estado.text.toString().trim().length != 2) {
                conteudo.findViewById<TextInputLayout>(R.id.box_estado_endereco).error = getString(R.string.endereco_estado_invalido)
                return@setOnClickListener
            }

            salvarEndereco(
                AddressDto(
                    street = rua.text.toString().trim(),
                    number = numero.text.toString().trim(),
                    neighborhood = bairro.text.toString().trim(),
                    cep = cepLimpo,
                    city = cidade.text.toString().trim(),
                    state = estado.text.toString().trim().uppercase()
                ),
                salvar
            )
        }
        conteudo.findViewById<TextView>(R.id.btn_cancelar_edicao_endereco).setOnClickListener { dismiss() }

        return Dialog(requireContext()).apply {
            setContentView(conteudo)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt()
                    .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onDestroyView() {
        chamada?.cancel()
        chamada = null
        super.onDestroyView()
    }

    private fun salvarEndereco(address: AddressDto, botao: MaterialButton) {
        val sessoes = SessionManager(requireContext())
        val sessao = sessoes.snapshot()
        val profileId = sessao?.profileId
        if (sessao == null || profileId == null) {
            avisar(getString(R.string.locais_sessao_invalida))
            return
        }

        botao.isEnabled = false
        botao.setText(R.string.endereco_salvando)
        val api = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java)
        chamada = api.atualizar("Bearer ${sessao.accessToken}", ProfileUpdateRequest(address = address)).also { request ->
            request.enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (!isAdded || call.isCanceled || !sessoes.isCurrent(sessao)) return
                    if (!response.isSuccessful) {
                        falha(botao, getString(R.string.endereco_salvar_falhou))
                        return
                    }
                    val editorPai = parentFragment as? AlterarEnderecoDialog
                    if (editorPai != null) editorPai.aoSalvarEndereco() else dismiss()
                    avisar(getString(R.string.endereco_salvo_sucesso))
                }

                override fun onFailure(call: Call<ProfileResponse>, error: Throwable) {
                    if (call.isCanceled || !isAdded) return
                    falha(botao, getString(R.string.endereco_salvar_falhou))
                }
            })
        }
    }

    private fun falha(botao: MaterialButton, mensagem: String) {
        botao.isEnabled = true
        botao.setText(R.string.endereco_salvar)
        avisar(mensagem)
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
    }

    private fun enderecoDosArgumentos(args: Bundle): AddressDto? =
        if (args.getBoolean(ARG_HAS_ADDRESS)) {
            AddressDto(
                street = args.getString(ARG_STREET),
                number = args.getString(ARG_NUMBER),
                neighborhood = args.getString(ARG_NEIGHBORHOOD),
                cep = args.getString(ARG_CEP),
                city = args.getString(ARG_CITY),
                state = args.getString(ARG_STATE)
            )
        } else {
            null
        }

    companion object {
        const val TAG = "EditarEnderecoDialog"
        private const val ARG_HAS_ADDRESS = "has_address"
        private const val ARG_STREET = "street"
        private const val ARG_NUMBER = "number"
        private const val ARG_NEIGHBORHOOD = "neighborhood"
        private const val ARG_CEP = "cep"
        private const val ARG_CITY = "city"
        private const val ARG_STATE = "state"

        fun novo(address: AddressDto?) = EditarEnderecoDialog().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_HAS_ADDRESS, address != null)
                putString(ARG_STREET, address?.street)
                putString(ARG_NUMBER, address?.number)
                putString(ARG_NEIGHBORHOOD, address?.neighborhood)
                putString(ARG_CEP, address?.cep)
                putString(ARG_CITY, address?.city)
                putString(ARG_STATE, address?.state)
            }
        }
    }
}
