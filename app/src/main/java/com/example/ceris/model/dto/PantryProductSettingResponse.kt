package com.example.ceris.model.dto

data class PantryProductSettingResponse(
    val foodId: Int,
    val minimumQuantity: Int,
    val foodName: String? = null,
    val foodImageUrl: String? = null,
)
