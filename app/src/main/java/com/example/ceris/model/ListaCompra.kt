package com.example.ceris.model

/**
 * Lista de compras, como o card da tela de Minhas Listas precisa dela.
 *
 * O valor total vem já formatado ("R$ 65,29") porque moeda depende de locale, e
 * essa decisão é da origem do dado, não da View.
 */
data class ListaCompra(
    val id: String,
    val nome: String,
    val quantidadeItens: Int,
    val categoria: String,
    /** Percentual concluído, de 0 a 100. */
    val progresso: Int,
    val valorTotal: String
)
