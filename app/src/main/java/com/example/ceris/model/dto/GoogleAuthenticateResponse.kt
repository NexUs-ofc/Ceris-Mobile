package com.example.ceris.model.dto

data class GoogleAuthenticateResponse(
    val registrationRequired: Boolean,
    val session: GoogleSessionResponse?,
    val registration: GoogleRegistrationRequiredResponse?
)
