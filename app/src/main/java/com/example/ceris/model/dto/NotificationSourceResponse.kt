package com.example.ceris.model.dto

data class StockNotificationSource(
    val id: Int?,
    val foodName: String?,
    val productBrand: String?,
    val unitOfMeasure: String?,
    val quantity: Int?,
    val expiryDate: String?,
)

// Mantem o nome usado pelas notificacoes com o mesmo contrato de lojas.
typealias StoreNotificationSource = StoreResponse

data class EventNotificationSource(
    val id: String?,
    val title: String?,
    val description: String?,
    val date: String?,
    val duration: Int?,
    val location: String?,
    val peopleCount: Int?,
)
