package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotificationSourceAPITest {
    @Test
    fun destinosUsamCoreBearerESessaoCapturada() {
        val api =
            Retrofit.Builder().baseUrl("https://core.example/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(NotificationSourceAPI::class.java)
        val session = SessionSnapshot("generation", 12, "token", "refresh")
        listOf(
            api.stock("Bearer token", session).request() to "/api/stock",
            api.store("Bearer token", "45", session).request() to "/api/stores/45",
            api.event("Bearer token", "event-45", session).request() to "/api/events/event-45",
        ).forEach { (request, path) ->
            assertEquals("GET", request.method())
            assertEquals(path, request.url().encodedPath())
            assertEquals("Bearer token", request.header("Authorization"))
            assertEquals(session, request.tag(SessionSnapshot::class.java))
            assertNull(request.header("X-API-Key"))
        }
    }
}
