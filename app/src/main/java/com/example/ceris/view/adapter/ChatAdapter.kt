package com.example.ceris.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R

/** Uma linha da conversa. */
sealed class MensagemChat {
    data class DaCerita(val texto: String) : MensagemChat()

    data class DeQuemUsa(val texto: String) : MensagemChat()

    data class Divisor(val texto: String) : MensagemChat()
}

/**
 * Conversa da Cerita.
 *
 * Três layouts, escolhidos pelo tipo da linha: balão da Cerita à esquerda,
 * balão de quem usa à direita e o divisor de dia no meio. A lista é guardada
 * aqui mesmo porque a tela ainda não fala com a IA — quando falar, isto vira
 * submitList sem mexer nos layouts.
 */
class ChatAdapter : RecyclerView.Adapter<ChatAdapter.Linha>() {

    private companion object {
        const val CERITA = 0
        const val QUEM_USA = 1
        const val DIVISOR = 2
    }

    private val mensagens = mutableListOf<MensagemChat>()

    class Linha(view: View) : RecyclerView.ViewHolder(view) {
        val texto: TextView = view.findViewById(R.id.texto)
    }

    fun trocar(novas: List<MensagemChat>) {
        mensagens.clear()
        mensagens.addAll(novas)
        notifyDataSetChanged()
    }

    /** @return a posição da nova linha, para a tela rolar até ela. */
    fun acrescentar(mensagem: MensagemChat): Int {
        mensagens.add(mensagem)
        notifyItemInserted(mensagens.lastIndex)
        return mensagens.lastIndex
    }

    override fun getItemViewType(position: Int): Int = when (mensagens[position]) {
        is MensagemChat.DaCerita -> CERITA
        is MensagemChat.DeQuemUsa -> QUEM_USA
        is MensagemChat.Divisor -> DIVISOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha {
        val layout = when (viewType) {
            CERITA -> R.layout.item_chat_bot
            QUEM_USA -> R.layout.item_chat_usuario
            else -> R.layout.item_chat_data
        }

        return Linha(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: Linha, position: Int) {
        holder.texto.text = when (val mensagem = mensagens[position]) {
            is MensagemChat.DaCerita -> mensagem.texto
            is MensagemChat.DeQuemUsa -> mensagem.texto
            is MensagemChat.Divisor -> mensagem.texto
        }
    }

    override fun getItemCount(): Int = mensagens.size
}
