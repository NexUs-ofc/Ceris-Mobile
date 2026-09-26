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
import com.example.ceris.R
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto

/**
 * Lista de produtos do estoque.
 *
 * Usa ListAdapter para que a lista se atualize por diferença, em vez de
 * redesenhar tudo a cada mudança de filtro.
 *
 * A foto do produto não é carregada aqui: o projeto ainda não tem biblioteca de
 * imagem remota. Enquanto não tiver, o card mostra o espaço da imagem vazio.
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

        @Suppress("unused")
        private val imagem: ImageView = itemView.findViewById(R.id.img_produto)

        fun exibir(produto: Produto) {
            val contexto = itemView.context

            nome.text = produto.nome
            validade.text = contexto.getString(R.string.produto_validade, produto.validade)
            quantidade.text = contexto.getString(R.string.produto_quantidade, produto.quantidade)

            val (rotulo, fundo, texto) = aparencia(produto.situacao)
            selo.setText(rotulo)
            selo.backgroundTintList = ContextCompat.getColorStateList(contexto, fundo)
            selo.setTextColor(ContextCompat.getColor(contexto, texto))

            itemView.setOnClickListener { aoTocar(produto) }
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
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<Produto>() {
        override fun areItemsTheSame(anterior: Produto, novo: Produto) = anterior.id == novo.id
        override fun areContentsTheSame(anterior: Produto, novo: Produto) = anterior == novo
    }
}
