package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/** Contrato compartilhado por /stores/nearby e /stores/{id}. Horarios em HH:mm. */
data class StoreResponse(
    val id: Int?,
    val name: String?,
    val street: String?,
    val number: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val openingTime: String?,
    val closingTime: String?,
    val cnpj: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null,
    @SerializedName(value = "storeImageUrl", alternate = ["profileImageUrl"])
    val storeImageUrl: String? = null,
)
