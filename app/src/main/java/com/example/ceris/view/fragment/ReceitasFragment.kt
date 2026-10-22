package com.example.ceris.view.fragment

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.ChatbotActivity
import com.example.ceris.view.DetalheReceitaActivity
import com.example.ceris.view.adapter.ReceitaAdapter
import com.example.ceris.viewmodel.ReceitasViewModel

class ReceitasFragment : Fragment(R.layout.fragment_receitas), ReceitasViewModel.Listener {
    private val viewModel: ReceitasViewModel by viewModels()
    private val adapter =
        ReceitaAdapter { receita ->
            startActivity(DetalheReceitaActivity.intent(requireContext(), receita.id))
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.lista_receitas).adapter = adapter
        view.findViewById<TextView>(R.id.link_gerar_receita).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { startActivity(Intent(requireContext(), ChatbotActivity::class.java)) }
        }
        view.findViewById<View>(R.id.btn_tentar_receitas).setOnClickListener { viewModel.carregar() }
        viewModel.init(SessionManager(requireContext()))
        viewModel.listener = this
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) viewModel.carregar()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null && isResumed) viewModel.carregar()
    }

    override fun estadoAtualizado(estado: ReceitasViewModel.Estado) {
        val raiz = view ?: return
        val receitas = (estado as? ReceitasViewModel.Estado.Conteudo)?.receitas
        raiz.findViewById<View>(R.id.progresso_receitas).isVisible = estado is ReceitasViewModel.Estado.Carregando
        raiz.findViewById<View>(R.id.lista_receitas).isVisible = !receitas.isNullOrEmpty()
        raiz.findViewById<View>(R.id.estado_receitas_vazio).isVisible = receitas?.isEmpty() == true
        raiz.findViewById<View>(R.id.estado_receitas_erro).isVisible = estado is ReceitasViewModel.Estado.Erro
        if (estado is ReceitasViewModel.Estado.Erro) {
            raiz.findViewById<TextView>(R.id.txt_receitas_erro).text = estado.mensagem
        }
        adapter.submitList(receitas.orEmpty())
    }

    override fun onDestroyView() {
        viewModel.listener = null
        viewModel.cancelar()
        view?.findViewById<RecyclerView>(R.id.lista_receitas)?.adapter = null
        super.onDestroyView()
    }
}
