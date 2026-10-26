package com.example.ceris.model

import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.StoreResponse
import java.text.NumberFormat
import java.util.Locale

private fun String?.addressPart(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun address(
    street: String?,
    number: String?,
    neighborhood: String?,
    city: String?,
    state: String?,
): String =
    listOfNotNull(
        listOfNotNull(street.addressPart(), number.addressPart()).joinToString(", ").ifEmpty { null },
        neighborhood.addressPart(),
        listOfNotNull(city.addressPart(), state.addressPart()).joinToString(" / ").ifEmpty { null },
    ).joinToString(" - ")

fun AddressDto.formattedAddress(): String = address(street, number, neighborhood, city, state)

/** Texto vazio significa dado ausente; a tela decide o placeholder ou oculta o campo. */
fun StoreResponse.toLocal(): Local {
    val realId = requireNotNull(id) { "Loja sem ID" }
    require(realId > 0) { "ID de loja invalido" }
    return Local(
        id = realId.toString(),
        nome = name?.trim().orEmpty(),
        endereco = address(street, number, neighborhood, city, state),
        distancia = formatDistance(distanceKm),
        imagemUrl = storeImageUrl?.trim()?.takeIf { it.isNotEmpty() },
        enderecoCompleto = address(street, number, neighborhood, city, state),
        horario = formatHours(openingTime, closingTime),
        latitude = latitude,
        longitude = longitude,
    )
}

private fun formatHours(openingTime: String?, closingTime: String?): String? {
    fun hour(value: String?): String? {
        val parts = value?.split(':') ?: return null
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return if (minutes == 0) "${hours}h" else "${hours}h${minutes.toString().padStart(2, '0')}"
    }
    val opening = hour(openingTime) ?: return null
    val closing = hour(closingTime) ?: return null
    return "$opening às $closing"
}

fun formatDistance(distanceKm: Double?): String {
    if (distanceKm == null || !distanceKm.isFinite() || distanceKm < 0) return ""
    val meters = distanceKm < 1
    val formatter =
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            maximumFractionDigits = if (meters) 0 else 1
        }
    return "${formatter.format(if (meters) distanceKm * 1000 else distanceKm)} ${if (meters) "m" else "km"}"
}
