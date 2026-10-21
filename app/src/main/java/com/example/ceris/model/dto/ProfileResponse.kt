package com.example.ceris.model.dto

/** Campos consumidos por Locais de GET /api/profile. A Core nao expoe coordenadas aqui. */
data class ProfileResponse(
    val id: Int?,
    val address: ProfileAddressResponse?,
)

data class ProfileAddressResponse(
    val street: String?,
    val number: String?,
    val neighborhood: String?,
    val cep: String?,
    val city: String?,
    val state: String?,
)
