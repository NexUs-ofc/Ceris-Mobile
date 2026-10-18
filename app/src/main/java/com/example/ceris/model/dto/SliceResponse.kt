package com.example.ceris.model.dto

data class SliceResponse<T>(val content: List<T>, val hasNext: Boolean)
