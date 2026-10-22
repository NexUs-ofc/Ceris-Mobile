package com.example.ceris.model.dto

data class RecipeResponse(
    val id: String,
    val title: String? = null,
    val servingSize: Int? = null,
    val ingredients: List<RecipeIngredientResponse>? = null,
    val instructions: String? = null,
    val createdBy: String? = null,
)

data class RecipeIngredientResponse(
    val foodId: Int? = null,
    val requiredQuantity: String? = null,
    val mandatory: Boolean? = null,
    val possibleSubstitutes: List<Int>? = null,
    val foodName: String? = null,
)
