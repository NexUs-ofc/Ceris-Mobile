package com.example.ceris.util

import java.text.NumberFormat
import java.util.Locale

// Locale fixo: com o do aparelho, quem usa o telefone em ingles veria "$65.29".
object Moeda {

    private val BRASIL: Locale = Locale.forLanguageTag("pt-BR")

    private val formatador: NumberFormat = NumberFormat.getCurrencyInstance(BRASIL)

    // O espaco e normalizado porque o ICU usa o fino.
    fun formatar(valor: Double): String =
        formatador.format(valor).replace('\u00A0', ' ')

    // Aceita "12,90" e "12.90"; campo vazio vale zero.
    fun interpretar(texto: String?): Double {
        if (texto.isNullOrBlank()) return 0.0

        val limpo = texto.replace(Regex("[^0-9,.]"), "")

        // So com virgula o ponto e separador de milhar ("1.234,56"). Sem ela,
        // o ponto e o decimal ("12.90") e apaga-lo daria 1290.
        val normalizado = if (limpo.contains(',')) {
            limpo.replace(".", "").replace(',', '.')
        } else {
            limpo
        }

        return normalizado.toDoubleOrNull() ?: 0.0
    }
}
