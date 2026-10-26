package com.example.ceris.util

fun mensagemDeErroReceita(
    codigo: Int,
    corpo: String?,
): String =
    when (codigo) {
        403 -> "Você não tem acesso a esta receita."
        404 -> "Esta receita não está mais disponível."
        else -> mensagemDeErro(codigo, corpo)
    }
