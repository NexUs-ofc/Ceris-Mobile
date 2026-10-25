package com.example.ceris.repository

import com.example.ceris.api.RecipeAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.Receita
import com.example.ceris.model.dto.RecipeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class RecipeRepository(
    private val api: RecipeAPI,
    private val sessionManager: SessionManager,
) {
    fun listar(
        onSuccess: (List<Receita>) -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<*>? {
        val session = sessionManager.snapshot() ?: return null.also { onFailure(ShoppingListRepository.SemSessao()) }
        return solicitar(api.listar("Bearer ${session.accessToken}", session), session, onError, onFailure) { receitas ->
            onSuccess(receitas.map { it.paraReceita() })
        }
    }

    fun detalhe(
        id: String,
        onSuccess: (Receita) -> Unit,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): Call<*>? {
        if (!Regex("[a-fA-F0-9]{24}").matches(id)) {
            onError(404, null)
            return null
        }
        val session = sessionManager.snapshot() ?: return null.also { onFailure(ShoppingListRepository.SemSessao()) }
        return solicitar(api.detalhe("Bearer ${session.accessToken}", id, session), session, onError, onFailure) { receita ->
            if (receita.id != id) {
                onFailure(IOException("A receita recebida não corresponde à solicitada."))
            } else {
                onSuccess(receita.paraReceita())
            }
        }
    }

    private fun <T> solicitar(
        chamada: Call<T>,
        session: SessionSnapshot,
        onError: (Int, String?) -> Unit,
        onFailure: (Throwable) -> Unit,
        onSuccess: (T) -> Unit,
    ): Call<T> {
        chamada.enqueue(
            object : Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: Response<T>,
                ) {
                    val erro = runCatching { response.errorBody()?.use { it.string() } }.getOrNull()
                    if (call.isCanceled) return
                    if (!sessionManager.isCurrent(session)) {
                        onFailure(ShoppingListRepository.SemSessao())
                        return
                    }
                    val body = response.body()
                    when {
                        !response.isSuccessful -> onError(response.code(), erro)
                        body == null -> onFailure(IOException("O servidor retornou uma resposta vazia."))
                        else -> onSuccess(body)
                    }
                }

                override fun onFailure(
                    call: Call<T>,
                    t: Throwable,
                ) {
                    if (!call.isCanceled) {
                        onFailure(if (sessionManager.isCurrent(session)) t else ShoppingListRepository.SemSessao())
                    }
                }
            },
        )
        return chamada
    }
}

fun RecipeResponse.paraReceita() =
    Receita(
        id = id,
        nome = title?.trim()?.takeIf { it.isNotEmpty() } ?: "Receita sem título",
        tempoPreparo = estimatedTime?.trim()?.takeIf { it.isNotEmpty() },
        porcoes = servingSize?.takeIf { it > 0 },
        dificuldade = difficulty,
        ingredientes =
            ingredients.orEmpty().map { ingrediente ->
                listOfNotNull(
                    ingrediente.requiredQuantity?.trim()?.takeIf { it.isNotEmpty() },
                    ingrediente.foodName?.trim()?.takeIf { it.isNotEmpty() } ?: "Ingrediente indisponível",
                ).joinToString(" ")
            },
        passos =
            instructions.orEmpty().lineSequence()
                .map { it.trim().replace(Regex("^(?:\\d+[.)](?:\\s+|$)|[-*•]\\s+)"), "").trim() }
                .filter { it.isNotEmpty() }
                .toList(),
    )
