package com.example.ceris.model.dto

data class GoogleRegistrationRequiredResponse(
    val googleTicket: String,
    val email: String?,
    val name: String?,
    val profileImageUrl: String?,
    val requiredFields: List<String>?
)
