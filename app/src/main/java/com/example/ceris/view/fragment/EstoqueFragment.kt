package com.example.ceris.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Produto
import com.example.ceris.view.ChatbotActivity
import com.example.ceris.view.MainActivity
import com.example.ceris.view.DetalheProdutoActivity
import com.example.ceris.view.adapter.ProdutoAdapter
import com.example.ceris.viewmodel.EstoqueViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Tela Inicial - estoque.
 *
 * O conteúdo e a adaptação ao tablet vivem no XML e em dimens/values-sw480dp.
 * A barra de navegação pertence à MainActivity.
 */
class EstoqueFragment : Fragment(R.layout.fragment_estoque), EstoqueViewModel.Listener {

    private val viewModel: EstoqueViewModel by viewModels()

    private val adapter = ProdutoAdapter { produto ->
        context?.let { DetalheProdutoActivity.abrir(it, produto) }
    }

    private lateinit var lista: RecyclerView
    private lateinit var estadoVazio: View
    private lateinit var textoVazio: TextView
    private lateinit var grupoFiltros: ChipGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lista = view.findViewById(R.id.lista_produtos)
        estadoVazio = view.findViewById(R.id.estado_vazio)
        textoVazio = view.findViewById(R.id.txt_vazio)

        lista.adapter = adapter

        viewModel.listener = this
        viewModel.init(SessionManager(requireContext()))

        view.findViewById<EditText>(R.id.input_busca).doAfterTextChanged {
            viewModel.buscar(it?.toString().orEmpty())
        }

        grupoFiltros = view.findViewById(R.id.grupo_filtros)
        grupoFiltros.setOnCheckedStateChangeListener { grupo, marcados ->
            val id = marcados.firstOrNull()
            val categoria = id?.let { grupo.findViewById<Chip>(it)?.tag as? String }

            viewModel.filtrar(
                when {
                    categoria != null -> EstoqueViewModel.Filtro.Categoria(categoria)
                    id == R.id.filtro_vencendo -> EstoqueViewModel.Filtro.Vencendo
                    else -> EstoqueViewModel.Filtro.Todos
                }
            )
        }

        view.findViewById<ImageButton>(R.id.btn_assistente).setOnClickListener {
            startActivity(Intent(activity, ChatbotActivity::class.java))
        }

        // O perfil exibe a barra sem ser um destino dela, então quem troca o
        // conteúdo é a Activity: o fragment não mexe no FragmentManager dela.
        view.findViewById<ImageButton>(R.id.btn_perfil).setOnClickListener {
            (activity as? MainActivity)?.abrirPerfil()
        }
    }

    // No onResume, e nao so no onViewCreated: a Cerita pode mexer no estoque
    // na conversa, e voltar do chat tem de mostrar o que mudou.
    override fun onResume() {
        super.onResume()
        viewModel.carregar()
    }

    // As abas sao escondidas e reexibidas, nao recriadas, entao o onResume nao
    // roda na volta. Sem isto, o que a nota fiscal acabou de gravar so
    // apareceria reabrindo o app.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.carregar()
    }

    override fun produtosExibidos(produtos: List<Produto>, estoqueVazio: Boolean) {
        adapter.submitList(produtos)

        val vazio = produtos.isEmpty()
        lista.visibility = if (vazio) View.GONE else View.VISIBLE
        estadoVazio.visibility = if (vazio) View.VISIBLE else View.GONE
        textoVazio.setText(if (estoqueVazio) R.string.estoque_vazio else R.string.estoque_sem_resultado)
    }

    override fun categoriasDisponiveis(categorias: List<String>, selecionada: String?) {
        // Todos e Vencendo sao fixos no XML; o que vem depois deles e categoria.
        val fixos = setOf(R.id.filtro_todos, R.id.filtro_vencendo)
        (grupoFiltros.childCount - 1 downTo 0)
            .map { grupoFiltros.getChildAt(it) }
            .filter { it.id !in fixos }
            .forEach { grupoFiltros.removeView(it) }

        categorias.forEach { nome ->
            val chip = layoutInflater.inflate(R.layout.item_filtro_chip, grupoFiltros, false) as Chip
            chip.id = View.generateViewId()
            chip.text = nome
            chip.tag = nome
            grupoFiltros.addView(chip)

            if (nome == selecionada) {
                chip.isChecked = true
            }
        }

        // A categoria marcada pode ter sumido do estoque; nesse caso o
        // ViewModel ja voltou para Todos, e a tela precisa acompanhar.
        if (grupoFiltros.checkedChipId == View.NO_ID) {
            grupoFiltros.check(R.id.filtro_todos)
        }
    }

    override fun carregando(carregando: Boolean) {
        // Sem isto, a "despensa vazia" pisca enquanto o primeiro GET nao volta.
        if (carregando && adapter.itemCount == 0) {
            estadoVazio.visibility = View.GONE
        }
    }

    override fun makeText(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }
}
