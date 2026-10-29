package com.example.ceris.model

data class ListaCompra(
    val id: String,
    val nome: String,
    val quantidadeItens: Int,
    // Mantido como rótulo de apresentação das listas atuais.
    val categoria: String,
    val progresso: Int,
    val valorTotal: String,
    val data: String? = null
)
