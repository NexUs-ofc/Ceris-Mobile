package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.model.ListaCompra
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Lista de listas de compras.
 */
class ListaAdapter(
    private val aoTocar: (ListaCompra) -> Unit = {}
) : ListAdapter<ListaCompra, ListaAdapter.ListaViewHolder>(Diferenca) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lista, parent, false)
        return ListaViewHolder(view, aoTocar)
    }

    override fun onBindViewHolder(holder: ListaViewHolder, position: Int) {
        holder.exibir(getItem(position))
    }

    class ListaViewHolder(
        itemView: View,
        private val aoTocar: (ListaCompra) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nome: TextView = itemView.findViewById(R.id.txt_nome_lista)
        private val itens: TextView = itemView.findViewById(R.id.txt_itens)
        private val categoria: TextView = itemView.findViewById(R.id.txt_categoria)
        private val percentual: TextView = itemView.findViewById(R.id.txt_percentual)
        private val valor: TextView = itemView.findViewById(R.id.txt_valor)
        private val barra: LinearProgressIndicator = itemView.findViewById(R.id.barra_progresso)

        fun exibir(lista: ListaCompra) {
            val contexto = itemView.context

            nome.text = lista.nome
            itens.text = contexto.getString(R.string.lista_itens, lista.quantidadeItens)
            categoria.text = contexto.getString(R.string.lista_categoria, lista.categoria)
            percentual.text = contexto.getString(R.string.lista_percentual, lista.progresso)
            valor.text = lista.valorTotal

            // Sem animação: numa lista reciclada ela animaria do valor do item
            // anterior, que não tem relação com este.
            barra.setProgressCompat(lista.progresso, false)

            itemView.setOnClickListener { aoTocar(lista) }
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<ListaCompra>() {
        override fun areItemsTheSame(anterior: ListaCompra, novo: ListaCompra) =
            anterior.id == novo.id

        override fun areContentsTheSame(anterior: ListaCompra, novo: ListaCompra) =
            anterior == novo
    }
}
