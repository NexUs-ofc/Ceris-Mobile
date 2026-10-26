package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeAPITest {
    @Test
    fun receitasSalvasUsamCoreBearerESessaoCapturada() {
        val api =
            Retrofit.Builder().baseUrl("https://core.example/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(RecipeAPI::class.java)
        val session = SessionSnapshot("generation", 12, "token", "refresh")
        listOf(
            api.listar("Bearer token", session).request() to "/api/recipes",
            api.detalhe("Bearer token", "507f1f77bcf86cd799439011", session).request() to "/api/recipes/507f1f77bcf86cd799439011",
        ).forEach { (request, path) ->
            assertEquals("GET", request.method())
            assertEquals(path, request.url().encodedPath())
            assertEquals("Bearer token", request.header("Authorization"))
            assertEquals(session, request.tag(SessionSnapshot::class.java))
            assertNull(request.header("X-API-Key"))
            assertNull(request.url().query())
        }
    }
}
