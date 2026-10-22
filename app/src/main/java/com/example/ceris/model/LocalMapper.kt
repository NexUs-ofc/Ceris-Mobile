package com.example.ceris.model

import com.example.ceris.model.dto.ProfileAddressResponse
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

fun ProfileAddressResponse.formattedAddress(): String = address(street, number, neighborhood, city, state)

/** Texto vazio significa dado ausente; a tela decide o placeholder ou oculta o campo. */
fun StoreResponse.toLocal(): Local {
    val realId = requireNotNull(id) { "Loja sem ID" }
    require(realId > 0) { "ID de loja invalido" }
    return Local(
        id = realId.toString(),
        nome = name?.trim().orEmpty(),
        endereco = address(street, number, neighborhood, city, state),
        distancia = formatDistance(distanceKm),
        imagemUrl = profileImageUrl?.trim()?.takeIf { it.isNotEmpty() },
    )
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
