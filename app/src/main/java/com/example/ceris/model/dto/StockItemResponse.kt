package com.example.ceris.model.dto

/** Item do estoque como a Core devolve em GET /api/stock. */
data class StockItemResponse(
    val id: Int,
    val foodId: Int? = null,
    val foodName: String? = null,
    val productBrand: String? = null,
    val unitOfMeasure: String? = null,
    val quantity: Int = 0,
    /** ISO yyyy-MM-dd, ou nulo quando o produto nao tem validade cadastrada. */
    val expiryDate: String? = null
)
