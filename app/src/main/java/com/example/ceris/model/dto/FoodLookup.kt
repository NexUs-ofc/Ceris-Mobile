package com.example.ceris.model.dto

import com.example.ceris.model.CadastroPorCodigo

data class FoodLookupRequest(val names: List<String>)
data class FoodLookupResponse(val items: List<FoodLookupMatch> = emptyList())
data class FoodLookupMatch(val index: Int, val status: String?, val product: FoodLookupProduct? = null)

/** Dados da OFF; só são enviados para cadastro após confirmar a revisão. */
data class FoodLookupProduct(
    val gtin: String?,
    val name: String?,
    val productBrand: String?,
    val foodImageUrl: String?,
    val packageQuantity: Double?,
    val unitOfMeasure: String?,
    val gpcCode: String?,
) {
    fun valido(): Boolean = gtin?.let(CadastroPorCodigo::gtinValido) == true &&
        !name.isNullOrBlank() && packageQuantity != null && packageQuantity.isFinite() && packageQuantity > 0 &&
        unitOfMeasure in setOf("g", "kg", "ml", "l", "unit") && gpcCode?.matches(Regex("\\d{8}")) == true

    fun cadastro(quantity: Int, expiryDate: String?) = FoodRegistrationRequest(
        gtin = requireNotNull(gtin), name = requireNotNull(name), productBrand = productBrand,
        foodImageUrl = foodImageUrl, packageQuantity = requireNotNull(packageQuantity),
        unitOfMeasure = requireNotNull(unitOfMeasure), gpcCode = requireNotNull(gpcCode),
        quantity = quantity, expiryDate = expiryDate,
    )
}
