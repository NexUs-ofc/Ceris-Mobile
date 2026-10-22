package com.example.ceris.repository

import com.example.ceris.model.dto.RecipeIngredientResponse
import com.example.ceris.model.dto.RecipeResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMappingTest {
    @Test
    fun contratoDoCorePreservaIngredientesPorcoesEPassos() {
        val dto =
            Gson().fromJson(
                """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "title": "Frango ao molho",
                  "servingSize": 4,
                  "ingredients": [{
                    "foodId": 12,
                    "foodName": "peito de frango",
                    "requiredQuantity": "500g",
                    "mandatory": true,
                    "possibleSubstitutes": [13]
                  }],
                  "instructions": "1. Corte o frango.\n2. Cozinhe em fogo baixo.",
                  "createdBy": "ai"
                }
                """.trimIndent(),
                RecipeResponse::class.java,
            )
        val receita = dto.paraReceita()

        assertEquals("507f1f77bcf86cd799439011", receita.id)
        assertEquals("Frango ao molho", receita.nome)
        assertEquals(4, receita.porcoes)
        assertEquals(listOf("500g peito de frango"), receita.ingredientes)
        assertEquals(listOf("Corte o frango.", "Cozinhe em fogo baixo."), receita.passos)
        assertEquals(12, dto.ingredients?.single()?.foodId)
        assertEquals(listOf(13), dto.ingredients?.single()?.possibleSubstitutes)
    }

    @Test
    fun receitaAntigaSemMetadadosNaoRecebeFotoTempoOuDificuldadeInventados() {
        val receita = RecipeResponse(id = "recipe", title = "Sopa").paraReceita()

        assertEquals("Sopa", receita.nome)
        assertNull(receita.tempoPreparo)
        assertNull(receita.imagemUrl)
        assertNull(receita.dificuldade)
        assertNull(receita.porcoes)
        assertTrue(receita.ingredientes.isEmpty())
        assertTrue(receita.passos.isEmpty())
    }

    @Test
    fun ingredientesSemQuantidadePreservamNome() {
        val receita =
            RecipeResponse(
                id = "recipe",
                ingredients = listOf(RecipeIngredientResponse(foodId = 1, foodName = "Sal e pimenta a gosto")),
            ).paraReceita()

        assertEquals(listOf("Sal e pimenta a gosto"), receita.ingredientes)
    }

    @Test
    fun passoEmTextoLivreNaoPerdePontuacaoNemTempoDeCozimento() {
        val receita =
            RecipeResponse(
                id = "recipe",
                instructions = "Misture 1,5 litro de água. Cozinhe por 15 minutos.",
            ).paraReceita()

        assertEquals(listOf("Misture 1,5 litro de água. Cozinhe por 15 minutos."), receita.passos)
    }

    @Test
    fun passosNumeradosComParentesesELinhasVaziasNaoDuplicamNumeracao() {
        val receita =
            RecipeResponse(
                id = "recipe",
                instructions = "\n1) Corte o frango em cubos.\n\n2) Misture bem.\n",
            ).paraReceita()

        assertEquals(listOf("Corte o frango em cubos.", "Misture bem."), receita.passos)
    }

    @Test
    fun quantidadeDecimalNoInicioDoPreparoNaoPerdeParteDoNumero() {
        val receita =
            RecipeResponse(
                id = "recipe",
                instructions = "1.5 litros de água devem ferver antes de adicionar o arroz.",
            ).paraReceita()

        assertEquals(listOf("1.5 litros de água devem ferver antes de adicionar o arroz."), receita.passos)
    }

    @Test
    fun preparoVazioNaoCriaPassoEmBranco() {
        val receita = RecipeResponse(id = "recipe", instructions = " \n\n ").paraReceita()

        assertTrue(receita.passos.isEmpty())
    }
}
