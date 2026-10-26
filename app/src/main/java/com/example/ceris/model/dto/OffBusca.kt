package com.example.ceris.model.dto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** Resposta de GET api/v2/search do Open Food Facts. */
data class OffBusca(
    val products: List<OffProduto> = emptyList()
)

data class OffProduto(
    @SerializedName("product_name")
    val productName: String? = null,
    val brands: String? = null,
    @SerializedName("image_front_url")
    val imageFrontUrl: String? = null,
    /** Valores por 100 g. Chaves como "energy-kcal_100g"; sodio vem em gramas. */
    val nutriments: JsonObject? = null,
    val code: String? = null,
    @SerializedName("product_quantity") val productQuantity: Double? = null,
    @SerializedName("product_quantity_unit") val productQuantityUnit: String? = null,
    @SerializedName("categories_tags") val categoriesTags: List<String>? = null
)
