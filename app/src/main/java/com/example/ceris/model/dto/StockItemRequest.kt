package com.example.ceris.model.dto

/** Corpo de POST /api/stock na Core. */
data class StockItemRequest(
    val foodId: Int,
    /** Inteiro e positivo: e o que a Core valida. */
    val quantity: Int,
    /** yyyy-MM-dd, ou nulo para produto sem validade. */
    val expiryDate: String? = null
)
