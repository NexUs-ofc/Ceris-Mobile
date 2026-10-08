package com.example.ceris.model

// Nao existe no contrato da Core: a escolha fica no aparelho (ListaLocalStore).
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
        val PADRAO = HORTIFRUTI

        fun porRotulo(rotulo: String?): CategoriaItem =
            entries.firstOrNull { it.rotulo.equals(rotulo, ignoreCase = true) } ?: PADRAO

        fun porNome(nome: String?): CategoriaItem =
            entries.firstOrNull { it.name == nome } ?: PADRAO

        fun rotulos(): List<String> = entries.map { it.rotulo }
    }
}
