package com.example.ceris.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ListaCompra
import com.example.ceris.view.ListaComprasActivity
import com.example.ceris.view.adapter.ListaAdapter
import com.example.ceris.viewmodel.ListasViewModel
import com.google.android.material.button.MaterialButton

class ListasFragment :
    Fragment(R.layout.fragment_listas),
    ListasViewModel.Listener,
    NomeListaBottomSheet.Listener {

    private val viewModel: ListasViewModel by viewModels()

    private lateinit var lista: RecyclerView
    private lateinit var txtVazia: TextView

    private val adapter = ListaAdapter { escolhida -> abrirDetalhe(escolhida.id, escolhida.nome) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lista = view.findViewById(R.id.lista_listas)
        txtVazia = view.findViewById(R.id.txt_listas_vazia)
        lista.adapter = adapter

        view.findViewById<MaterialButton>(R.id.btn_criar_lista).setOnClickListener {
            pedirNome()
        }

        viewModel.listener = this
        viewModel.init(SessionManager(requireContext()), ListaLocalStore(requireContext()))
    }

    // Recarrega ao voltar do detalhe: progresso e total podem ter mudado lá.
    override fun onResume() {
        super.onResume()
        viewModel.carregar()
    }

    override fun listasCarregadas(listas: List<ListaCompra>) {
        adapter.submitList(listas)

        val vazia = listas.isEmpty()
        lista.visibility = if (vazia) View.GONE else View.VISIBLE
        txtVazia.visibility = if (vazia) View.VISIBLE else View.GONE
    }

    override fun listaCriada(id: String, nome: String) {
        abrirDetalhe(id, nome)
    }

    override fun makeText(message: String) {
        // O fragment pode já ter saído de cena enquanto a chamada estava no ar.
        val contexto = context ?: return
        Toast.makeText(contexto, message, Toast.LENGTH_LONG).show()
    }

    // childFragmentManager para o sheet achar este fragment como ouvinte.
    private fun pedirNome() {
        NomeListaBottomSheet.criar()
            .show(childFragmentManager, NomeListaBottomSheet.TAG)
    }

    override fun aoConfirmarNomeDaLista(nome: String) {
        viewModel.criarLista(nome)
    }

    private fun abrirDetalhe(id: String, nome: String) {
        startActivity(
            Intent(requireContext(), ListaComprasActivity::class.java)
                .putExtra(ListaComprasActivity.EXTRA_LISTA_ID, id)
                .putExtra(ListaComprasActivity.EXTRA_LISTA_NOME, nome)
        )
    }
}
