package com.example.ceris.model.dto

import com.example.ceris.BuildConfig
import com.example.ceris.model.Address

data class PasswordRegisterRequest (
    val type: String = BuildConfig.ACCOUNT_TYPE,
    val email: String,
    val password: String,
    val name: String,
    val phones: List<String>? = null,
    val address: Address,
    val profileImageURL: String? = null
)