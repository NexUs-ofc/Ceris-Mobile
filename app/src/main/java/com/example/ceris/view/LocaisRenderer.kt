package com.example.ceris.view

import android.view.View
import android.widget.TextView
import com.example.ceris.R
import com.example.ceris.view.adapter.LocalAdapter
import com.example.ceris.viewmodel.LocaisStatus
import com.example.ceris.viewmodel.LocaisUiState

fun renderLocais(
    view: View,
    adapter: LocalAdapter,
    state: LocaisUiState,
) {
    val noAddress =
        if (state.status == LocaisStatus.LOADING) {
            R.string.locais_carregando_endereco
        } else {
            R.string.locais_endereco_indisponivel
        }
    view.findViewById<TextView>(R.id.txt_endereco_atual).text =
        state.address.ifBlank {
            view.context.getString(noAddress)
        }
    val content = state.status == LocaisStatus.CONTENT
    // null limpa sincronicamente antes de mostrar dados de outra conta.
    adapter.submitList(if (content) state.locais else null)
    view.findViewById<View>(R.id.lista_locais).visibility = if (content) View.VISIBLE else View.GONE
    val loading = state.status in setOf(LocaisStatus.IDLE, LocaisStatus.LOADING)
    view.findViewById<View>(R.id.carregando_locais).visibility = if (loading) View.VISIBLE else View.GONE
    view.findViewById<View>(R.id.estado_locais).visibility = if (content) View.GONE else View.VISIBLE
    view.findViewById<TextView>(R.id.mensagem_locais).setText(
        when (state.status) {
            LocaisStatus.IDLE, LocaisStatus.LOADING -> R.string.locais_carregando
            LocaisStatus.EMPTY -> R.string.locais_vazio
            LocaisStatus.ADDRESS_MISSING -> R.string.locais_sem_endereco
            LocaisStatus.NOT_GEOCODED -> R.string.locais_sem_coordenadas
            LocaisStatus.AUTH_REQUIRED -> R.string.locais_sessao_invalida
            else -> R.string.locais_falha
        },
    )
    view.findViewById<TextView>(R.id.btn_tentar_locais).apply {
        visibility = if (state.status in setOf(LocaisStatus.ERROR, LocaisStatus.AUTH_REQUIRED)) View.VISIBLE else View.GONE
        setText(if (state.status == LocaisStatus.AUTH_REQUIRED) R.string.locais_entrar else R.string.locais_tentar)
    }
}
