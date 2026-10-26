package com.example.ceris.model

import com.example.ceris.model.dto.OffProdutoDetalhe
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadastroPorCodigoTest {

    @Test
    fun gtins_reais_validos() {
        // Leite Moca (achado no Open Food Facts), um EAN-8 e um UPC-A conhecidos.
        listOf("7891000100103", "96385074", "036000291452").forEach {
            assertTrue(it, CadastroPorCodigo.gtinValido(it))
        }
    }

    @Test
    fun digito_errado_leitura_pela_metade_e_lixo_sao_recusados() {
        listOf("7891000100104", "789100010010", "78910001", "abc1000100103", "", "1234567").forEach {
            assertFalse(it, CadastroPorCodigo.gtinValido(it))
        }
    }

    @Test
    fun le_o_produto_da_base_aberta() {
        val achado = CadastroPorCodigo.doOpenFoodFacts(
            OffProdutoDetalhe("Leite Condensado Integral moça", "Nestlé, Moça", 395.0, "g")
        )!!

        assertEquals("Leite Condensado Integral moça", achado.nome)
        assertEquals("Nestlé", achado.marca)
        assertEquals(395.0, achado.quantidadeEmbalagem!!, 0.0)
        assertEquals(UnidadeEmbalagem.GRAMA, achado.unidadeEmbalagem)
    }

    @Test
    fun embalagem_com_unidade_desconhecida_fica_em_branco() {
        val achado = CadastroPorCodigo.doOpenFoodFacts(OffProdutoDetalhe("Ovos", null, 12.0, "pieces"))!!

        assertNull(achado.quantidadeEmbalagem)
        assertNull(achado.unidadeEmbalagem)
    }

    @Test
    fun produto_vazio_na_base_conta_como_nao_achado() {
        assertNull(CadastroPorCodigo.doOpenFoodFacts(OffProdutoDetalhe(" ", null, null, null)))
        assertNull(CadastroPorCodigo.doOpenFoodFacts(null))
    }

    private fun montar(
        gtin: String = "7891000100103",
        nome: String = "Leite Moça",
        embalagem: String = "395",
        unidade: UnidadeEmbalagem? = UnidadeEmbalagem.GRAMA,
        categoria: CategoriaAlimento? = CategoriaAlimento.LATICINIOS,
        quantidade: Int = 2,
        validade: String? = "2027-01-10"
    ) = CadastroPorCodigo.montar(gtin, nome, "Nestlé", embalagem, unidade, categoria, quantidade, validade)

    @Test
    fun monta_o_pedido_da_core() {
        val pedido = montar().getOrThrow()

        assertEquals("7891000100103", pedido.gtin)
        assertEquals(395.0, pedido.packageQuantity, 0.0)
        assertEquals("g", pedido.unitOfMeasure)
        assertEquals("10000025", pedido.gpcCode)
        assertEquals(2, pedido.quantity)
        assertEquals("2027-01-10", pedido.expiryDate)
    }

    @Test
    fun embalagem_com_virgula_e_unidade_mandam_o_enum_da_core() {
        val pedido = montar(embalagem = "1,5", unidade = UnidadeEmbalagem.LITRO).getOrThrow()
        assertEquals(1.5, pedido.packageQuantity, 0.0)
        assertEquals("l", pedido.unitOfMeasure)

        assertEquals("unit", montar(unidade = UnidadeEmbalagem.UNIDADE).getOrThrow().unitOfMeasure)
    }

    @Test
    fun cada_falta_tem_a_sua_mensagem() {
        assertEquals("Código de barras inválido. Leia de novo.", montar(gtin = "123").exceptionOrNull()?.message)
        assertEquals("Informe o nome do produto.", montar(nome = " ").exceptionOrNull()?.message)
        assertEquals("Informe o tamanho da embalagem (ex.: 395).", montar(embalagem = "0").exceptionOrNull()?.message)
        assertEquals("Escolha a unidade da embalagem.", montar(unidade = null).exceptionOrNull()?.message)
        assertEquals("Escolha a categoria do produto.", montar(categoria = null).exceptionOrNull()?.message)
    }

    @Test
    fun json_enviado_tem_exatamente_os_campos_da_core() {
        // Campos do record FoodRegistrationRequest da NexUs-Core (conferido no fonte).
        val core = setOf(
            "gtin", "name", "productBrand", "packageQuantity", "unitOfMeasure", "gpcCode", "quantity", "expiryDate"
        )

        val json = Gson().toJsonTree(montar().getOrThrow()) as JsonObject

        assertEquals(core, json.keySet())
    }

    @Test
    fun incluiFoodImageUrlNoContratoDaCoreQuandoAConsultaTemFoto() {
        val pedido = CadastroPorCodigo.montar(
            "7891000100103", "Leite Moça", "Nestlé", "395", UnidadeEmbalagem.GRAMA,
            CategoriaAlimento.LATICINIOS, 1, null, imagemUrl = " https://img.example/leite.jpg ",
        ).getOrThrow()
        val json = Gson().toJsonTree(pedido).asJsonObject
        assertEquals("https://img.example/leite.jpg", json["foodImageUrl"].asString)
    }

    @Test
    fun cada_categoria_tem_um_codigo_gpc_proprio_de_8_digitos() {
        CategoriaAlimento.entries.forEach { assertTrue(it.name, Regex("\\d{8}").matches(it.gpc)) }
        assertEquals(CategoriaAlimento.entries.size, CategoriaAlimento.entries.map { it.gpc }.toSet().size)
    }
}
