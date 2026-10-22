package com.example.ceris.view.adapter

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.util.unidadeNaTela
import com.example.ceris.view.utils.Seletor
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

    /**
     * Rotulo de cada produto do catalogo: nome e marca. O catalogo repete o
     * nome para cada marca ("Alface crespa" aparece varias vezes), e so pelo
     * nome a pessoa nao sabe qual esta escolhendo.
     */
    private var porRotulo: Map<String, FoodResponse> = emptyMap()
    private var rotulos: Array<String> = emptyArray()

    fun trocar(novos: List<ItemNota>, catalogo: List<FoodResponse>) {
        porRotulo = catalogo
            .sortedBy { it.name.lowercase() }
            .associateBy(::rotulo)
        rotulos = porRotulo.keys.toTypedArray()
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
        private val produto: MaterialAutoCompleteTextView = view.findViewById(R.id.campo_produto)
        private val stepper: View = view.findViewById(R.id.stepper_nota)
        private val validade: TextView = view.findViewById(R.id.txt_validade_nota)

        fun mostrar(item: ItemNota) {
            nome.text = item.nomeNaNota

            // Listener fora antes de marcar: senao a reciclagem do card dispara
            // o clique de outro item.
            incluir.setOnCheckedChangeListener(null)
            incluir.isChecked = item.incluir
            incluir.setOnCheckedChangeListener { _, marcado -> item.incluir = marcado }

            // Digitavel: com centenas de produtos, rolar a lista inteira nao da.
            // Filtrado, a posicao clicada nao e a do catalogo; por isso o produto
            // sai do rotulo clicado, nunca do indice.
            produto.setSimpleItems(rotulos)
            val escolhidoAntes = porRotulo.values.firstOrNull { it.id == item.alimentoId }
            produto.setText(escolhidoAntes?.let(::rotulo) ?: item.alimentoNome.orEmpty(), false)
            produto.setOnItemClickListener { lista, _, posicao, _ ->
                val escolhido = porRotulo[lista.getItemAtPosition(posicao) as String] ?: return@setOnItemClickListener
                item.alimentoId = escolhido.id
                item.alimentoNome = escolhido.name
                unidadeNaTela(escolhido.unitOfMeasure)?.let { item.unidade = it }
                // Escolher o produto e dizer que quer o item.
                item.incluir = true
                notifyItemChanged(bindingAdapterPosition)
            }

            // Um Seletor por ligacao: o card reciclado passa a mexer no item novo.
            Seletor(
                stepper,
                inicial = item.quantidade,
                formato = { itemView.context.getString(R.string.produto_quantidade_unidade, it, item.unidade) },
                aoMudar = { item.quantidade = it }
            )

            mostrarValidade(item)
            validade.setOnClickListener { escolherValidade(item) }
        }

        private fun mostrarValidade(item: ItemNota) {
            val contexto = itemView.context
            val data = item.validade?.let { runCatching { ISO.parse(it) }.getOrNull() }

            validade.text = data
                ?.let { contexto.getString(R.string.nota_validade, CURTA.format(it)) }
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

    private fun rotulo(food: FoodResponse): String =
        listOfNotNull(food.name, food.productBrand?.takeIf { it.isNotBlank() }).joinToString(" · ")

    private companion object {
        val ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val CURTA = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
    }
}
