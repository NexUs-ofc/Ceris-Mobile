package com.example.ceris.model

import com.example.ceris.model.dto.FoodLookupResponse
import com.example.ceris.model.dto.FoodLookupProduct
import com.example.ceris.model.dto.ReceiptItem
import kotlin.math.ceil

data class RevisaoNota(
    val itens: List<ItemNota>,
    val ignorados: Int,
    val indisponiveis: Int,
    val nomesIgnorados: List<String> = emptyList(),
    val nomesNaoEncontrados: List<String> = emptyList(),
)

/** Índices preservam repetidos e ordem da nota; nunca casam um produto por posição de resposta. */
fun revisarNota(originais: List<ReceiptItem>, lookup: FoodLookupResponse): RevisaoNota {
    val porIndice = lookup.items.groupBy { it.index }
    val itens = originais.mapIndexedNotNull { index, item ->
        val resultado = porIndice[index]?.singleOrNull() ?: return@mapIndexedNotNull null
        val produto = resultado.product?.takeIf { resultado.status == "FOUND" && it.valido() }
            ?: return@mapIndexedNotNull null
        ItemNota(
            nomeNaNota = produto.name?.takeIf { it.isNotBlank() } ?: item.name, alimentoId = null,
            alimentoNome = listOfNotNull(produto.name, produto.productBrand,
                "${produto.packageQuantity} ${produto.unitOfMeasure}").joinToString(" · "),
            quantidade = quantidadeDeEmbalagens(item, produto),
            // A quantidade do estoque conta embalagens; massa/volume é o conteúdo de cada uma.
            unidade = "un", validade = item.expirationDate,
            incluir = true, conferir = true, produtoEncontrado = produto,
        )
    }
    val naoEncontrados = originais.mapIndexedNotNull { index, item ->
        val result = porIndice[index]?.singleOrNull()
        val produto = result?.product?.takeIf { result.status == "FOUND" && it.valido() }
        item.name.takeIf { produto == null && result != null && result.status != "UNAVAILABLE" }
    }
    val indisponiveis = originais.indices.count { index ->
        val result = porIndice[index]?.singleOrNull()
        result == null || result.status == "UNAVAILABLE"
    }
    val ignorados = originais.size - itens.size
    val nomesIgnorados = originais.mapIndexedNotNull { index, item ->
        val result = porIndice[index]?.singleOrNull()
        val produto = result?.product?.takeIf { result.status == "FOUND" && it.valido() }
        (item.rawText?.trim()?.takeIf { it.isNotEmpty() } ?: item.name).takeIf { produto == null }
    }
    return RevisaoNota(itens, ignorados, indisponiveis, nomesIgnorados, naoEncontrados)
}

/** Converte a linha ponderada da nota para a contagem inteira de pacotes do estoque. */
private fun quantidadeDeEmbalagens(item: ReceiptItem, produto: FoodLookupProduct): Int {
    val quantidade = item.quantity.takeIf { it.isFinite() && it > 0 } ?: return 1
    val unidadeDaNota = unidadeFisica(item.unit) ?: return ceil(quantidade).toInt().coerceAtLeast(1)
    val unidadeDaEmbalagem = unidadeFisica(produto.unitOfMeasure) ?: return ceil(quantidade).toInt().coerceAtLeast(1)
    if (unidadeDaNota.first != unidadeDaEmbalagem.first) return ceil(quantidade).toInt().coerceAtLeast(1)

    val conteudo = produto.packageQuantity?.takeIf { it.isFinite() && it > 0 }
        ?: return ceil(quantidade).toInt().coerceAtLeast(1)
    val quantidadeNaUnidadeBase = quantidade * unidadeDaNota.second
    val conteudoNaUnidadeBase = conteudo * unidadeDaEmbalagem.second
    return ceil(quantidadeNaUnidadeBase / conteudoNaUnidadeBase).toInt().coerceAtLeast(1)
}

/** Dimensão e fator para g/ml; nulo indica unidade de contagem ou desconhecida. */
private fun unidadeFisica(valor: String?): Pair<String, Double>? = when (valor?.trim()?.lowercase()) {
    "g", "gr", "grama", "gramas" -> "massa" to 1.0
    "kg", "quilo", "quilos", "quilograma", "quilogramas" -> "massa" to 1000.0
    "mg", "miligramas" -> "massa" to 0.001
    "ml", "mililitro", "mililitros" -> "volume" to 1.0
    "l", "lt", "litro", "litros" -> "volume" to 1000.0
    else -> null
}
