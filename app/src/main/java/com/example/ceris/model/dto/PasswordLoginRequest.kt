package com.example.ceris.model.dto

import com.example.ceris.model.Channel

data class PasswordLoginRequest(
    val email: String,
    val password: String,
    val channel: Channel,
)