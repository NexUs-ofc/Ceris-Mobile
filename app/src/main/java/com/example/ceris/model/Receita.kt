package com.example.ceris.model

/**
 * Receita salva, como o card da tela de Receitas precisa dela.
 *
 * O tempo de preparo vem já formatado ("45 min", "1h 20min") porque quem decide
 * a forma é a origem do dado, não a View. O card só exibe.
 */
data class Receita(
    val id: String,
    val nome: String,
    val tempoPreparo: String,
    val imagemUrl: String? = null
)
