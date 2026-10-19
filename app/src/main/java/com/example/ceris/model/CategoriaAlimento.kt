package com.example.ceris.model

/**
 * Categorias de alimento da Core, cada uma com um codigo GPC que ela aceita.
 *
 * A Core so cadastra produto novo com um codigo GPC alimentar conhecido
 * (categorization/gpc-brick-categories.csv) e deriva a categoria dele. O app
 * nao conhece GPC; a pessoa escolhe a categoria e o app manda o primeiro
 * codigo daquela categoria no CSV da Core (conferido em 2026-10-19).
 */
enum class CategoriaAlimento(val rotulo: String, val gpc: String) {
    ACUCARES("Açúcares e Adoçantes", "10000043"),
    BEBIDAS("Bebidas", "10000114"),
    BISCOITOS("Biscoitos e Snacks", "10000160"),
    CARNES("Carnes, Aves e Pescados", "10000016"),
    CEREAIS("Cereais e Grãos", "10000007"),
    CONGELADOS("Congelados", "10000002"),
    CONSERVAS("Conservas e Enlatados", "10000003"),
    DOCES("Doces e Sobremesas", "10000045"),
    FARINHAS("Farinhas, Fermentos e Misturas", "10000068"),
    HORTIFRUTI("Frutas, Verduras e Legumes", "10000205"),
    LATICINIOS("Laticínios e Ovos", "10000025"),
    MASSAS("Massas", "10000240"),
    MOLHOS("Molhos, Condimentos e Temperos", "10000048"),
    OLEOS("Óleos e Gorduras", "10000040"),
    PADARIA("Padaria", "10000164"),
    PRONTOS("Pratos Prontos", "10000255"),
    OUTROS("Outros", "10000104");

    companion object {
        fun rotulos(): List<String> = entries.map { it.rotulo }

        fun porRotulo(rotulo: String): CategoriaAlimento? = entries.firstOrNull { it.rotulo == rotulo }
    }
}
