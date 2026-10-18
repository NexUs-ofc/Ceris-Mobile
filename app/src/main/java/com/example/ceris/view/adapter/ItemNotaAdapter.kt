package com.example.ceris.view.adapter

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.util.unidadeNaTela
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Itens lidos da nota, na revisao. A lista e editada no lugar: o que a pessoa
 * muda aqui e exatamente o que vai para o estoque.
 */
class ItemNotaAdapter : RecyclerView.Adapter<ItemNotaAdapter.Card>() {

    val itens = mutableListOf<ItemNota>()

    private var catalogo: List<FoodResponse> = emptyList()

    fun trocar(novos: List<ItemNota>, catalogo: List<FoodResponse>) {
        this.catalogo = catalogo.sortedBy { it.name.lowercase() }
        itens.clear()
        itens.addAll(novos)
        notifyDataSetChanged()
    }

    override fun getItemCount() = itens.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Card(LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false))

    override fun onBindViewHolder(card: Card, position: Int) = card.mostrar(itens[position])

    inner class Card(view: View) : RecyclerView.ViewHolder(view) {
        private val incluir: MaterialCheckBox = view.findViewById(R.id.check_incluir)
        private val nome: TextView = view.findViewById(R.id.txt_nome_nota)
        private val conferir: TextView = view.findViewById(R.id.txt_conferir)
        private val produto: MaterialAutoCompleteTextView = view.findViewById(R.id.campo_produto)
        private val quantidade: TextView = view.findViewById(R.id.txt_quantidade_nota)
        private val validade: TextView = view.findViewById(R.id.txt_validade_nota)
        private val menos: ImageButton = view.findViewById(R.id.btn_diminuir_nota)
        private val mais: ImageButton = view.findViewById(R.id.btn_aumentar_nota)

        fun mostrar(item: ItemNota) {
            nome.text = item.nomeNaNota
            conferir.visibility = if (item.conferir && item.alimentoId == null) View.VISIBLE else View.GONE

            // Listener fora antes de marcar: senao a reciclagem do card dispara
            // o clique de outro item.
            incluir.setOnCheckedChangeListener(null)
            incluir.isChecked = item.incluir
            incluir.setOnCheckedChangeListener { _, marcado -> item.incluir = marcado }

            produto.setSimpleItems(catalogo.map { it.name }.toTypedArray())
            produto.setText(item.alimentoNome.orEmpty(), false)
            produto.setOnItemClickListener { _, _, posicao, _ ->
                val escolhido = catalogo[posicao]
                item.alimentoId = escolhido.id
                item.alimentoNome = escolhido.name
                unidadeNaTela(escolhido.unitOfMeasure)?.let { item.unidade = it }
                // Escolher o produto e dizer que quer o item.
                item.incluir = true
                notifyItemChanged(bindingAdapterPosition)
            }

            mostrarQuantidade(item)
            menos.setOnClickListener {
                if (item.quantidade > 1) {
                    item.quantidade -= 1
                    mostrarQuantidade(item)
                }
            }
            mais.setOnClickListener {
                item.quantidade += 1
                mostrarQuantidade(item)
            }

            mostrarValidade(item)
            validade.setOnClickListener { escolherValidade(item) }
        }

        private fun mostrarQuantidade(item: ItemNota) {
            quantidade.text = itemView.context.getString(
                R.string.produto_quantidade_unidade, item.quantidade, item.unidade
            )
        }

        private fun mostrarValidade(item: ItemNota) {
            val contexto = itemView.context
            val data = item.validade?.let { runCatching { ISO.parse(it) }.getOrNull() }

            validade.text = data
                ?.let { contexto.getString(R.string.produto_validade, CURTA.format(it)) }
                ?: contexto.getString(R.string.nota_definir_validade)
        }

        private fun escolherValidade(item: ItemNota) {
            val inicial = Calendar.getInstance().apply {
                item.validade?.let { runCatching { ISO.parse(it) }.getOrNull() }?.let { time = it }
            }

            DatePickerDialog(
                itemView.context,
                { _, ano, mes, dia ->
                    val escolhida = Calendar.getInstance().apply { set(ano, mes, dia) }
                    item.validade = ISO.format(escolhida.time)
                    mostrarValidade(item)
                },
                inicial.get(Calendar.YEAR),
                inicial.get(Calendar.MONTH),
                inicial.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private companion object {
        val ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val CURTA = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
    }
}
