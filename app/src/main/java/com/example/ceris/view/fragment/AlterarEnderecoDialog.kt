package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.LocaisAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.repository.LocaisRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Escolhe a origem da consulta de lojas; não altera o cadastro do usuário. */
class AlterarEnderecoDialog : DialogFragment() {
    interface Listener {
        fun usarLocalizacaoAtual()
        fun usarEnderecoSalvo()
    }

    private var profileCall: Call<ProfileResponse>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_alterar_endereco, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_localizacao_atual).setOnClickListener {
            (parentFragment as? Listener)?.usarLocalizacaoAtual()
            dismiss()
        }
        view.findViewById<View>(R.id.btn_cancelar_endereco).setOnClickListener { dismiss() }
        carregarEnderecoSalvo(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.4f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt()
                    .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }
    }

    private fun carregarEnderecoSalvo(root: View) {
        val status = root.findViewById<TextView>(R.id.estado_endereco_salvo)
        val sessionManager = SessionManager(requireContext())
        val session = sessionManager.snapshot()
        if (session?.profileId == null) {
            status.setText(R.string.endereco_salvo_indisponivel)
            return
        }
        val api = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, LocaisAPI::class.java)
        profileCall = api.profile("Bearer ${session.accessToken}", session).also { call ->
            call.enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (call.isCanceled || !sessionManager.isCurrent(session)) return
                    if (!response.isSuccessful) {
                        status.setText(R.string.endereco_salvo_falha)
                        return
                    }
                    val profile = response.body()?.takeIf { it.id == session.profileId }
                    val address = profile?.address
                    val label = LocaisRepository.enderecoCurto(address)
                    val currentRoot = view ?: return
                    if (label.isNullOrBlank()) {
                        status.setText(R.string.endereco_salvo_indisponivel)
                        return
                    }
                    status.visibility = View.GONE
                    val list = currentRoot.findViewById<LinearLayout>(R.id.lista_enderecos)
                    val row = layoutInflater.inflate(R.layout.item_endereco_salvo, list, false)
                    row.findViewById<ImageView>(R.id.icone_endereco)
                        .setImageResource(R.drawable.ic_menu_house)
                    row.findViewById<TextView>(R.id.txt_titulo_endereco).setText(R.string.endereco_atual)
                    row.findViewById<TextView>(R.id.txt_endereco).text = label
                    row.findViewById<View>(R.id.check_endereco).visibility =
                        if (arguments?.getBoolean(ARG_SAVED_SELECTED) == true) View.VISIBLE else View.INVISIBLE
                    row.setOnClickListener {
                        (parentFragment as? Listener)?.usarEnderecoSalvo()
                        dismiss()
                    }
                    list.addView(row)
                }

                override fun onFailure(call: Call<ProfileResponse>, error: Throwable) {
                    if (!call.isCanceled) view?.findViewById<TextView>(R.id.estado_endereco_salvo)
                        ?.setText(R.string.endereco_salvo_falha)
                }
            })
        }
    }

    override fun onDestroyView() {
        profileCall?.cancel()
        profileCall = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AlterarEnderecoDialog"
        private const val ARG_SAVED_SELECTED = "saved_selected"

        fun novo(savedSelected: Boolean) = AlterarEnderecoDialog().apply {
            arguments = Bundle().apply { putBoolean(ARG_SAVED_SELECTED, savedSelected) }
        }
    }
}
