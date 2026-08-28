package com.example.ceris.model

data class VerifyRegistrationRequest (
    val registrationId: String,
    val otp: String
)