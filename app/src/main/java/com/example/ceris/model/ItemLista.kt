package com.example.ceris.model

// `valor` e o total do item, nao o preco unitario: a soma deles e o total estimado.
data class ItemLista(
    val id: String,
    val nome: String,
    val quantidade: Double,
    val unidade: UnidadeMedida,
    val categoria: CategoriaItem,
    val valor: Double,
    val marcado: Boolean
) {
    fun quantidadeFormatada(): String =
        if (quantidade % 1.0 == 0.0) {
            quantidade.toLong().toString()
        } else {
            quantidade.toString().replace('.', ',')
        }

    fun descricao(): String = "${quantidadeFormatada()} ${unidade.rotuloPara(quantidade)}"
}
