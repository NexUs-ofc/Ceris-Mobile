package com.example.ceris.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Mensagem para a Cerita.
 *
 * O contrato do ms-ia e snake_case, diferente da Core, que e camelCase — por
 * isso o SerializedName aqui e nao nos DTOs antigos. Sem ele o Gson manda
 * "sessionId" e o servidor abre sessao nova a cada mensagem, perdendo o fio da
 * conversa.
 *
 * household_account_id e account_id existem no contrato e ficam de fora de
 * proposito: o controller sobrescreve account_id pelo subject do token, e o
 * household tem default no servidor. Mandar um palpite daqui seria inventar
 * dado que o backend ja sabe.
 */
data class ChatRequest(
    val mensagem: String,
    /** Nulo na primeira mensagem: quem abre a sessao e o servidor. */
    @SerializedName("session_id")
    val sessionId: String? = null
) {
    companion object {
        /** Mesmo teto do LIMITE_MENSAGEM no ms-ia; acima disso volta 422. */
        const val LIMITE_MENSAGEM = 2000
    }
}
