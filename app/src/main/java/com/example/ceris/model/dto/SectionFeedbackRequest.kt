package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

data class SectionFeedbackRequest(
    @SerializedName("nota") val nota: Int,
    @SerializedName("feedback") val feedback: String?
)
