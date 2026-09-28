package com.example.ceris.model.dto

data class ShoppingListRequest(
    val title: String,
    val eventId: String? = null
)
