package com.example.ceris.model

data class VerifyRegistrationResponse (
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt : String
)