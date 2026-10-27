package com.example.ceris.api

import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.FoodBatchRegistrationRequest
import com.example.ceris.model.dto.FoodRegistrationRequest
import com.google.gson.JsonParser
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockBatchAPITest {
    @Test
    fun confirmacaoEnviaProdutosEmLoteComSessaoCapturada() {
        val api = Retrofit.Builder().baseUrl("https://core.example/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(StockAPI::class.java)
        val session = SessionSnapshot("generation", 12, "token", "refresh")
        val product = FoodRegistrationRequest("7891000100103", "Arroz", null,
            packageQuantity = 1.0, unitOfMeasure = "kg", gpcCode = "10000225",
            quantity = 2, expiryDate = "2027-01-01")
        val request = api.registrarLote("Bearer token", FoodBatchRegistrationRequest(listOf(product)), session).request()
        assertEquals("POST", request.method())
        assertEquals("/api/foods/batch", request.url().encodedPath())
        assertEquals(session, request.tag(SessionSnapshot::class.java))
        assertEquals("Bearer token", request.header("Authorization"))
        val buffer = Buffer()
        request.body()!!.writeTo(buffer)
        val products = JsonParser().parse(buffer.readUtf8()).asJsonObject.getAsJsonArray("products")
        assertEquals(1, products.size())
        assertEquals(2, products[0].asJsonObject.get("quantity").asInt)
        assertEquals("2027-01-01", products[0].asJsonObject.get("expiryDate").asString)
    }
}
