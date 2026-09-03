package com.example.ceris.model.dto

data class ViaCEPResponse(
    val cep: String? = null,
    val logradouro: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val erro: String? = null
) {
    fun isValid(): Boolean = erro == null && !cep.isNullOrBlank()
}
