package com.example.ceris.model.dto

import com.example.ceris.model.SessionResponse

data class PasswordLoginResponse(
    override var accessToken: String,
    override var accessTokenExpiresAt: String,
    override var refreshToken: String,
    override var refreshTokenExpiresAt: String
) : SessionResponse()