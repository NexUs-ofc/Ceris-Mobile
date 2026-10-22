package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StoreAPI
import com.example.ceris.api.aoTerminar
import com.example.ceris.api.bearer
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Local
import com.example.ceris.model.dto.AddressDto
import com.example.ceris.repository.LocaisRepository
import com.example.ceris.view.DetalheMercadoActivity
import com.example.ceris.view.adapter.LocalAdapter

/**
 * Locais (Figma > Protótipos > V1 > Screen - Locais).
 *
 * O endereco do cabecalho e o do perfil na Core, e a lista sao os mercados
 * que a Core acha perto dele. "Alterar" abre o popup de trocar endereco; ao
 * trocar, a lista e recalculada.
 */
class LocaisFragment : Fragment(R.layout.fragment_locais), AlterarEnderecoBottomSheet.Listener {

    private val adapter = LocalAdapter { local -> context?.let { DetalheMercadoActivity.abrir(it, local) } }

    private val perfilApi by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java) }
    private val repository by lazy {
        LocaisRepository(RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StoreAPI::class.java))
    }

    private var endereco: AddressDto? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.lista_locais).adapter = adapter
        view.findViewById<View>(R.id.btn_alterar).setOnClickListener {
            AlterarEnderecoBottomSheet.novo(endereco).show(childFragmentManager, AlterarEnderecoBottomSheet.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) carregar()
    }

    private fun carregar() {
        val token = SessionManager(requireContext()).bearer() ?: return

        perfilApi.buscar(token).aoTerminar(
            onSucesso = { perfil -> mostrarEndereco(perfil?.address) },
            onFalha = { }
        )
        repository.proximos(token, onSucesso = ::mostrarLocais, onFalha = ::avisar)
    }

    private fun mostrarEndereco(novo: AddressDto?) {
        endereco = novo
        view?.findViewById<TextView>(R.id.txt_endereco_atual)?.text =
            LocaisRepository.enderecoCurto(novo) ?: getString(R.string.locais_sem_endereco)
    }

    private fun mostrarLocais(locais: List<Local>) {
        val raiz = view ?: return
        adapter.submitList(locais)
        raiz.findViewById<View>(R.id.txt_sem_locais).visibility = if (locais.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun enderecoAlterado(novo: AddressDto) {
        mostrarEndereco(novo)
        carregar()
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
    }
}
