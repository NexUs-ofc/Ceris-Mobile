package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Local

/**
 * Lista de estabelecimentos próximos.
 *
 * Como o ProdutoAdapter, usa ListAdapter para atualizar por diferença: quando o
 * endereço muda e a lista é recalculada, só os cards afetados são redesenhados.
 *
 * A foto de perfil da loja vem da Core; o icone de mapa fica como placeholder.
 */
class LocalAdapter(
    private val aoTocar: ((Local) -> Unit)? = null,
) : ListAdapter<Local, LocalAdapter.LocalViewHolder>(Diferenca) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LocalViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_local, parent, false)
        return LocalViewHolder(view, aoTocar)
    }

    override fun onBindViewHolder(
        holder: LocalViewHolder,
        position: Int,
    ) {
        holder.exibir(getItem(position))
    }

    class LocalViewHolder(
        itemView: View,
        private val aoTocar: ((Local) -> Unit)?,
    ) : RecyclerView.ViewHolder(itemView) {
        private val nome: TextView = itemView.findViewById(R.id.txt_nome_local)
        private val endereco: TextView = itemView.findViewById(R.id.txt_endereco_local)
        private val distancia: TextView = itemView.findViewById(R.id.selo_distancia)

        private val imagem: ImageView = itemView.findViewById(R.id.img_local)

        fun exibir(local: Local) {
            nome.text = local.nome.ifBlank { itemView.context.getString(R.string.locais_nome_indisponivel) }
            endereco.text = local.endereco
            endereco.visibility = if (local.endereco.isBlank()) View.GONE else View.VISIBLE
            distancia.text = itemView.context.getString(R.string.locais_distancia, local.distancia)
            distancia.visibility = if (local.distancia.isBlank()) View.GONE else View.VISIBLE
            val photoUrl = local.imagemUrl?.takeIf { it.isNotBlank() }
            if (photoUrl == null) {
                Glide.with(itemView).clear(imagem)
                imagem.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val padding = itemView.resources.getDimensionPixelSize(R.dimen.spacing_m)
                imagem.setPadding(padding, padding, padding, padding)
                imagem.setImageResource(R.drawable.ic_menu_map_pin)
            } else {
                imagem.scaleType = ImageView.ScaleType.CENTER_CROP
                imagem.setPadding(0, 0, 0, 0)
                Glide.with(itemView)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_local_photo_placeholder)
                    .error(R.drawable.ic_local_photo_placeholder)
                    .into(imagem)
            }
            imagem.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            itemView.setOnClickListener(aoTocar?.let { callback -> View.OnClickListener { callback(local) } })
            itemView.isClickable = aoTocar != null
            itemView.isFocusable = aoTocar != null
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Local>() {
        override fun areItemsTheSame(
            anterior: Local,
            novo: Local,
        ) = anterior.id == novo.id

        override fun areContentsTheSame(
            anterior: Local,
            novo: Local,
        ) = anterior == novo
    }
}
