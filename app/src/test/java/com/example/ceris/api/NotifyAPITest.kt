package com.example.ceris.api

import com.example.ceris.model.dto.RegisterPushInstallationRequest
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotifyAPITest {
    private val api =
        Retrofit.Builder().baseUrl("https://notify.example/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(NotifyAPI::class.java)

    private fun assertBearer(request: Request) {
        assertEquals("Bearer token", request.header("Authorization"))
        assertNull(request.header("X-API-Key"))
    }

    @Test
    fun cadastroEnviaFIDSemPerfilOuChaveDeAPI() {
        val request = api.registrarInstalacao("Bearer token", RegisterPushInstallationRequest("fid-confirmado")).request()
        assertEquals("PUT", request.method())
        assertEquals("/api/push/installations", request.url().encodedPath())
        assertBearer(request)
        val buffer = Buffer()
        request.body()!!.writeTo(buffer)
        assertEquals("{\"installationId\":\"fid-confirmado\"}", buffer.readUtf8())
    }

    @Test
    fun historicoUsaPaginacaoDoNotify() {
        val request = api.listar("Bearer token", 2, 20).request()
        assertEquals("GET", request.method())
        assertEquals("/api/notifications", request.url().encodedPath())
        assertEquals("2", request.url().queryParameter("page"))
        assertEquals("20", request.url().queryParameter("size"))
        assertBearer(request)
    }

    @Test
    fun desvinculacaoELeituraUsamIdsDistintos() {
        val revoke = api.desvincularInstalacao("Bearer token", "cadastro").request()
        val read = api.marcarComoLida("Bearer token", "aviso").request()
        assertEquals("DELETE", revoke.method())
        assertEquals("/api/push/installations/cadastro", revoke.url().encodedPath())
        assertEquals("PATCH", read.method())
        assertEquals("/api/notifications/aviso/read", read.url().encodedPath())
        assertBearer(revoke)
        assertBearer(read)
    }
}
