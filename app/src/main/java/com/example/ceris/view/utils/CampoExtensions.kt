package com.example.ceris.view.utils

import com.google.android.material.textfield.TextInputLayout

/**
 * Erro embaixo do campo, no lugar do Toast.
 *
 * O Toast aparecia por cima da tela, sumia sozinho e não dizia qual campo
 * estava errado — quem errava o e-mail e a senha ao mesmo tempo via uma frase
 * genérica e tinha de adivinhar. No Figma a borda do campo fica vermelha e a
 * mensagem aparece logo abaixo dele, e é isso que o TextInputLayout já sabe
 * fazer sozinho com setError.
 *
 * O isErrorEnabled entra antes da mensagem para o espaço ser reservado de uma
 * vez: na ordem inversa o formulário dá um pulo quando o texto aparece.
 */
fun TextInputLayout.mostrarErro(mensagem: String?) {
    if (mensagem == null) {
        error = null
        isErrorEnabled = false
    } else {
        isErrorEnabled = true
        error = mensagem
    }
}

/** Limpa o estado de erro de todos os campos, antes de uma nova tentativa. */
fun limparErros(vararg campos: TextInputLayout) {
    campos.forEach { it.mostrarErro(null) }
}
