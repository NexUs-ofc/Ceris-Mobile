package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ceris.R
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto

/**
 * Lista de produtos do estoque.
 *
 * Usa ListAdapter para que a lista se atualize por diferença, em vez de
 * redesenhar tudo a cada mudança de filtro.
 *
 * A foto vem da Core (foodImageUrl). Sem foto, o card mostra o ícone de
 * produto no lugar, para a moldura não ficar vazia.
 */
class ProdutoAdapter(
    private val aoTocar: (Produto) -> Unit = {}
) : ListAdapter<Produto, ProdutoAdapter.ProdutoViewHolder>(Diferenca) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ProdutoViewHolder(view, aoTocar)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        holder.exibir(getItem(position))
    }

    class ProdutoViewHolder(
        itemView: View,
        private val aoTocar: (Produto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nome: TextView = itemView.findViewById(R.id.txt_nome)
        private val validade: TextView = itemView.findViewById(R.id.txt_validade)
        private val quantidade: TextView = itemView.findViewById(R.id.txt_quantidade)
        private val selo: TextView = itemView.findViewById(R.id.selo_situacao)

        private val imagem: ImageView = itemView.findViewById(R.id.img_produto)

        fun exibir(produto: Produto) {
            val contexto = itemView.context

            nome.text = produto.nome
            validade.text = if (produto.validade.isBlank()) {
                contexto.getString(R.string.produto_sem_validade)
            } else {
                contexto.getString(R.string.produto_validade, produto.validade)
            }
            quantidade.text = contexto.getString(
                R.string.produto_quantidade_unidade, produto.quantidade, produto.unidade
            )

            itemView.setOnClickListener { aoTocar(produto) }

            // Sempre limpa antes: o card e reciclado e mostraria a foto de outro produto.
            Glide.with(imagem).clear(imagem)
            if (produto.imagemUrl.isNullOrBlank()) {
                imagem.setImageResource(R.drawable.ph_produto)
            } else {
                Glide.with(imagem)
                    .load(produto.imagemUrl)
                    .placeholder(R.drawable.ph_produto)
                    .error(R.drawable.ph_produto)
                    .fitCenter()
                    .into(imagem)
            }

            val (rotulo, fundo, texto) = aparencia(produto.situacao) ?: run {
                selo.visibility = View.GONE
                return
            }

            selo.visibility = View.VISIBLE
            selo.setText(rotulo)
            selo.backgroundTintList = ContextCompat.getColorStateList(contexto, fundo)
            selo.setTextColor(ContextCompat.getColor(contexto, texto))
        }

        /**
         * Rótulo e cores do selo. Ficam juntos de propósito: os três sempre
         * mudam em conjunto, e separá-los abriria espaço para um selo verde
         * escrito "Vencido".
         */
        private fun aparencia(situacao: SituacaoProduto) = when (situacao) {
            SituacaoProduto.FRESCO -> Triple(
                R.string.produto_fresco, R.color.selo_fresco_fundo, R.color.selo_fresco_texto
            )
            SituacaoProduto.VENCENDO -> Triple(
                R.string.produto_vencendo, R.color.selo_vencendo_fundo, R.color.selo_vencendo_texto
            )
            SituacaoProduto.VENCIDO -> Triple(
                R.string.produto_vencido, R.color.selo_vencido_fundo, R.color.selo_vencido_texto
            )
            SituacaoProduto.SEM_VALIDADE -> null
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Produto>() {
        override fun areItemsTheSame(anterior: Produto, novo: Produto) = anterior.id == novo.id
        override fun areContentsTheSame(anterior: Produto, novo: Produto) = anterior == novo
    }
}
