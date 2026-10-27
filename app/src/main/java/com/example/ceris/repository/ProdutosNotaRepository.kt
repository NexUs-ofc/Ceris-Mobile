package com.example.ceris.repository

import com.example.ceris.api.openfoodfacts.OpenFoodFactsAPI
import com.example.ceris.model.CadastroPorCodigo
import com.example.ceris.model.CategoriaAlimento
import com.example.ceris.model.dto.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.Normalizer

/** Busca publica por nome, sem Bearer, antes da confirmacao. Nunca grava produtos. */
class ProdutosNotaRepository(private val api: OpenFoodFactsAPI) {
    private var sequence = 0L
    private var active: Call<OffBusca>? = null

    fun cancelar() {
        sequence++
        active?.cancel()
        active = null
    }

    fun buscar(names: List<String>, done: (FoodLookupResponse) -> Unit) {
        cancelar()
        val current = sequence
        val results = mutableListOf<FoodLookupMatch>()
        val batch = mutableMapOf<String, FoodLookupMatch>()
        val deadline = System.currentTimeMillis() + 60_000
        fun next(index: Int) {
            if (current != sequence) return
            if (index == names.size) { active = null; done(FoodLookupResponse(results)); return }
            val name = names[index].trim()
            val key = normalize(name)
            fun deliver(match: FoodLookupMatch) {
                if (current != sequence) return
                batch[key] = match
                results += match.copy(index = index)
                next(index + 1)
            }
            val cached = synchronized(cache) { cache[key]?.takeIf { it.first > System.currentTimeMillis() }?.second }
            val previous = batch[key] ?: cached
            if (previous != null) { deliver(previous); return }
            if (words(name).size < 2) { deliver(FoodLookupMatch(index, "NOT_FOUND")); return }
            if (System.currentTimeMillis() >= deadline || !reserve()) {
                deliver(FoodLookupMatch(index, "UNAVAILABLE")); return
            }
            val call = api.buscarPorNome(name, campos = FIELDS, tamanho = 20)
            active = call
            call.enqueue(object : Callback<OffBusca> {
                override fun onResponse(call: Call<OffBusca>, response: Response<OffBusca>) {
                    if (current != sequence || call.isCanceled) return
                    if (!response.isSuccessful || response.body() == null) {
                        if (response.code() == 429) synchronized(cache) { blockedUntil = System.currentTimeMillis() + 60_000 }
                        deliver(FoodLookupMatch(index, "UNAVAILABLE"))
                        return
                    }
                    val match = choose(name, response.body()!!.products.orEmpty())
                    synchronized(cache) {
                        if (cache.size >= 500) cache.clear()
                        cache[key] = (System.currentTimeMillis() + 600_000) to match
                    }
                    deliver(match)
                }
                override fun onFailure(call: Call<OffBusca>, t: Throwable) {
                    if (!call.isCanceled && current == sequence) deliver(FoodLookupMatch(index, "UNAVAILABLE"))
                }
            })
        }
        next(0)
    }

    companion object {
        private const val FIELDS = "code,product_name,brands,product_quantity,product_quantity_unit,image_front_url,categories_tags"
        private val cache = mutableMapOf<String, Pair<Long, FoodLookupMatch>>()
        private val requests = java.util.ArrayDeque<Long>()
        private var blockedUntil = 0L
        private fun reserve(): Boolean = synchronized(cache) {
            val now = System.currentTimeMillis()
            while (requests.isNotEmpty() && now - requests.first >= 60_000) requests.removeFirst()
            if (now < blockedUntil || requests.size >= 10) false else { requests.addLast(now); true }
        }
        private fun normalize(value: String) = Normalizer.normalize(value.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9 ]"), " ").trim()
        private fun words(value: String) = normalize(value).split(Regex("\\s+"))
            .filter { (it.length >= 3 || it.all(Char::isDigit) && it.isNotEmpty()) && it !in setOf("un", "und", "unid", "kg", "gr", "ml", "lt", "de", "do", "da") }
        internal fun choose(name: String, products: List<OffProduto>): FoodLookupMatch {
            val requested = words(name)
            if (requested.size < 2) return FoodLookupMatch(0, "NOT_FOUND")
            val matches = products.mapNotNull { product ->
                val candidate = FoodLookupProduct(
                    product.code, product.productName?.trim(), product.brands?.split(",")?.firstOrNull()?.trim(),
                    product.imageFrontUrl?.takeIf { it.startsWith("https://") && it.length <= 500 },
                    product.productQuantity, product.productQuantityUnit?.lowercase(java.util.Locale.ROOT)?.trim(),
                    (CadastroPorCodigo.categoriaDaBase(product.categoriesTags) ?: CategoriaAlimento.OUTROS).gpc,
                )
                val actual = words("${candidate.name.orEmpty()} ${candidate.productBrand.orEmpty()}")
                candidate.takeIf { it.valido() && requested.all { word -> actual.any { found -> found == word || word.length >= 4 && found.startsWith(word) } } }
            }.distinctBy { it.gtin }
            return when (matches.size) {
                0 -> FoodLookupMatch(0, "NOT_FOUND")
                1 -> FoodLookupMatch(0, "FOUND", matches.single())
                else -> FoodLookupMatch(0, "AMBIGUOUS")
            }
        }
    }
}
