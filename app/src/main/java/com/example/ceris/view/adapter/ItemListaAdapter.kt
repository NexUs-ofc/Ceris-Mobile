package com.example.ceris.view.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.model.CategoriaItem
import com.example.ceris.model.ItemLista
import com.example.ceris.util.Moeda
import com.google.android.material.checkbox.MaterialCheckBox

class ItemListaAdapter(
    private val aoMarcar: (ItemLista, Boolean) -> Unit,
    private val aoSegurar: (ItemLista) -> Unit = {}
) : ListAdapter<ItemListaAdapter.Linha, RecyclerView.ViewHolder>(Diferenca) {

    sealed class Linha {
        data class Cabecalho(val categoria: CategoriaItem) : Linha()

        data class Produto(val item: ItemLista) : Linha()
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Linha.Cabecalho -> TIPO_CABECALHO
        is Linha.Produto -> TIPO_PRODUTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TIPO_CABECALHO) {
            CabecalhoViewHolder(
                inflater.inflate(R.layout.item_lista_cabecalho, parent, false)
            )
        } else {
            ProdutoViewHolder(
                inflater.inflate(R.layout.item_lista_produto, parent, false),
                aoMarcar,
                aoSegurar
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val linha = getItem(position)) {
            is Linha.Cabecalho -> (holder as CabecalhoViewHolder).exibir(linha.categoria)
            is Linha.Produto -> (holder as ProdutoViewHolder).exibir(linha.item)
        }
    }

    class CabecalhoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val texto: TextView = itemView.findViewById(R.id.txt_cabecalho)

        fun exibir(categoria: CategoriaItem) {
            texto.text = categoria.cabecalho()
        }
    }

    class ProdutoViewHolder(
        itemView: View,
        private val aoMarcar: (ItemLista, Boolean) -> Unit,
        private val aoSegurar: (ItemLista) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val caixa: MaterialCheckBox = itemView.findViewById(R.id.check_item)
        private val nome: TextView = itemView.findViewById(R.id.txt_nome_item)
        private val quantidade: TextView = itemView.findViewById(R.id.txt_quantidade_item)
        private val valor: TextView = itemView.findViewById(R.id.txt_valor_item)

        fun exibir(item: ItemLista) {
            val contexto = itemView.context

            nome.text = item.nome
            quantidade.text = item.descricao()
            valor.text = Moeda.formatar(item.valor)

            // Sem listener durante a atribuição: numa View reciclada o setChecked
            // dispararia o callback e marcaria o item errado no servidor.
            caixa.setOnCheckedChangeListener(null)
            caixa.isChecked = item.marcado
            caixa.setOnCheckedChangeListener { _, marcado -> aoMarcar(item, marcado) }
            caixa.contentDescription = item.nome

            val cor = ContextCompat.getColor(
                contexto,
                if (item.marcado) R.color.lista_item_concluido else R.color.md_theme_onBackground
            )
            nome.setTextColor(cor)
            valor.setTextColor(cor)
            nome.paintFlags = if (item.marcado) {
                nome.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                nome.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            itemView.setOnClickListener { caixa.isChecked = !caixa.isChecked }
            itemView.setOnLongClickListener {
                aoSegurar(item)
                true
            }
        }
    }

    // Agrupa por categoria na ordem do enum e intercala os cabeçalhos.
    companion object {
        private const val TIPO_CABECALHO = 0
        private const val TIPO_PRODUTO = 1

        fun agrupar(itens: List<ItemLista>): List<Linha> =
            itens
                .groupBy { it.categoria }
                .toSortedMap()
                .flatMap { (categoria, doGrupo) ->
                    listOf(Linha.Cabecalho(categoria)) + doGrupo.map { Linha.Produto(it) }
                }
    }

    private object Diferenca : DiffUtil.ItemCallback<Linha>() {
        override fun areItemsTheSame(anterior: Linha, novo: Linha): Boolean = when {
            anterior is Linha.Cabecalho && novo is Linha.Cabecalho ->
                anterior.categoria == novo.categoria
            anterior is Linha.Produto && novo is Linha.Produto ->
                anterior.item.id == novo.item.id
            else -> false
        }

        override fun areContentsTheSame(anterior: Linha, novo: Linha): Boolean = anterior == novo
    }
}
