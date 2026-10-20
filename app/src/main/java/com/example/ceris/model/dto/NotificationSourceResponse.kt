package com.example.ceris.model.dto

data class StockNotificationSource(
    val id: Int?,
    val foodName: String?,
    val productBrand: String?,
    val unitOfMeasure: String?,
    val quantity: Int?,
    val expiryDate: String?,
)

data class StoreNotificationSource(
    val id: Int?,
    val name: String?,
    val street: String?,
    val number: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val openingTime: String?,
    val closingTime: String?,
)

data class EventNotificationSource(
    val id: String?,
    val title: String?,
    val description: String?,
    val date: String?,
    val duration: Int?,
    val location: String?,
    val peopleCount: Int?,
)
