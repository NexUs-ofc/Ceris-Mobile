package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RecipeAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.StockAPI
import com.example.ceris.api.bearer
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Receita
import com.example.ceris.repository.ReceitasRepository
import com.example.ceris.view.DetalheReceitaActivity
import com.example.ceris.view.adapter.ReceitaAdapter

/**
 * Receitas salvas (Figma > Protótipos > V1 > Screen - Receitas). As que a
 * Cerita salvou na conversa aparecem aqui, vindas da Core.
 */
class ReceitasFragment : Fragment(R.layout.fragment_receitas) {

    private val adapter = ReceitaAdapter { receita -> context?.let { DetalheReceitaActivity.abrir(it, receita) } }

    private val repository by lazy {
        ReceitasRepository(
            RetrofitClient.getApiAutenticada(ApiBaseUrl.core, RecipeAPI::class.java),
            RetrofitClient.getApiAutenticada(ApiBaseUrl.core, StockAPI::class.java)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.lista_receitas).adapter = adapter
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
        repository.salvas(token, onSucesso = ::mostrar, onFalha = { mensagem ->
            context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
        })
    }

    private fun mostrar(receitas: List<Receita>) {
        val raiz = view ?: return
        adapter.submitList(receitas)
        raiz.findViewById<View>(R.id.estado_vazio_receitas).visibility =
            if (receitas.isEmpty()) View.VISIBLE else View.GONE
    }
}
