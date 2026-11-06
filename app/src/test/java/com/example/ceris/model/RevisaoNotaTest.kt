package com.example.ceris.model

import com.example.ceris.model.dto.*
import org.junit.Assert.*
import org.junit.Test

class RevisaoNotaTest {
    private val produto = FoodLookupProduct("7891000100103", "Leite integral", "Marca QA", null, 1000.0, "ml", "10000025")
    private val nota = listOf(ReceiptItem("LEITE INTEGRAL", quantity = 2.0), ReceiptItem("SEM CORRESPONDENCIA"))

    @Test fun somenteEncontradosValidosEntramNaRevisao() {
        val result = revisarNota(nota, FoodLookupResponse(listOf(
            FoodLookupMatch(1, "NOT_FOUND"), FoodLookupMatch(0, "FOUND", produto),
        )))
        assertEquals(1, result.itens.size)
        assertEquals(1, result.ignorados)
        assertEquals(0, result.indisponiveis)
        assertEquals("Leite integral", result.itens.single().nomeNaNota)
        assertEquals(2, result.itens.single().quantidade)
        assertEquals("un", result.itens.single().unidade)
        assertEquals(1000.0, result.itens.single().produtoEncontrado!!.packageQuantity!!, 0.0)
        assertTrue(result.itens.single().incluir)
    }

    @Test fun ambiguosIndisponiveisEIndicesDuplicadosNaoEntram() {
        val response = FoodLookupResponse(listOf(FoodLookupMatch(0, "AMBIGUOUS"), FoodLookupMatch(1, "UNAVAILABLE")))
        val result = revisarNota(nota, response)
        assertTrue(result.itens.isEmpty())
        assertEquals(2, result.ignorados)
        assertEquals(1, result.indisponiveis)
        assertEquals(listOf("LEITE INTEGRAL"), result.nomesNaoEncontrados)
        assertTrue(revisarNota(nota, FoodLookupResponse(listOf(
            FoodLookupMatch(0, "FOUND", produto), FoodLookupMatch(0, "FOUND", produto),
        ))).itens.isEmpty())
    }

    @Test fun respostaFoundInvalidaNuncaFabricaIdentificadorOuEmbalagem() {
        listOf(produto.copy(gtin = "123"), produto.copy(packageQuantity = Double.NaN),
            produto.copy(packageQuantity = -1.0), produto.copy(unitOfMeasure = "invalid")).forEach {
            assertTrue(revisarNota(nota, FoodLookupResponse(listOf(FoodLookupMatch(0, "FOUND", it)))).itens.isEmpty())
        }
    }

    @Test fun cadastroPreservaGtinEmbalagemEQuantidadeConfirmada() {
        val request = produto.cadastro(3, "2027-01-01")
        assertEquals("7891000100103", request.gtin)
        assertEquals(1000.0, request.packageQuantity, 0.0)
        assertEquals(3, request.quantity)
        assertEquals("2027-01-01", request.expiryDate)
    }

    @Test fun quantidadePonderadaDaNotaViraContagemDePacotes() {
        val chocolate = produto.copy(name = "Nescau", packageQuantity = 400.0, unitOfMeasure = "g")
        val notaPonderada = listOf(ReceiptItem("NESCAU 400 G", quantity = 800.0, unit = "g"))
        val revisao = revisarNota(notaPonderada, FoodLookupResponse(listOf(FoodLookupMatch(0, "FOUND", chocolate))))

        assertEquals(2, revisao.itens.single().quantidade)
        assertEquals("un", revisao.itens.single().unidade)
    }

    @Test fun itensSemCorrespondenciaIncluemNomeParaAvisoVisivel() {
        val revisao = revisarNota(nota, FoodLookupResponse(listOf(
            FoodLookupMatch(0, "UNAVAILABLE"), FoodLookupMatch(1, "NOT_FOUND"),
        )))

        assertEquals(listOf("LEITE INTEGRAL", "SEM CORRESPONDENCIA"), revisao.nomesIgnorados)
        assertEquals(2, revisao.ignorados)
        assertEquals(1, revisao.indisponiveis)
    }
}
