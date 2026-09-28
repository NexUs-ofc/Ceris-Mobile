package com.example.ceris.view

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.CategoriaItem
import com.example.ceris.model.ItemLista
import com.example.ceris.model.UnidadeMedida
import com.example.ceris.repository.ShoppingListRepository
import com.example.ceris.util.Moeda
import com.example.ceris.view.adapter.ItemListaAdapter
import com.example.ceris.view.fragment.AdicionarItemBottomSheet
import com.example.ceris.view.fragment.NomeListaBottomSheet
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.ListaComprasViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class ListaComprasActivity :
    AppCompatActivity(),
    ListaComprasViewModel.Listener,
    AdicionarItemBottomSheet.Listener,
    NomeListaBottomSheet.Listener {

    private val viewModel: ListaComprasViewModel by viewModels()

    private lateinit var titulo: TextView
    private lateinit var concluidos: TextView
    private lateinit var percentual: TextView
    private lateinit var barra: LinearProgressIndicator
    private lateinit var listaItens: RecyclerView
    private lateinit var imgVazio: View
    private lateinit var txtVazio: View
    private lateinit var grupoTotal: Group
    private lateinit var txtTotal: TextView

    private val adapter = ItemListaAdapter(
        aoMarcar = { item, marcado -> viewModel.alternarMarcado(item.id, marcado) },
        aoSegurar = { item -> confirmarRemocaoDoItem(item) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_lista_compras)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val listaId = intent.getStringExtra(EXTRA_LISTA_ID)

        if (listaId.isNullOrBlank()) {
            makeText("Não foi possível abrir a lista.")
            finish()
            return
        }

        ligarViews()

        titulo.text = intent.getStringExtra(EXTRA_LISTA_NOME).orEmpty()

        viewModel.listener = this
        viewModel.init(listaId, SessionManager(this), ListaLocalStore(this))
        viewModel.carregar()
    }

    private fun ligarViews() {
        titulo = findViewById(R.id.txt_titulo)
        concluidos = findViewById(R.id.txt_concluidos)
        percentual = findViewById(R.id.txt_percentual)
        barra = findViewById(R.id.barra_progresso)
        listaItens = findViewById(R.id.lista_itens)
        imgVazio = findViewById(R.id.img_vazio)
        txtVazio = findViewById(R.id.txt_vazio)
        grupoTotal = findViewById(R.id.grupo_total)
        txtTotal = findViewById(R.id.txt_total)

        listaItens.adapter = adapter

        findViewById<ImageButton>(R.id.btn_voltar).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_renomear).setOnClickListener { pedirNovoNome() }
        findViewById<MaterialButton>(R.id.btn_adicionar_item).setOnClickListener {
            AdicionarItemBottomSheet().show(supportFragmentManager, AdicionarItemBottomSheet.TAG)
        }
    }

    override fun aoConfirmarItem(
        nome: String,
        quantidade: Double,
        unidade: UnidadeMedida,
        categoria: CategoriaItem,
        valor: Double
    ) {
        viewModel.adicionarItem(nome, quantidade, unidade, categoria, valor)
    }

    override fun detalheCarregado(detalhe: ShoppingListRepository.Detalhe) {
        titulo.text = detalhe.nome

        concluidos.text = getString(R.string.lista_concluidos, detalhe.concluidos, detalhe.total)
        percentual.text = getString(R.string.lista_percentual, detalhe.progresso)
        barra.setProgressCompat(detalhe.progresso, true)

        val vazia = detalhe.itens.isEmpty()

        adapter.submitList(ItemListaAdapter.agrupar(detalhe.itens))

        // A RecyclerView fica visivel mesmo vazia: a ilustracao esta ancorada
        // nas bordas dela, e em GONE a ConstraintLayout a reduz a um ponto.
        imgVazio.visibility = if (vazia) View.VISIBLE else View.GONE
        txtVazio.visibility = if (vazia) View.VISIBLE else View.GONE
        grupoTotal.visibility = if (vazia) View.GONE else View.VISIBLE

        txtTotal.text = Moeda.formatar(detalhe.valorTotal)
    }

    override fun listaRemovida() {
        finish()
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun pedirNovoNome() {
        NomeListaBottomSheet.renomear(titulo.text.toString())
            .show(supportFragmentManager, NomeListaBottomSheet.TAG)
    }

    override fun aoConfirmarNomeDaLista(nome: String) {
        viewModel.renomear(nome)
    }

    // Toque longo remove: o design não tem botão de excluir na linha.
    private fun confirmarRemocaoDoItem(item: ItemLista) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.nome)
            .setMessage(R.string.lista_item_remover_pergunta)
            .setNegativeButton(R.string.lista_nome_cancelar, null)
            .setPositiveButton(R.string.lista_excluir_confirmar) { _, _ ->
                viewModel.removerItem(item.id)
            }
            .show()
    }

    companion object {
        const val EXTRA_LISTA_ID = "lista_id"
        const val EXTRA_LISTA_NOME = "lista_nome"
    }
}
