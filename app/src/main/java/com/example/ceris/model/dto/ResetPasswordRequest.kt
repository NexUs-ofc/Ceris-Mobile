package com.example.ceris.model.dto

data class ResetPasswordRequest(
    val resetTicket: String,
    val newPassword: String
)
