package com.example.ceris.model.dto

// id e o documento do Notify para o DELETE, nao o FID enviado no cadastro.
data class PushInstallationResponse(val id: String, val status: String)
