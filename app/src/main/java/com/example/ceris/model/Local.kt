package com.example.ceris.model

/**
 * Estabelecimento próximo, como a tela de Locais precisa dele.
 *
 * A distância vem já formatada porque quem decide arredondamento e unidade é a
 * origem do dado, não a View. O card só exibe.
 */
data class Local(
    val id: String,
    val nome: String,
    val endereco: String,
    val distancia: String,
    val imagemUrl: String? = null
)
