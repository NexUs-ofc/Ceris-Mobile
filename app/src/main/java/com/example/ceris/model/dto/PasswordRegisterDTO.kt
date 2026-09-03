package com.example.ceris.model.dto

import com.example.ceris.model.Address
import com.example.ceris.model.dto.PasswordRegisterRequest

data class PasswordRegisterDTO(

    var email: String? = null,
    var password: String? = null,
    var familyName: String? = null,

    var neighborhood: String? = null,
    var street: String? = null,
    var number: String? = null,
    var cep: String? = null,
    var city: String? = null,
    var state: String? = null

) {
    fun isAllNotNull(): Boolean {
        return email != null &&
                password != null &&
                familyName != null &&
                neighborhood != null &&
                street != null &&
                number != null &&
                cep != null &&
                city != null &&
                state != null
    }

    fun toRequest(): PasswordRegisterRequest {
        return PasswordRegisterRequest(
            name = familyName!!,
            email = email!!,
            password = password!!,
            address = Address(
                neighborhood = neighborhood!!,
                street = street!!,
                number = number!!,
                cep = cep!!,
                city = city!!,
                state = state!!
            )
        )
    }
}