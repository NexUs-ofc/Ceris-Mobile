package com.example.ceris.repository

import com.example.ceris.api.RecipeAPI
import com.example.ceris.api.StockAPI
import com.example.ceris.api.aoTerminar
import com.example.ceris.model.Receita
import com.example.ceris.model.dto.RecipeResponse

/**
 * Receitas salvas da Core. O ingrediente vem so com o id do alimento, entao o
 * nome sai do catalogo; se o catalogo falhar, a receita aparece assim mesmo,
 * so sem os nomes que faltaram.
 */
class ReceitasRepository(private val receitas: RecipeAPI, private val estoque: StockAPI) {

    fun salvas(token: String, onSucesso: (List<Receita>) -> Unit, onFalha: (String) -> Unit) {
        receitas.salvas(token).aoTerminar(
            onSucesso = { lista ->
                estoque.alimentos(token).aoTerminar(
                    onSucesso = { catalogo ->
                        val nomes = catalogo.orEmpty().associate { it.id to it.name }
                        onSucesso(lista.orEmpty().map { paraReceita(it, nomes) })
                    },
                    onFalha = { onSucesso(lista.orEmpty().map { paraReceita(it, emptyMap()) }) }
                )
            },
            onFalha = onFalha
        )
    }

    companion object {
        fun paraReceita(receita: RecipeResponse, nomes: Map<Int, String>) = Receita(
            id = receita.id,
            nome = receita.title.orEmpty(),
            porcoes = receita.servingSize?.takeIf { it > 0 },
            ingredientes = receita.ingredients.orEmpty().mapNotNull { ingrediente ->
                val nome = ingrediente.foodId?.let(nomes::get)
                listOfNotNull(ingrediente.requiredQuantity?.trim()?.takeIf { it.isNotEmpty() }, nome)
                    .joinToString(" de ")
                    .ifBlank { null }
            },
            passos = passos(receita.instructions)
        )

        /**
         * O modo de preparo vem num texto so. Quebra por linha e tira a
         * numeracao que vier escrita ("1.", "2)"), porque a tela numera.
         */
        fun passos(texto: String?): List<String> =
            texto.orEmpty()
                .split('\n')
                .map { it.trim().replace(Regex("^\\d+\\s*[.)\\-]\\s*"), "") }
                .filter { it.isNotEmpty() }
    }
}
