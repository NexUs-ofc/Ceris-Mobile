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

    @Test fun jsonDaNotaPreservaEanETextoOriginalParaIdentificacao() {
        val item = Gson().fromJson(
            """{"name":"PAP H PERSONAL VIP","raw_text":"PAP H PERSONAL VIP","ean":"7891150039001","ean_candidates":[],"quantity":1,"unit":"un","unit_price":31.9}""",
            com.example.ceris.model.dto.ReceiptItem::class.java,
        )
        assertEquals("7891150039001", item.ean)
        assertEquals("PAP H PERSONAL VIP", item.rawText)
        assertEquals(31.9, item.unitPrice!!, 0.0)
    }

    @Test fun nomeUnicoIgnoraPesoEUnidadeImpressosNaNota() {
        val nesquik = milk.copy(
            productName = "Bebida em pó NESQUIK",
            brands = "Nestlé",
            productQuantity = 400.0,
            productQuantityUnit = "g",
        )
        val resultado = ProdutosNotaRepository.choose("NESQUIK 400 G UN", listOf(nesquik))
        assertEquals("FOUND", resultado.status)
        assertEquals("7891000100103", resultado.product?.gtin)
    }

    @Test fun nomeDeUmTokenPodeEncontrarItemSemEanEContinuaAmbiguoSeDuplicado() {
        val batata = milk.copy(
            productName = "Batata inglesa",
            brands = "",
            categoriesTags = listOf("en:vegetables"),
        )
        assertEquals("FOUND", ProdutosNotaRepository.choose("BATATA", listOf(batata)).status)
        assertEquals("AMBIGUOUS", ProdutosNotaRepository.choose(
            "BATATA", listOf(batata, batata.copy(code = "3017620422003")),
        ).status)
    }

    @Test fun nomeComVariosProdutosDeCodigoDiferenteContinuaAmbiguo() {
        val nesquik = milk.copy(productName = "Bebida em pó NESQUIK", brands = "Nestlé")
        val resultado = ProdutosNotaRepository.choose(
            "NESQUIK 400 G",
            listOf(nesquik, nesquik.copy(code = "3017620422003", productQuantity = 500.0)),
        )
        assertEquals("AMBIGUOUS", resultado.status)
    }
}
