package com.example.ceris.model.dto

// Espelha o RefreshRequest do NexUs-Auth: um unico campo, refreshToken.
data class RefreshRequest(
    val refreshToken: String
)
