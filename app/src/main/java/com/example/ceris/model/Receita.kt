package com.example.ceris.model

data class Receita(
    val id: String,
    val nome: String,
    val tempoPreparo: String? = null,
    val imagemUrl: String? = null,
    val porcoes: Int? = null,
    val dificuldade: String? = null,
    val ingredientes: List<String> = emptyList(),
    val passos: List<String> = emptyList(),
)
