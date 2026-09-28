package com.example.ceris.model.dto

data class ShoppingListItemResponse(
    val id: String,
    val name: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val checked: Boolean,
    val foodId: Int? = null
)
