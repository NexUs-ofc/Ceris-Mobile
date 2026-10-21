package com.example.ceris.model

/** Tabela nutricional por 100 g, como o detalhe mostra. Nulo = a fonte nao tinha o dado. */
data class Nutricao(
    val imagemUrl: String?,
    val energiaKcal: Double?,
    val carboidratos: Double?,
    val proteinas: Double?,
    val gorduras: Double?,
    val gordurasSaturadas: Double?,
    val fibras: Double?,
    /** Em gramas, como a fonte entrega; a tela converte para mg. */
    val sodioGramas: Double?
)
