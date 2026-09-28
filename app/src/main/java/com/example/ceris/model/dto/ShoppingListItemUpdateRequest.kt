package com.example.ceris.model.dto

// Os dois campos sao opcionais: manda-se so o que mudou.
data class ShoppingListItemUpdateRequest(
    val quantity: Double? = null,
    val checked: Boolean? = null
)
