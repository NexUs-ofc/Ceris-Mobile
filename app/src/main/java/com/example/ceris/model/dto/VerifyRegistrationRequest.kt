package com.example.ceris.model.dto

data class VerifyRegistrationRequest (
    val registrationId: String,
    val otp: String
)