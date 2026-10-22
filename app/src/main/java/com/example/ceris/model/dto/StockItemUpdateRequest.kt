package com.example.ceris.model.dto

/** PATCH /api/stock/{id} da Core: campo nulo fica como esta. */
data class StockItemUpdateRequest(
    val quantity: Int? = null,
    val expiryDate: String? = null
)
