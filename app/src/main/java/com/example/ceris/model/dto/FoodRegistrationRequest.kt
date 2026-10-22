package com.example.ceris.model.dto

/**
 * Cadastro por codigo de barras na Core (POST /api/foods).
 *
 * Se o GTIN ja existe no catalogo, a Core reaproveita o alimento e so cria o
 * item no estoque; gpcCode so e exigido quando o produto e novo.
 */
data class FoodRegistrationRequest(
    val gtin: String,
    val name: String,
    val productBrand: String?,
    /** Foto da base aberta. A Core so grava quando o alimento ainda nao tem. */
    val foodImageUrl: String? = null,
    val packageQuantity: Double,
    /** Enum da Core: g, kg, ml, l ou unit. */
    val unitOfMeasure: String,
    val gpcCode: String,
    val quantity: Int,
    /** yyyy-MM-dd, ou nulo para produto sem validade. */
    val expiryDate: String?
)
