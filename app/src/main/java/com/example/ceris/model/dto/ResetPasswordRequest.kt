package com.example.ceris.model.dto

data class ResetPasswordRequest(
    val resetId: String,
    val otp: String,
    val newPassword: String
)
