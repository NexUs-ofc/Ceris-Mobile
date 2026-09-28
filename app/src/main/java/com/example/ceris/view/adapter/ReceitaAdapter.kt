package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.model.Receita
import com.google.android.material.imageview.ShapeableImageView

/**
 * Lista de receitas salvas.
 *
 * A foto não é carregada aqui: o projeto ainda não tem biblioteca de imagem
 * remota. Enquanto não tiver, a moldura fica com a cor de superfície.
 */
class ReceitaAdapter(
    private val aoTocar: (Receita) -> Unit = {}
) : ListAdapter<Receita, ReceitaAdapter.ReceitaViewHolder>(Diferenca) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receita, parent, false)
        return ReceitaViewHolder(view, aoTocar)
    }

    override fun onBindViewHolder(holder: ReceitaViewHolder, position: Int) {
        holder.exibir(getItem(position))
    }

    class ReceitaViewHolder(
        itemView: View,
        private val aoTocar: (Receita) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nome: TextView = itemView.findViewById(R.id.txt_nome_receita)
        private val tempo: TextView = itemView.findViewById(R.id.txt_tempo_receita)

        @Suppress("unused")
        private val imagem: ShapeableImageView = itemView.findViewById(R.id.img_receita)

        fun exibir(receita: Receita) {
            nome.text = receita.nome
            tempo.text = receita.tempoPreparo

            itemView.setOnClickListener { aoTocar(receita) }
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Receita>() {
        override fun areItemsTheSame(anterior: Receita, novo: Receita) = anterior.id == novo.id
        override fun areContentsTheSame(anterior: Receita, novo: Receita) = anterior == novo
    }
}
