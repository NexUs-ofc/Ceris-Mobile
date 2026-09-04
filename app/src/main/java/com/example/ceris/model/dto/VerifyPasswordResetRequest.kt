package com.example.ceris.model.dto

data class VerifyPasswordResetRequest(
    val resetId: String,
    val otp: String
)
