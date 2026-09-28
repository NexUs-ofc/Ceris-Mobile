package com.example.ceris.model.dto

data class ShoppingListResponse(
    val id: String,
    val title: String,
    val householdId: Int? = null,
    val eventId: String? = null,
    // Nome feio, mas e o do contrato (ShoppingList.array_list): renomear da null no Gson.
    val arrayList: List<ShoppingListItemResponse>? = null
)
