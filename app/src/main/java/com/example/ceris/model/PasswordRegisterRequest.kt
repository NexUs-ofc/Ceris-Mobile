package com.example.ceris.model

import com.example.ceris.BuildConfig

data class PasswordRegisterRequest (
    val type: String = BuildConfig.ACCOUNT_TYPE,
    val email: String,
    val password: String,
    val name: String,
    val phones: List<String>? = null,
    val address: Address,
    val profileImageURL: String? = null
)

data class Address (
    val neighborhood: String,
    val street: String,
    val number: String,
    val cep: String,
    val city: String,
    val state: String
)
