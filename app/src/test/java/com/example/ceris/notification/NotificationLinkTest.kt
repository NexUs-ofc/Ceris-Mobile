package com.example.ceris.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationLinkTest {
    private fun uri(
        type: String = "PRODUCT_EXPIRING",
        source: String = "45",
    ) = "ceris://notification/12/notice-1?type=$type&sourceId=$source"

    @Test
    fun quatroTiposAbremOsDestinosCorretos() {
        mapOf(
            "PRODUCT_EXPIRING" to NotificationLink.Destination.PRODUCT,
            "UPCOMING_EVENT" to NotificationLink.Destination.EVENT,
            "NEARBY_STORE" to NotificationLink.Destination.STORE,
            "NEW_NEARBY_STORE" to NotificationLink.Destination.STORE,
        ).forEach { (type, destination) ->
            val link = requireNotNull(NotificationLink.parse(uri(type)))
            assertEquals(destination, link.destination)
            assertEquals(link, NotificationLink.parse(link.toUri()))
            assertEquals("45", link.sourceId)
            assertEquals("notice-1", link.notificationId)
        }
        assertEquals("event_abc-123", NotificationLink.parse(uri("UPCOMING_EVENT", "event_abc-123"))?.sourceId)
    }

    @Test
    fun aceitaSomentePerfilDestinatario() {
        val link = requireNotNull(NotificationLink.parse(uri()))
        assertTrue(link.belongsTo(12))
        assertFalse(link.belongsTo(13))
        assertFalse(link.belongsTo(null))
    }

    @Test
    fun rejeitaLinksMalformadosExternosEDestinosInjetados() {
        listOf(
            null, "", "https://notification/12/id?type=PRODUCT_EXPIRING&sourceId=45",
            uri().replace("notification/", "user@notification/"),
            uri().replace("notification/", "notification:123/"),
            uri().replace("/12/", "/0/"), uri().replace("/12/", "/-1/"),
            uri().replace("/12/", "/2147483648/"), uri().replace("notice-1", ".."),
            uri().replace("notice-1", "%2e%2e"), uri().replace("notice-1", "id/extra"),
            uri() + "#fragment", uri() + "&type=NEARBY_STORE", uri() + "&url=https://example.com",
            uri("UNKNOWN"), uri(source = "-1"), uri(source = "0"), uri(source = "2147483648"),
            uri(source = ".."), uri(source = "%2Fapi"), uri().replace("sourceId=45", "sourceId"),
            "ceris://notification/12/notice-1", uri().replace("notice-1", "a".repeat(257)),
        ).forEach { assertNull("Deveria rejeitar: $it", NotificationLink.parse(it)) }
    }

    @Test
    fun pushGeraLinkSemTituloOuCredenciais() {
        val message = PushMessage("notice-1", 12, "PRODUCT_EXPIRING", "45", "Privado", "Mensagem privada")
        val link = requireNotNull(NotificationLink.from(message))
        assertEquals(uri(), link.toUri())
        assertFalse(link.toUri().contains("Privado"))
        assertNull(NotificationLink.from(message.copy(sourceId = "../../other")))
    }
}
