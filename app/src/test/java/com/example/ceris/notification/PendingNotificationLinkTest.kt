package com.example.ceris.notification

import com.example.ceris.support.MemoryPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingNotificationLinkTest {
    private val preferences = MemoryPreferences()
    private var now = 1_000_000L
    private val pending = PendingNotificationLink(preferences) { now }
    private val link = requireNotNull(NotificationLink.parse("ceris://notification/12/notice?type=PRODUCT_EXPIRING&sourceId=45"))

    @Test
    fun sobreviveAoProcessoMasConsomeApenasUmaVez() {
        pending.save(link)
        val recreated = PendingNotificationLink(preferences) { now }
        assertEquals(link, recreated.take())
        assertNull(pending.take())
    }

    @Test
    fun naoReabreDestinoAntigoOuComRelogioRecuado() {
        pending.save(link)
        now += PendingNotificationLink.MAX_AGE_MS + 1
        assertNull(pending.take())
        pending.save(link)
        now -= 100
        assertNull(pending.take())
    }

    @Test
    fun novaNotificacaoSubstituiAnteriorELogoutLimpaDestino() {
        pending.save(link)
        val other = requireNotNull(NotificationLink.parse("ceris://notification/12/other?type=NEARBY_STORE&sourceId=8"))
        pending.save(other)
        assertEquals(other, pending.take())
        pending.save(link)
        pending.clear()
        assertNull(pending.take())
    }

    @Test
    fun explicacaoAntigaNaoImpedeDialogoDoAndroid() {
        preferences.edit().putBoolean("permission_requested", true).apply()
        val state = PushState(preferences)
        assertFalse(state.permissionRequested)
        state.requestPermission()
        assertTrue(PushState(preferences).permissionRequested)
    }
}
