package com.example.ceris.model

data class ListaCompra(
    val id: String,
    val nome: String,
    val quantidadeItens: Int,
    // "Compras" ou "Eventos", derivado do eventId.
    val categoria: String,
    val progresso: Int,
    val valorTotal: String
)
