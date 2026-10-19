package com.example.ceris.local

/** A geracao muda no login/logout, mas permanece durante a renovacao. */
data class SessionSnapshot(
    val generation: String,
    val profileId: Int?,
    val accessToken: String,
    val refreshToken: String,
) {
    override fun toString(): String = "SessionSnapshot(profileId=$profileId)"
}
