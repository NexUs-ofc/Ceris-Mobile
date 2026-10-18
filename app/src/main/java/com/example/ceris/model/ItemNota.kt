package com.example.ceris.model

/**
 * Um item lido da nota, na tela de revisao. Mutavel de proposito: a pessoa
 * marca, troca o produto, ajusta quantidade e validade antes de salvar.
 */
data class ItemNota(
    /** Como veio impresso na nota, ex.: "ALFACE CRESPA UN". */
    val nomeNaNota: String,
    /** Produto do catalogo da Core. Sem ele, a Core nao aceita o item. */
    var alimentoId: Int?,
    var alimentoNome: String?,
    var quantidade: Int,
    var unidade: String,
    /** yyyy-MM-dd, ou nulo. */
    var validade: String?,
    var incluir: Boolean,
    /** A leitura ficou em duvida: a tela destaca para a pessoa conferir. */
    val conferir: Boolean
)
