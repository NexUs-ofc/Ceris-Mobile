package com.example.ceris.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O Retrofit recusa baseUrl sem barra no fim, e cada endereco novo colado no
 * local.properties e uma chance de esquecer dela. O erro aparece como o app
 * fechando na abertura da tela, nao como configuracao errada.
 */
class ApiBaseUrlTest {
    @Test
    fun acrescentaABarraQueFalta() {
        assertEquals("https://api.ceris.app/", ApiBaseUrl.normalizar("https://api.ceris.app"))
    }

    @Test
    fun mantemABarraQueJaExiste() {
        assertEquals("https://api.ceris.app/", ApiBaseUrl.normalizar("https://api.ceris.app/"))
    }

    @Test
    fun mantemOCaminhoEAcrescentaABarra() {
        assertEquals("https://api.ceris.app/v1/", ApiBaseUrl.normalizar("https://api.ceris.app/v1"))
    }

    @Test
    fun aceitaHttpSemTrocarPorHttps() {
        assertEquals("http://10.0.2.2:8080/", ApiBaseUrl.normalizar("http://10.0.2.2:8080"))
    }

    @Test
    fun assumeHttpsQuandoSoVeioODominio() {
        assertEquals("https://api.ceris.app/", ApiBaseUrl.normalizar("api.ceris.app"))
    }

    @Test
    fun tiraOEspacoColadoJuntoDoEndereco() {
        assertEquals("https://api.ceris.app/", ApiBaseUrl.normalizar("  https://api.ceris.app  "))
    }

    @Test
    fun enderecoVazioViraAReserva() {
        assertEquals(ApiBaseUrl.RESERVA, ApiBaseUrl.normalizar(""))
        assertEquals(ApiBaseUrl.RESERVA, ApiBaseUrl.normalizar("   "))
    }

    @Test
    fun enderecoAusenteViraAReserva() {
        assertEquals(ApiBaseUrl.RESERVA, ApiBaseUrl.normalizar(null))
    }

    @Test
    fun sentinelaDoBuildGradleViraAReserva() {
        assertEquals(ApiBaseUrl.RESERVA, ApiBaseUrl.normalizar(ApiBaseUrl.NAO_CONFIGURADA))
    }

    @Test
    fun aReservaEValidaParaORetrofit() {
        assertTrue(ApiBaseUrl.RESERVA.startsWith("http"))
        assertTrue(ApiBaseUrl.RESERVA.endsWith("/"))
    }

    @Test
    fun aReservaNaoPassaPorConfigurada() {
        assertFalse(ApiBaseUrl.configurada(ApiBaseUrl.RESERVA))
        assertTrue(ApiBaseUrl.configurada("https://api.ceris.app/"))
    }
}
