package com.example.ceris.model.dto

/** Receita salva como a Core devolve em GET /api/recipes. */
data class RecipeResponse(
    val id: String,
    val title: String? = null,
    val servingSize: Int? = null,
    val ingredients: List<RecipeIngredientResponse>? = null,
    val instructions: String? = null
)

data class RecipeIngredientResponse(
    val foodId: Int? = null,
    /** Texto livre: "500g", "2 dentes". */
    val requiredQuantity: String? = null
)
