package com.example.ceris.model

import java.io.Serializable

/**
 * Receita salva, como a lista e o detalhe precisam dela.
 *
 * A Core ainda nao manda foto, tempo nem dificuldade: o que ela nao tem fica
 * nulo e a tela esconde o pedaco correspondente, em vez de inventar valor.
 * Serializable para ir inteira ao detalhe.
 */
data class Receita(
    val id: String,
    val nome: String,
    val tempoPreparo: String? = null,
    val imagemUrl: String? = null,
    val porcoes: Int? = null,
    /** "500g de peito de frango": quantidade e nome do alimento. */
    val ingredientes: List<String> = emptyList(),
    val passos: List<String> = emptyList()
) : Serializable
