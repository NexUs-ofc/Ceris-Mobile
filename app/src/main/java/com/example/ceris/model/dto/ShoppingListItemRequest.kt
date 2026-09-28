package com.example.ceris.model.dto

data class ShoppingListItemRequest(
    val name: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val foodId: Int? = null
)
