package com.example.ceris.repository

import com.example.ceris.model.dto.OffBusca
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Casamento com o Open Food Facts. Os produtos abaixo sao um exemplo no formato
 * da API, nao dado real: o teste cobre a regra, nao o conteudo da base.
 */
class NutricaoRepositoryTest {

    private val resposta = Gson().fromJson(
        """
        {"products":[
          {"product_name":"Leite UHT integral","brands":"Outra Marca",
           "nutriments":{"energy-kcal_100g":60}},
          {"product_name":"Leite Integral 1L","brands":"Piracanjuba",
           "image_front_url":"https://example.org/foto.jpg",
           "nutriments":{"energy-kcal_100g":190,"carbohydrates_100g":0,"proteins_100g":26,
             "fat_100g":9,"saturated-fat_100g":2.5,"fiber_100g":0,"sodium_100g":0.075}},
          {"product_name":"Leite Desnatado","brands":"Piracanjuba",
           "nutriments":{"energy-kcal_100g":35}},
          {"product_name":"Leite Integral 1L","brands":"Piracanjuba"}
        ]}
        """.trimIndent(),
        OffBusca::class.java
    ).products

    @Test
    fun escolheProdutoComMarcaETodasAsPalavrasDoNome() {
        val nutricao = escolher("Leite Integral 1L", "Piracanjuba", resposta)

        assertEquals(190.0, nutricao?.energiaKcal ?: -1.0, 0.0)
        assertEquals(26.0, nutricao?.proteinas ?: -1.0, 0.0)
        assertEquals(0.075, nutricao?.sodioGramas ?: -1.0, 0.0)
        assertEquals("https://example.org/foto.jpg", nutricao?.imagemUrl)
    }

    @Test
    fun semMarcaCorrespondenteNaoMostraNada() {
        assertNull(escolher("Leite Integral 1L", "Vigor", resposta))
    }

    @Test
    fun coberturaAbaixoDoMinimoNaoMostraNada() {
        // 5 palavras no nome; o produto tem so 3 delas (60%), abaixo de 70%.
        assertNull(escolher("Leite Integral Premium Desnatado Piracanjuba", "Piracanjuba", resposta))
    }

    @Test
    fun produtoSemEnergiaNaoEEscolhido() {
        assertNull(escolher("Leite Desnatado", "Piracanjuba", listOf(resposta.last())))
    }

    @Test
    fun semNomeNaoBusca() {
        assertNull(escolher("", "Piracanjuba", resposta))
    }

    @Test
    fun semMarcaCasaSoPeloNome() {
        val nutricao = escolher("Leite Desnatado", null, resposta)

        assertEquals(35.0, nutricao?.energiaKcal ?: -1.0, 0.0)
    }

    @Test
    fun semMarcaUmaPalavraSoNaoBasta() {
        // "Leite" casaria com qualquer leite da base.
        assertNull(escolher("Leite", "", resposta))
    }
}
