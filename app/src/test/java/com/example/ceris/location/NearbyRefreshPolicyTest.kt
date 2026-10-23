package com.example.ceris.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyRefreshPolicyTest {
    private val point = DeviceCoordinates(-23.5, -46.6)

    @Test
    fun ignoraOscilacaoMasRenovaParadoAposUmMinuto() {
        val policy = NearbyRefreshPolicy()
        assertTrue(policy.shouldQuery(point, 0))
        assertFalse(policy.shouldQuery(point.copy(latitude = -23.50001), 20_000))
        assertFalse(policy.shouldQuery(point, 59_999))
        assertTrue(policy.shouldQuery(point, 60_000))
    }

    @Test
    fun movimentoSignificativoRespeitaIntervaloMinimoEResetAceitaNovaOrigem() {
        val policy = NearbyRefreshPolicy()
        val moved = point.copy(latitude = -23.502)
        assertTrue(policy.shouldQuery(point, 0))
        assertFalse(policy.shouldQuery(moved, 14_999))
        assertTrue(policy.shouldQuery(moved, 15_000))
        policy.reset()
        assertTrue(policy.shouldQuery(point, 15_001))
    }
}
