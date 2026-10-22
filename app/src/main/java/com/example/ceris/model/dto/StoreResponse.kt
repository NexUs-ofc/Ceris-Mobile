package com.example.ceris.model.dto

/** Mercado como a Core devolve. Horarios em "HH:mm"; distancia so no /nearby. */
data class StoreResponse(
    val id: Int,
    val name: String? = null,
    val storeImageUrl: String? = null,
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null,
    val openingTime: String? = null,
    val closingTime: String? = null
)
