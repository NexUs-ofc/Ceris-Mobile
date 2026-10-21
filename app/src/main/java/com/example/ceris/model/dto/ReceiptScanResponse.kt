package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Leitura de nota fiscal do ms-ia (POST /receipt/scan).
 *
 * snake_case como o resto do ms-ia, por isso o SerializedName.
 */
data class ReceiptScanResponse(
    val store: String? = null,
    @SerializedName("purchase_date")
    val purchaseDate: String? = null,
    val total: Double? = null,
    val items: List<ReceiptItem> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ReceiptItem(
    val name: String,
    /** Hoje vem sempre nulo: o ms-ia ainda nao consulta o catalogo da Core. */
    @SerializedName("food_id")
    val foodId: Int? = null,
    /** Pode ser fracionada (0,5 kg); a Core so aceita inteiro. */
    val quantity: Double = 1.0,
    val unit: String = "un",
    val category: String? = null,
    /** yyyy-MM-dd. Quase sempre estimada pelo tipo do produto. */
    @SerializedName("expiration_date")
    val expirationDate: String? = null,
    @SerializedName("expiration_estimated")
    val expirationEstimated: Boolean = true,
    @SerializedName("needs_review")
    val needsReview: Boolean = false
)
