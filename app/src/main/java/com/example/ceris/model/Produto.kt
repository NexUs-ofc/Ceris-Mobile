package com.example.ceris.model

/**
 * Produto do estoque, como o card da Tela Inicial precisa dele.
 *
 * A validade fica como texto já formatado porque quem decide o formato é a
 * origem do dado, não a View. O card só exibe.
 */
data class Produto(
    val id: String,
    val nome: String,
    val quantidade: Int,
    val validade: String,
    val situacao: SituacaoProduto,
    val imagemUrl: String? = null
)

/**
 * Situação do produto conforme a validade. Define o selo colorido do card.
 */
enum class SituacaoProduto {
    FRESCO,
    VENCENDO,
    VENCIDO
}
