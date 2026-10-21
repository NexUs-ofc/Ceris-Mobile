package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Andamento de uma leitura por pedido no ms-ia (POST /receipt/scan/async e
 * GET /receipt/scan/{pedido}). status: "processando", "pronto" ou "erro".
 */
data class ReceiptScanStatus(
    @SerializedName("pedido_id")
    val pedidoId: String,
    val status: String,
    val resultado: ReceiptScanResponse? = null,
    val codigo: Int? = null,
    val detalhe: String? = null
)
