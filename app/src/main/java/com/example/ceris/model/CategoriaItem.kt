package com.example.ceris.model

// Categorias antigas da lista sao preservadas para exibicao, sem selecao manual.
// A ordem das constantes e a ordem dos grupos na tela.
enum class CategoriaItem(val rotulo: String) {
    HORTIFRUTI("Hortifruti"),
    DESPENSA("Despensa"),
    ACOUGUE("Açougue"),
    LATICINIOS("Laticínios"),
    PADARIA("Padaria"),
    BEBIDAS("Bebidas"),
    LIMPEZA("Limpeza"),
    HIGIENE("Higiene"),
    OUTROS("Outros");

    fun cabecalho(): String = rotulo.uppercase()

    companion object {
        val PADRAO = OUTROS

        fun porNome(nome: String?): CategoriaItem =
            entries.firstOrNull { it.name == nome } ?: PADRAO
    }
}
