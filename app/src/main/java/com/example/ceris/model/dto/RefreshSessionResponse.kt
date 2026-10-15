package com.example.ceris.model.dto

import com.example.ceris.model.SessionResponse

// O /token/refresh devolve uma sessao inteira, igual ao login: o refresh token
// tambem e trocado, entao guardar so o access deixaria a renovacao seguinte
// com um token ja gasto.
data class RefreshSessionResponse(
    override var accessToken: String,
    override var accessTokenExpiresAt: String,
    override var refreshToken: String,
    override var refreshTokenExpiresAt: String
) : SessionResponse()
