package com.example.ceris.model.dto

/** Alimento do catalogo da Core (GET /api/stock/foods). */
data class FoodResponse(
    val id: Int,
    val name: String,
    val categoryName: String? = null,
    val productBrand: String? = null,
    val foodImageUrl: String? = null,
    val packageQuantity: Double? = null,
    val unitOfMeasure: String? = null
)
