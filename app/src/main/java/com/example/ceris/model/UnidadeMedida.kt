package com.example.ceris.model

// `api` bate caractere a caractere com o enum UnitOfMeasure da Core.
enum class UnidadeMedida(
    val api: String,
    private val singular: String,
    private val plural: String
) {
    UNIDADE("unit", "unidade", "unidades"),
    GRAMA("g", "grama", "gramas"),
    QUILO("kg", "quilo", "quilos"),
    MILILITRO("ml", "mililitro", "mililitros"),
    LITRO("l", "litro", "litros");

    fun rotulo(): String = plural.replaceFirstChar { it.uppercase() }

    fun rotuloPara(quantidade: Double): String =
        if (quantidade == 1.0) singular else plural

    companion object {
        fun porApi(valor: String?): UnidadeMedida =
            entries.firstOrNull { it.api.equals(valor, ignoreCase = true) } ?: UNIDADE

        fun porRotulo(rotulo: String?): UnidadeMedida =
            entries.firstOrNull { it.rotulo().equals(rotulo, ignoreCase = true) } ?: UNIDADE

        fun rotulos(): List<String> = entries.map { it.rotulo() }
    }
}
