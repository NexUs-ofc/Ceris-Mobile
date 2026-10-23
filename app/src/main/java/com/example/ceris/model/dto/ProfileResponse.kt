package com.example.ceris.model.dto

/** GET /api/profile da Core. */
data class ProfileResponse(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val address: AddressDto? = null,
    val dietaryRestrictions: List<String>? = null
)

/** Endereco como a Core devolve e recebe: CEP so com os 8 digitos, UF com 2 letras. */
data class AddressDto(
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val cep: String? = null,
    val city: String? = null,
    val state: String? = null
)

/** PATCH /api/profile. Nulo nao muda o campo. */
data class ProfileUpdateRequest(
    val name: String? = null,
    val address: AddressDto? = null,
    val dietaryRestrictions: List<String>? = null
)
