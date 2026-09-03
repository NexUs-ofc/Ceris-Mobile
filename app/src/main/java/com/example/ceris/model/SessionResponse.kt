package com.example.ceris.model

abstract class SessionResponse {
    abstract var accessToken: String
    abstract var accessTokenExpiresAt: String
    abstract var refreshToken: String
    abstract var refreshTokenExpiresAt : String
}