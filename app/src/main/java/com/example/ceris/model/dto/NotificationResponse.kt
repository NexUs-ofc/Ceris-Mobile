package com.example.ceris.model.dto

data class NotificationResponse(
    val id: String,
    // String preserva tipos futuros que o app ainda nao conhece.
    val type: String,
    val sourceId: String,
    val title: String,
    val message: String,
    val createdAt: String,
    val readAt: String?,
    val read: Boolean,
)
