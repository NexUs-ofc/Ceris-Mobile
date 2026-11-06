package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Leitura de nota fiscal do ms-ia (POST /receipt/scan).
 *
 * snake_case como o resto do ms-ia, por isso o SerializedName.
 */
data class ReceiptScanResponse(
    val store: String? = null,
    val cnpj: String? = null,
    @SerializedName("purchase_date")
    val purchaseDate: String? = null,
    val total: Double? = null,
    @SerializedName("items_total")
    val itemsTotal: Double = 0.0,
    val items: List<ReceiptItem> = emptyList(),
    val source: String = "ocr",
    @SerializedName("ocr_confidence")
    val ocrConfidence: Double = 0.0,
    val discarded: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class ReceiptItem(
    val name: String,
    /** Descricao preservada antes da normalizacao por IA. */
    @SerializedName("raw_text")
    val rawText: String? = null,
    /** GTIN lido da propria nota; evita busca textual sujeita a abreviacoes do cupom. */
    val ean: String? = null,
    @SerializedName("ean_candidates")
    val eanCandidates: List<String> = emptyList(),
    /** Hoje pode vir nulo: o ms-ia ainda nao consulta o catalogo da Core. */
    @SerializedName("food_id")
    val foodId: Int? = null,
    /** Pode ser fracionada (0,5 kg); a Core so aceita inteiro. */
    val quantity: Double = 1.0,
    val unit: String = "un",
    @SerializedName("unit_price")
    val unitPrice: Double? = null,
    val category: String? = null,
    /** yyyy-MM-dd. Quase sempre estimada pelo tipo do produto. */
    @SerializedName("expiration_date")
    val expirationDate: String? = null,
    @SerializedName("expiration_estimated")
    val expirationEstimated: Boolean = true,
    @SerializedName("needs_review")
    val needsReview: Boolean = false
)
