package com.example.ceris.api

import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.util.mensagemDeErro
import com.example.ceris.util.mensagemDeFalha
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** "Bearer <token>" da sessao atual, ou nulo sem sessao. */
fun SessionManager.bearer(): String? =
    getString(SessionKeys.ACCESS_TOKEN, "").takeIf { it.isNotBlank() }?.let { "Bearer $it" }

/**
 * Enfileira a chamada e entrega o corpo, ou a mensagem de erro ja pronta para
 * a tela (no formato de cada servico, pelo MensagensDeApi). Para as telas que
 * so mostram o resultado e nao precisam distinguir codigo de erro.
 */
fun <T> Call<T>.aoTerminar(onSucesso: (T?) -> Unit, onFalha: (String) -> Unit) {
    enqueue(
        object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    onSucesso(response.body())
                } else {
                    onFalha(mensagemDeErro(response.code(), runCatching { response.errorBody()?.string() }.getOrNull()))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (!call.isCanceled) onFalha(mensagemDeFalha(t))
            }
        }
    )
}
