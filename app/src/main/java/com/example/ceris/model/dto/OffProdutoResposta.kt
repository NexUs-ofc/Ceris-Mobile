package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/** GET api/v2/product/{gtin} do Open Food Facts. status 1 = achou. */
data class OffProdutoResposta(
    val status: Int = 0,
    val product: OffProdutoDetalhe? = null
)

data class OffProdutoDetalhe(
    @SerializedName("product_name")
    val productName: String? = null,
    val brands: String? = null,
    @SerializedName("product_quantity")
    val productQuantity: Double? = null,
    @SerializedName("product_quantity_unit")
    val productQuantityUnit: String? = null,
    @SerializedName("image_front_url")
    val imageFrontUrl: String? = null
)
