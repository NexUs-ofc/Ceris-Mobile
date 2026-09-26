package com.example.ceris.model.dto

import com.example.ceris.model.Channel

data class GoogleAuthenticateRequest(
    val idToken: String,
    val channel: Channel = Channel.MOBILE
)
