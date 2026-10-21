package com.example.ceris.util

/**
 * Unidade como o usuario ve. A Core guarda o enum (g, kg, ml, l, unit), e a
 * tela mostra abreviacao em portugues: "unit" vira "un".
 */
fun unidadeNaTela(bruta: String?): String? {
    val limpa = bruta?.trim()?.lowercase().orEmpty()

    return when (limpa) {
        "" -> null
        "unit", "unidade", "unidades", "un" -> "un"
        else -> limpa
    }
}
