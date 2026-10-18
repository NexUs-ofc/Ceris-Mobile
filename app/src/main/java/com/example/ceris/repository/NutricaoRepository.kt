package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.openfoodfacts.OpenFoodFactsAPI
import com.example.ceris.model.Nutricao
import com.example.ceris.model.dto.OffBusca
import com.example.ceris.model.dto.OffProduto
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.Normalizer

/**
 * Tabela nutricional do produto, pela busca publica do Open Food Facts.
 *
 * So devolve dado quando o casamento com o produto do estoque e seguro: a
 * marca bate e quase todas as palavras do nome aparecem no produto encontrado.
 * Na duvida, devolve nulo e a tela nao mostra a tabela.
 */
class NutricaoRepository(private val api: OpenFoodFactsAPI) {

    fun buscar(nome: String, marca: String?, aoTerminar: (Nutricao?) -> Unit) {
        if (marca.isNullOrBlank()) return aoTerminar(null)

        api.buscarPorMarca(marca).enqueue(object : Callback<OffBusca> {
            override fun onResponse(call: Call<OffBusca>, response: Response<OffBusca>) {
                val produtos = response.body()?.products.orEmpty()
                aoTerminar(escolher(nome, marca, produtos))
            }

            override fun onFailure(call: Call<OffBusca>, t: Throwable) {
                Log.w(TAG, "tabela nutricional indisponivel", t)
                aoTerminar(null)
            }
        })
    }

    private companion object {
        const val TAG = "NutricaoRepo"
    }
}

/** Fracao minima das palavras do nome que precisa aparecer no produto encontrado. */
internal const val COBERTURA_MINIMA = 0.7

/**
 * Escolhe o produto da lista que corresponde ao item do estoque. Funcao pura,
 * para os testes cobrirem o casamento sem rede.
 */
internal fun escolher(nome: String, marca: String, produtos: List<OffProduto>): Nutricao? {
    val termos = palavras(nome)
    val marcaPalavras = palavras(marca)
    if (termos.isEmpty() || marcaPalavras.isEmpty()) return null

    return produtos
        .mapNotNull { produto ->
            val encontradas = palavras("${produto.productName.orEmpty()} ${produto.brands.orEmpty()}")
            val cobertura = termos.count { it in encontradas }.toDouble() / termos.size
            val marcaConfere = marcaPalavras.any { it in encontradas }

            // Sem energia cadastrada a tabela nao serve para nada.
            val temTabela = produto.nutriments?.numero("energy-kcal_100g") != null

            if (temTabela && marcaConfere && cobertura >= COBERTURA_MINIMA) cobertura to produto else null
        }
        .maxByOrNull { it.first }
        ?.let { paraNutricao(it.second) }
}

internal fun paraNutricao(produto: OffProduto): Nutricao {
    val valores = produto.nutriments
    return Nutricao(
        imagemUrl = produto.imageFrontUrl?.takeIf { it.isNotBlank() },
        energiaKcal = valores.numero("energy-kcal_100g"),
        carboidratos = valores.numero("carbohydrates_100g"),
        proteinas = valores.numero("proteins_100g"),
        gorduras = valores.numero("fat_100g"),
        gordurasSaturadas = valores.numero("saturated-fat_100g"),
        fibras = valores.numero("fiber_100g"),
        sodioGramas = valores.numero("sodium_100g")
    )
}

private fun JsonObject?.numero(chave: String): Double? {
    val valor = this?.get(chave) ?: return null
    return if (valor.isJsonPrimitive && valor.asJsonPrimitive.isNumber) valor.asDouble else null
}

/** Palavras com 3 letras ou mais, sem acento e sem pontuacao: "Piracanjuba 1L" vira {piracanjuba}. */
internal fun palavras(texto: String): Set<String> =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 3 }
        .toSet()
