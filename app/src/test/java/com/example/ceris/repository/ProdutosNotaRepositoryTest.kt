package com.example.ceris.repository

import com.example.ceris.model.dto.OffProduto
import com.google.gson.Gson
import com.example.ceris.model.dto.OffBusca
import org.junit.Assert.*
import org.junit.Test

class ProdutosNotaRepositoryTest {
    private val milk = OffProduto(productName = "Leite integral", brands = "Marca QA", code = "7891000100103",
        productQuantity = 1000.0, productQuantityUnit = "ml", categoriesTags = listOf("en:milks"))

    @Test fun buscaPreservaNomeCodigoEmbalagemECategoria() {
        val result = ProdutosNotaRepository.choose("LEITE INTEGRAL", listOf(milk))
        assertEquals("FOUND", result.status)
        assertEquals("7891000100103", result.product!!.gtin)
        assertEquals("10000025", result.product!!.gpcCode)
        assertEquals(1000.0, result.product!!.packageQuantity!!, 0.0)
    }
    @Test fun naoEscolheEntreGtinDiferentesNemAceitaDadosInsuficientes() {
        assertEquals("AMBIGUOUS", ProdutosNotaRepository.choose("Leite integral",
            listOf(milk, milk.copy(code = "3017620422003"))).status)
        assertEquals("NOT_FOUND", ProdutosNotaRepository.choose("Leite integral",
            listOf(milk.copy(productQuantity = null))).status)
        assertEquals("NOT_FOUND", ProdutosNotaRepository.choose("Arroz branco", listOf(milk)).status)
    }
    @Test fun jsonDaBaseAbertaPreservaCamposParaCadastroEmLote() {
        val response = Gson().fromJson("""{"products":[{"code":"7891000100103","product_name":"Leite integral","product_quantity":1000,"product_quantity_unit":"ml","categories_tags":["en:milks"]}]}""", OffBusca::class.java)
        assertEquals("FOUND", ProdutosNotaRepository.choose("Leite integral", response.products).status)
    }
}
