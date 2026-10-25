package com.example.ceris.repository

import com.example.ceris.model.PassoReceita
import com.example.ceris.model.RecipeDifficulty
import com.example.ceris.model.dto.RecipeIngredientResponse
import com.example.ceris.model.dto.RecipeResponse
import com.example.ceris.model.dto.RecipeStepResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMappingTest {
    @Test
    fun contratoDoCorePreservaMetadadosIngredientesEPassos() {
        val dto =
            Gson().fromJson(
                """
                {
                  "id": "507f1f77bcf86cd799439011",
                  "title": "Frango ao molho",
                  "servingSize": 4,
                  "estimatedTime": " 40 min ",
                  "difficulty": "EASY",
                  "ingredients": [{
                    "foodId": 12,
                    "foodName": "peito de frango",
                    "requiredQuantity": "500g",
                    "mandatory": true,
                    "possibleSubstitutes": [13]
                  }],
                  "steps": [
                    {"title": "Cortar o frango", "description": "Corte o frango em cubos."},
                    {"title": "Cozinhar", "description": "Cozinhe em fogo baixo."}
                  ],
                  "createdBy": "ai"
                }
                """.trimIndent(),
                RecipeResponse::class.java,
            )
        val receita = dto.paraReceita()

        assertEquals("507f1f77bcf86cd799439011", receita.id)
        assertEquals("Frango ao molho", receita.nome)
        assertEquals(4, receita.porcoes)
        assertEquals("40 min", receita.tempoPreparo)
        assertEquals(RecipeDifficulty.EASY, receita.dificuldade)
        assertEquals(listOf("500g peito de frango"), receita.ingredientes)
        assertEquals(
            listOf(PassoReceita("Cortar o frango", "Corte o frango em cubos."), PassoReceita("Cozinhar", "Cozinhe em fogo baixo.")),
            receita.passos,
        )
        assertEquals(12, dto.ingredients?.single()?.foodId)
        assertEquals(listOf(13), dto.ingredients?.single()?.possibleSubstitutes)
    }

    @Test
    fun receitaAntigaSemMetadadosNaoRecebeTempoOuDificuldadeInventados() {
        val receita =
            Gson().fromJson("""{"id":"recipe","title":"Sopa"}""", RecipeResponse::class.java).paraReceita()

        assertEquals("Sopa", receita.nome)
        assertNull(receita.tempoPreparo)
        assertNull(receita.dificuldade)
        assertNull(receita.porcoes)
        assertTrue(receita.ingredientes.isEmpty())
        assertTrue(receita.passos.isEmpty())
    }

    @Test
    fun dificuldadesDoCoreSaoExibidasEmPortugues() {
        val dificuldades =
            listOf(
                "EASY" to (RecipeDifficulty.EASY to "Fácil"),
                "MEDIUM" to (RecipeDifficulty.MEDIUM to "Média"),
                "HARD" to (RecipeDifficulty.HARD to "Difícil"),
            )
        dificuldades.forEach { (valor, esperado) ->
            val receita =
                Gson().fromJson("""{"id":"recipe","difficulty":"$valor"}""", RecipeResponse::class.java).paraReceita()

            assertEquals(esperado.first, receita.dificuldade)
            assertEquals(esperado.second, receita.dificuldade?.rotulo)
        }
    }

    @Test
    fun metadadosNulosOuVaziosSaoOmitidos() {
        val receita =
            Gson().fromJson(
                """{"id":"recipe","estimatedTime":" \n ","servingSize":0,"difficulty":null}""",
                RecipeResponse::class.java,
            ).paraReceita()

        assertNull(receita.tempoPreparo)
        assertNull(receita.porcoes)
        assertNull(receita.dificuldade)
    }

    @Test
    fun dificuldadeDesconhecidaNaoImpedeAExibicaoDaReceita() {
        val receita =
            Gson().fromJson(
                """{"id":"recipe","title":"Sopa","estimatedTime":"30 minutos","difficulty":"UNKNOWN"}""",
                RecipeResponse::class.java,
            ).paraReceita()

        assertEquals("Sopa", receita.nome)
        assertEquals("30 minutos", receita.tempoPreparo)
        assertNull(receita.dificuldade)
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
    fun descricaoPreservaParagrafosPontuacaoEQuantidades() {
        val descricao = "Misture 1,5 litro de água.\n\nCozinhe por 15 minutos."
        val receita =
            RecipeResponse(
                id = "recipe",
                steps = listOf(RecipeStepResponse("Misturar e cozinhar", descricao)),
            ).paraReceita()

        assertEquals(listOf(PassoReceita("Misturar e cozinhar", descricao)), receita.passos)
    }

    @Test
    fun passosEstruturadosPreservamOrdemETitulosRepetidos() {
        val receita =
            RecipeResponse(
                id = "recipe",
                steps =
                    listOf(
                        RecipeStepResponse("Misturar", "Misture os ingredientes secos."),
                        RecipeStepResponse("Misturar", "Acrescente o leite e misture novamente."),
                    ),
            ).paraReceita()

        assertEquals(
            listOf(
                PassoReceita("Misturar", "Misture os ingredientes secos."),
                PassoReceita("Misturar", "Acrescente o leite e misture novamente."),
            ),
            receita.passos,
        )
    }

    @Test
    fun removeApenasEspacosExternosSemAlterarOTituloOuADescricao() {
        val receita =
            RecipeResponse(
                id = "recipe",
                steps = listOf(RecipeStepResponse("  Preparar 1.5 litros de água  ", " \nFerva a água.\nMisture bem. \n")),
            ).paraReceita()

        assertEquals(listOf(PassoReceita("Preparar 1.5 litros de água", "Ferva a água.\nMisture bem.")), receita.passos)
    }

    @Test
    fun respostaSemStepsNaoInventaTitulosAPartirDeInstructions() {
        val receita =
            Gson().fromJson(
                """{"id":"recipe","instructions":"1. Corte o frango.\n2. Cozinhe."}""",
                RecipeResponse::class.java,
            ).paraReceita()

        assertTrue(receita.passos.isEmpty())
    }

    @Test
    fun entradasNulasOuTotalmenteVaziasNaoCriamCardsSemConteudo() {
        val receita =
            Gson().fromJson(
                """{"id":"recipe","steps":[null,{}, {"title":" ","description":" \n "}]}""",
                RecipeResponse::class.java,
            ).paraReceita()

        assertTrue(receita.passos.isEmpty())
    }

    @Test
    fun etapasParciaisPreservamTodoConteudoRecebido() {
        val receita =
            RecipeResponse(
                id = "recipe",
                steps = listOf(RecipeStepResponse(title = "Cortar"), RecipeStepResponse(description = "Refogue a cebola.")),
            ).paraReceita()

        assertEquals(listOf(PassoReceita("Cortar", ""), PassoReceita("", "Refogue a cebola.")), receita.passos)
    }

    @Test
    fun stepsTemPrioridadeSobreCampoLegadoNoMesmoDocumento() {
        val receita =
            Gson().fromJson(
                """
                {
                  "id":"recipe",
                  "instructions":"Texto antigo",
                  "steps":[{"title":"Cortar","description":"Corte em cubos."}]
                }
                """.trimIndent(),
                RecipeResponse::class.java,
            ).paraReceita()

        assertEquals(listOf(PassoReceita("Cortar", "Corte em cubos.")), receita.passos)
    }
}
