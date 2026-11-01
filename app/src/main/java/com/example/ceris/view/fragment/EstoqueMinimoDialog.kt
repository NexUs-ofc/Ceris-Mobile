package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.MinimumQuantityRequest
import com.example.ceris.model.dto.PantryProductSettingResponse
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import com.example.ceris.view.utils.Seletor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** O minimo pertence ao alimento da casa, independentemente do lote aberto. */
class EstoqueMinimoDialog : DialogFragment() {
    private val api by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java) }
    private lateinit var sessions: SessionManager
    private var session: SessionSnapshot? = null
    private var chamada: Call<*>? = null
    private var seletor: Seletor? = null
    private var carregado = false
    private var salvando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessions = SessionManager(requireContext())
        session = sessions.snapshot()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialogo_estoque_minimo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        salvando = false
        seletor = Seletor(view.findViewById(R.id.stepper_dialogo), minimo = 0)
        view.findViewById<View>(R.id.btn_cancelar_dialogo).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.btn_salvar_dialogo).setOnClickListener {
            if (carregado) salvar() else carregar()
        }
        carregar(savedInstanceState?.takeIf { it.containsKey(QUANTIDADE) }?.getInt(QUANTIDADE))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
            setDimAmount(0.4f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt()
                    .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun sessaoAtual(): SessionSnapshot? {
        val origin = session
        if (origin == null || !sessions.isCurrent(origin)) {
            mostrarErro(getString(R.string.locais_sessao_invalida))
            return null
        }
        // Mantem a casa original, mas usa o token atual caso tenha sido renovado.
        return sessions.snapshot()
    }

    private fun carregar(quantidadeEditada: Int? = null) {
        carregado = false
        val origin = sessaoAtual() ?: return
        atualizarControles(ocupado = true)
        mostrarEstado(getString(R.string.estoque_minimo_carregando))
        chamada = api.configuracoes("Bearer ${origin.accessToken}", origin).also { call ->
            call.enqueue(retorno { configuracoes ->
                val foodId = requireArguments().getInt(FOOD_ID)
                seletor?.valor = quantidadeEditada
                    ?: configuracoes.firstOrNull { it.foodId == foodId }?.minimumQuantity ?: 1
                carregado = true
                mostrarEstado(null)
                atualizarControles(ocupado = false)
            })
        }
    }

    private fun salvar() {
        val origin = sessaoAtual() ?: return
        val quantidade = seletor?.valor ?: return
        salvando = true
        atualizarControles(ocupado = true)
        mostrarEstado(getString(R.string.estoque_minimo_salvando))
        chamada = api.configurarMinimo(
            "Bearer ${origin.accessToken}",
            requireArguments().getInt(FOOD_ID),
            MinimumQuantityRequest(quantidade),
            origin,
        ).also { call ->
            call.enqueue(retorno<PantryProductSettingResponse> {
                Toast.makeText(
                    requireContext(),
                    if (quantidade == 0) R.string.estoque_minimo_removido else R.string.estoque_minimo_salvo,
                    Toast.LENGTH_SHORT,
                ).show()
                dismissAllowingStateLoss()
            })
        }
    }

    private fun <T> retorno(onSuccess: (T) -> Unit) = object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (call.isCanceled || view == null || sessaoAtual() == null) return
            val body = response.body()
            if (response.isSuccessful && body != null) {
                onSuccess(body)
            } else {
                mostrarErro(
                    if (response.isSuccessful) getString(R.string.estoque_minimo_resposta_invalida)
                    else mensagemDeErro(response.code(), runCatching { response.errorBody()?.string() }.getOrNull()),
                )
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            if (!call.isCanceled && view != null) mostrarErro(mensagemDeFalha(t))
        }
    }

    private fun mostrarErro(mensagem: String) {
        salvando = false
        mostrarEstado(mensagem, erro = true)
        atualizarControles(ocupado = false)
    }

    private fun mostrarEstado(mensagem: String?, erro: Boolean = false) {
        view?.findViewById<TextView>(R.id.estado_estoque_minimo)?.apply {
            text = mensagem
            visibility = if (mensagem == null) View.GONE else View.VISIBLE
            setTextColor(ContextCompat.getColor(context, if (erro) R.color.md_theme_error else R.color.ceris_cinza))
        }
    }

    private fun atualizarControles(ocupado: Boolean) {
        val root = view ?: return
        root.findViewById<TextView>(R.id.btn_salvar_dialogo).apply {
            isEnabled = !ocupado
            setText(if (carregado) R.string.detalhe_baixa_salvar else R.string.estoque_minimo_tentar_novamente)
        }
        root.findViewById<View>(R.id.btn_menos).isEnabled = !ocupado && carregado
        root.findViewById<View>(R.id.btn_mais).isEnabled = !ocupado && carregado
        root.findViewById<View>(R.id.btn_cancelar_dialogo).isEnabled = !salvando
        isCancelable = !salvando
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Durante um PUT, recarrega do core: o servidor pode ter concluido a gravacao.
        if (carregado && !salvando) seletor?.let { outState.putInt(QUANTIDADE, it.valor) }
    }

    override fun onDestroyView() {
        chamada?.cancel()
        chamada = null
        seletor = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "EstoqueMinimoDialog"
        private const val FOOD_ID = "food_id"
        private const val QUANTIDADE = "quantidade"

        fun novo(foodId: Int) = EstoqueMinimoDialog().apply {
            require(foodId > 0)
            arguments = Bundle().apply { putInt(FOOD_ID, foodId) }
        }
    }
}
