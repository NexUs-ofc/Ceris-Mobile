package com.example.ceris.model

import com.example.ceris.model.dto.FoodLookupResponse
import com.example.ceris.model.dto.ReceiptItem
import kotlin.math.ceil

data class RevisaoNota(val itens: List<ItemNota>, val ignorados: Int, val indisponiveis: Int)

/** Índices preservam repetidos e ordem da nota; nunca casam um produto por posição de resposta. */
fun revisarNota(originais: List<ReceiptItem>, lookup: FoodLookupResponse): RevisaoNota {
    val porIndice = lookup.items.groupBy { it.index }
    val itens = originais.mapIndexedNotNull { index, item ->
        val resultado = porIndice[index]?.singleOrNull() ?: return@mapIndexedNotNull null
        val produto = resultado.product?.takeIf { resultado.status == "FOUND" && it.valido() }
            ?: return@mapIndexedNotNull null
        ItemNota(
            nomeNaNota = item.name, alimentoId = null,
            alimentoNome = listOfNotNull(produto.name, produto.productBrand,
                "${produto.packageQuantity} ${produto.unitOfMeasure}").joinToString(" · "),
            quantidade = ceil(item.quantity).toInt().coerceAtLeast(1),
            // Stock.quantity é número de embalagens; o volume/peso fica no produto.
            unidade = "un", validade = item.expirationDate,
            incluir = true, conferir = true, produtoEncontrado = produto,
        )
    }
    val indisponiveis = originais.indices.count { index ->
        val result = porIndice[index]?.singleOrNull()
        result == null || result.status == "UNAVAILABLE"
    }
    return RevisaoNota(itens, originais.size - itens.size, indisponiveis)
}
