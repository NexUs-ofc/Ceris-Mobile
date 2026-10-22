package com.example.ceris.model

import java.io.Serializable

/**
 * Estabelecimento proximo, como a tela de Locais e o detalhe precisam dele.
 *
 * Os textos vem ja formatados porque quem decide arredondamento e unidade e a
 * origem do dado, nao a View. Serializable para ir inteiro ao detalhe.
 */
data class Local(
    val id: String,
    val nome: String,
    val endereco: String,
    val distancia: String,
    val imagemUrl: String? = null,
    /** Rua, numero, bairro, cidade e UF, para o detalhe. */
    val enderecoCompleto: String = endereco,
    /** "7h às 22h", ou nulo quando a Core nao tem horario. */
    val horario: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Serializable
