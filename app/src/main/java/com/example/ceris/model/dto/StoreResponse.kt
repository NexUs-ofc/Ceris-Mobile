package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/** Contrato compartilhado por /stores/nearby e /stores/{id}. Horarios em HH:mm. */
data class StoreResponse(
    val id: Int?,
    val name: String? = null,
    val cnpj: String? = null,
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null,
    val openingTime: String? = null,
    val closingTime: String? = null,
    @SerializedName(value = "storeImageUrl", alternate = ["profileImageUrl"])
    val storeImageUrl: String? = null
)
