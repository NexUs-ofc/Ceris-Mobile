package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Receita
import com.google.android.material.imageview.ShapeableImageView

/**
 * Lista de receitas salvas.
 *
 * Sem foto ou tempo da Core, o card mostra o icone de receita e esconde o tempo.
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

        private val tempoGrupo: View = itemView.findViewById(R.id.grupo_tempo)
        private val imagem: ShapeableImageView = itemView.findViewById(R.id.img_receita)

        fun exibir(receita: Receita) {
            nome.text = receita.nome
            tempo.text = receita.tempoPreparo
            tempoGrupo.visibility = if (receita.tempoPreparo.isNullOrBlank()) View.GONE else View.VISIBLE

            Glide.with(imagem).clear(imagem)
            if (receita.imagemUrl == null) {
                imagem.setImageResource(R.drawable.ic_menu_chef_hat)
            } else {
                Glide.with(imagem).load(receita.imagemUrl).centerCrop().into(imagem)
            }

            itemView.setOnClickListener { aoTocar(receita) }
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Receita>() {
        override fun areItemsTheSame(anterior: Receita, novo: Receita) = anterior.id == novo.id
        override fun areContentsTheSame(anterior: Receita, novo: Receita) = anterior == novo
    }
}
