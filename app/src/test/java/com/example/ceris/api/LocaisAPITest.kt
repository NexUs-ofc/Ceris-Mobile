package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.StoreResponse
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocaisAPITest {
    private val retrofit = Retrofit.Builder().baseUrl("https://core.example/").addConverterFactory(GsonConverterFactory.create()).build()
    private val api = retrofit.create(LocaisAPI::class.java)
    private val session = SessionSnapshot("generation", 12, "token", "refresh")

    @Test
    fun usaCoreBearerESessaoSemCoordenadasOuRaioInventados() {
        listOf(
            api.profile("Bearer token", session).request() to "/api/profile",
            api.nearby("Bearer token", session).request() to "/api/stores/nearby",
        ).forEach { (request, path) ->
            assertEquals("GET", request.method())
            assertEquals("core.example", request.url().host())
            assertEquals(path, request.url().encodedPath())
            assertNull(request.url().query())
            assertEquals("Bearer token", request.header("Authorization"))
            assertEquals(session, request.tag(SessionSnapshot::class.java))
            assertNull(request.header("X-API-Key"))
        }
    }

    @Test
    fun serializaParametrosOpcionaisComNomesDaCore() {
        val url = api.nearby("Bearer token", session, -23.5, -46.6, 10.0).request().url()
        assertEquals("-23.5", url.queryParameter("latitude"))
        assertEquals("-46.6", url.queryParameter("longitude"))
        assertEquals("10.0", url.queryParameter("radiusKm"))
        val byProfile = api.nearby("Bearer token", session, radiusKm = 5.0).request().url()
        assertEquals(setOf("radiusKm"), byProfile.queryParameterNames())
    }

    @Test
    fun desserializaContratoRealDeLojaEPerfilComCamposAusentes() {
        val store =
            decode<StoreResponse>(
                """{"id":45,"name":"Mercado","cnpj":"123","street":"Rua A","number":"10","neighborhood":"Centro",
                "city":"Cidade","state":"SP","latitude":-23.5,"longitude":-46.6,"distanceKm":0.45,
                "openingTime":"08:00","closingTime":"18:00"}""",
            )
        assertEquals(45, store.id)
        assertEquals("123", store.cnpj)
        assertEquals(-23.5, store.latitude)
        assertEquals(-46.6, store.longitude)
        assertEquals(0.45, store.distanceKm)
        assertEquals("08:00", store.openingTime)
        assertEquals("18:00", store.closingTime)
        assertNull(decode<StoreResponse>("""{"id":45,"distanceKm":null}""").openingTime)
        assertNull(decode<StoreResponse>("""{"id":45}""").distanceKm)
        val profile =
            decode<ProfileResponse>(
                """{"id":12,"name":"Pessoa","address":{"street":"Rua A","number":"10","neighborhood":"Centro",
                "cep":"01001000","city":"Cidade","state":"SP"}}""",
            )
        assertEquals("01001000", profile.address?.cep)
        assertEquals(12, profile.id)
        assertNull(decode<ProfileResponse>("""{"id":12,"address":null}""").address)
    }

    private inline fun <reified T> decode(json: String): T =
        requireNotNull(retrofit.responseBodyConverter<T>(T::class.java, emptyArray()).convert(ResponseBody.create(null, json)))
}
