package com.example.ceris.model

/**
 * Estabelecimento próximo, como a tela de Locais precisa dele.
 *
 * O mapper formata a distancia em pt-BR, incluindo m/km. Texto vazio indica
 * dado ausente e imagemUrl permanece nula quando a loja nao tem foto cadastrada.
 */
data class Local(
    val id: String,
    val nome: String,
    val endereco: String,
    val distancia: String,
    val imagemUrl: String? = null,
)
