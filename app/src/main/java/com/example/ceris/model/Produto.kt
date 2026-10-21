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
    val imagemUrl: String? = null,
    /** Como a Core manda: "un", "kg", "l"... Sem ela, 3 kg viraria "3 un". */
    val unidade: String = "un",
    /** Do catalogo da Core. Nulo quando o catalogo nao respondeu. */
    val categoria: String? = null,
    /** Marca do produto na Core. Usada para achar a tabela nutricional. */
    val marca: String? = null
)

/**
 * Situação do produto conforme a validade. Define o selo colorido do card.
 */
enum class SituacaoProduto {
    FRESCO,
    VENCENDO,
    VENCIDO,

    /** Cadastrado sem data: nao ha selo nem aviso a dar. */
    SEM_VALIDADE
}
