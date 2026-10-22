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
import com.example.ceris.model.Local
import com.google.android.material.imageview.ShapeableImageView

/**
 * Lista de estabelecimentos próximos.
 *
 * Como o ProdutoAdapter, usa ListAdapter para atualizar por diferença: quando o
 * endereço muda e a lista é recalculada, só os cards afetados são redesenhados.
 *
 * A foto vem da Core (storeImageUrl); sem ela, a moldura mostra o icone de loja.
 */
class LocalAdapter(
    private val aoTocar: (Local) -> Unit = {}
) : ListAdapter<Local, LocalAdapter.LocalViewHolder>(Diferenca) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_local, parent, false)
        return LocalViewHolder(view, aoTocar)
    }

    override fun onBindViewHolder(holder: LocalViewHolder, position: Int) {
        holder.exibir(getItem(position))
    }

    class LocalViewHolder(
        itemView: View,
        private val aoTocar: (Local) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nome: TextView = itemView.findViewById(R.id.txt_nome_local)
        private val endereco: TextView = itemView.findViewById(R.id.txt_endereco_local)
        private val distancia: TextView = itemView.findViewById(R.id.selo_distancia)

        private val imagem: ShapeableImageView = itemView.findViewById(R.id.img_local)

        fun exibir(local: Local) {
            nome.text = local.nome
            endereco.text = local.endereco
            distancia.text = itemView.context.getString(R.string.locais_distancia, local.distancia)
            distancia.visibility = if (local.distancia.isBlank()) View.GONE else View.VISIBLE

            Glide.with(imagem).clear(imagem)
            if (local.imagemUrl == null) {
                imagem.setImageResource(R.drawable.ic_menu_store)
            } else {
                Glide.with(imagem).load(local.imagemUrl).placeholder(R.drawable.ic_menu_store).centerCrop().into(imagem)
            }

            itemView.setOnClickListener { aoTocar(local) }
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Local>() {
        override fun areItemsTheSame(anterior: Local, novo: Local) = anterior.id == novo.id
        override fun areContentsTheSame(anterior: Local, novo: Local) = anterior == novo
    }
}
