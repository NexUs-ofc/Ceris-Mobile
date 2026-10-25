package com.example.ceris.model

import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.StoreResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LocalMapperTest {
    private val store = StoreResponse(
        id = 45,
        name = " Mercado ",
        street = " Rua A ",
        number = "10",
        neighborhood = "Centro",
        city = "Sao Paulo",
        state = "SP",
    )

    @Test
    fun preservaIdEFormataEnderecoSemFotoCadastrada() {
        val local = store.copy(distanceKm = 1.25).toLocal()
        assertEquals("45", local.id)
        assertEquals("Mercado", local.nome)
        assertEquals("Rua A, 10 - Centro - Sao Paulo / SP", local.endereco)
        assertEquals("1,2 km", local.distancia)
        assertNull(local.imagemUrl)
    }

    @Test
    fun repassaUrlDaFotoDaCoreParaOTela() {
        val local = store.copy(storeImageUrl = " https://img.example/loja.jpg ").toLocal()
        assertEquals("https://img.example/loja.jpg", local.imagemUrl)
        assertNull(store.copy(storeImageUrl = "  ").toLocal().imagemUrl)
    }

    @Test
    fun preservaDadosNecessariosAoDetalheDepoisDoMerge() {
        val local = store.copy(openingTime = "07:00", closingTime = "22:30", latitude = -23.5, longitude = -46.6).toLocal()
        assertEquals("Rua A, 10 - Centro - Sao Paulo / SP", local.enderecoCompleto)
        assertEquals("7h às 22h30", local.horario)
        assertEquals(-23.5, local.latitude ?: 0.0, 0.0)
        assertEquals(-46.6, local.longitude ?: 0.0, 0.0)
    }

    @Test
    fun enderecoParcialNaoTemNullNemSeparadorSolto() {
        assertEquals("Centro - SP", store.copy(street = null, number = " ", city = null).toLocal().endereco)
        assertEquals("", AddressDto(null, " ", null, null, null, "").formattedAddress())
        assertEquals("10", AddressDto(null, "10", null, null, null, null).formattedAddress())
        assertEquals("Rua A - Cidade", AddressDto("Rua A", null, null, null, "Cidade", null).formattedAddress())
    }

    @Test
    fun distanciaUsaMetrosOuKmPtBRIndependentementeDoAparelho() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            mapOf(0.0 to "0 m", 0.001 to "1 m", 0.45 to "450 m", 0.999 to "999 m", 1.0 to "1 km", 12.56 to "12,6 km")
                .forEach { (value, expected) -> assertEquals(expected, formatDistance(value)) }
        } finally {
            Locale.setDefault(previous)
        }
        listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { assertEquals("", formatDistance(it)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaLojaSemId() {
        store.copy(id = null).toLocal()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejeitaIdInvalido() {
        store.copy(id = 0).toLocal()
    }
}
