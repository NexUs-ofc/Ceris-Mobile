package com.example.ceris.model.dto

import com.example.ceris.BuildConfig
import com.example.ceris.model.Address

data class GoogleRegistrationRequest(
    val googleTicket: String,
    val type: String = BuildConfig.ACCOUNT_TYPE,
    val phones: List<String>? = null,
    val address: Address,
    val cnpj: String? = null,
    val planId: Long? = null
)
