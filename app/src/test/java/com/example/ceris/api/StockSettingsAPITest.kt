package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.MinimumQuantityRequest
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockSettingsAPITest {
    private val session = SessionSnapshot("generation", 12, "token", "refresh")

    private fun api(body: String): StockAPI {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.get("application/json"), body))
                .build()
        }.build()
        return Retrofit.Builder()
            .baseUrl("https://core.example/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(StockAPI::class.java)
    }

    @Test
    fun consultaConfiguracoesAutenticadasPeloContratoDoCore() {
        val call = api("""[{"foodId":42,"foodName":"Leite","minimumQuantity":0}]""")
            .configuracoes("Bearer token", session)
        val request = call.request()

        assertEquals("GET", request.method())
        assertEquals("/api/stock/settings", request.url().encodedPath())
        assertEquals("Bearer token", request.header("Authorization"))
        assertEquals(session, request.tag(SessionSnapshot::class.java))
        val setting = call.execute().body()!!.single()
        assertEquals(42, setting.foodId)
        assertEquals(0, setting.minimumQuantity)
    }

    @Test
    fun estoqueSemConfiguracaoRetornaListaVazia() {
        assertTrue(api("[]").configuracoes("Bearer token", session).execute().body()!!.isEmpty())
    }

    @Test
    fun configuraOuRemoveMinimoDoAlimentoComPutInclusiveZero() {
        for (minimum in listOf(1, 8, 0)) {
            val call = api("""{"foodId":42,"minimumQuantity":$minimum}""")
                .configurarMinimo("Bearer token", 42, MinimumQuantityRequest(minimum), session)
            val request = call.request()

            assertEquals("PUT", request.method())
            assertEquals("/api/stock/settings/42", request.url().encodedPath())
            assertEquals("Bearer token", request.header("Authorization"))
            assertEquals(session, request.tag(SessionSnapshot::class.java))
            val buffer = Buffer()
            request.body()!!.writeTo(buffer)
            val json = JsonParser().parse(buffer.readUtf8()).asJsonObject
            assertEquals(setOf("minimumQuantity"), json.keySet())
            assertEquals(minimum, json.get("minimumQuantity").asInt)
            assertEquals(minimum, call.execute().body()!!.minimumQuantity)
        }
    }
}
