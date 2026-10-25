package com.example.ceris.model.dto

import com.example.ceris.model.RecipeDifficulty

data class RecipeResponse(
    val id: String,
    val title: String? = null,
    val servingSize: Int? = null,
    val estimatedTime: String? = null,
    val difficulty: RecipeDifficulty? = null,
    val ingredients: List<RecipeIngredientResponse>? = null,
    val steps: List<RecipeStepResponse?>? = null,
    val createdBy: String? = null,
)

data class RecipeStepResponse(
    val title: String? = null,
    val description: String? = null,
)

data class RecipeIngredientResponse(
    val foodId: Int? = null,
    val requiredQuantity: String? = null,
    val mandatory: Boolean? = null,
    val possibleSubstitutes: List<Int>? = null,
    val foodName: String? = null,
)
