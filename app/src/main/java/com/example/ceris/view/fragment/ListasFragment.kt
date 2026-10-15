package com.example.ceris.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
    private lateinit var imgVazia: ImageView

    private var temListas = false

    private val adapter = ListaAdapter { escolhida -> abrirDetalhe(escolhida.id, escolhida.nome) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lista = view.findViewById(R.id.lista_listas)
        txtVazia = view.findViewById(R.id.txt_listas_vazia)
        imgVazia = view.findViewById(R.id.img_listas_vazia)
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
        temListas = listas.isNotEmpty()

        txtVazia.setText(R.string.listas_vazia)
        mostrarAviso(!temListas)
    }

    override fun listaCriada(id: String, nome: String) {
        abrirDetalhe(id, nome)
    }

    /**
     * Sem lista nenhuma na tela, a falha ocupa o lugar do aviso de vazio.
     *
     * Antes a tela ficava em branco: o `listasCarregadas` nunca chegava, as duas
     * views do estado vazio seguiam `gone` e o Toast sumia em alguns segundos.
     * Quem abria o app com a sessão morta via título, subtítulo e um vão branco
     * até o botão de criar.
     *
     * Com listas já na tela o Toast continua: trocar o conteúdo por uma mensagem
     * apagaria o que a pessoa estava lendo por causa de uma falha passageira.
     */
    override fun makeText(message: String) {
        // O fragment pode já ter saído de cena enquanto a chamada estava no ar.
        val contexto = context ?: return

        if (temListas) {
            Toast.makeText(contexto, message, Toast.LENGTH_LONG).show()
            return
        }

        txtVazia.text = message
        mostrarAviso(true)
    }

    private fun mostrarAviso(visivel: Boolean) {
        lista.visibility = if (visivel) View.GONE else View.VISIBLE
        txtVazia.visibility = if (visivel) View.VISIBLE else View.GONE
        imgVazia.visibility = if (visivel) View.VISIBLE else View.GONE
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
