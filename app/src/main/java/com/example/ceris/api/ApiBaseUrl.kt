package com.example.ceris.api

import com.example.ceris.BuildConfig

/**
 * Endereco de cada API, lido do local.properties pelo BuildConfig.
 *
 * Existe porque esses enderecos mudam toda hora e o Retrofit e rigoroso com
 * eles: baseUrl vazia, sem esquema ou sem a barra no fim levanta
 * IllegalArgumentException dentro do construtor do ViewModel, antes de a tela
 * aparecer. O app fechava sem mensagem nenhuma, parecendo bug de codigo,
 * quando era uma linha faltando no local.properties.
 *
 * Aqui a barra entra quando falta, o https entra quando so veio o dominio, e
 * endereco ausente vira uma reserva valida: o app abre, a chamada falha como
 * falha de rede e a tela consegue dizer o que houve.
 */
object ApiBaseUrl {
    /** A mesma sentinela que o GoogleSignInHelper usa para campo em branco. */
    const val NAO_CONFIGURADA = "PREENCHER"

    /**
     * Endereco de reserva para quando nao ha configuracao. Precisa ser valido
     * para o Retrofit aceitar, e localhost nunca responde no celular — a
     * falha aparece como erro de conexao, nao como app fechando.
     */
    const val RESERVA = "http://localhost/"

    val auth: String = normalizar(BuildConfig.AUTH_API_BASE_URL)

    val core: String = normalizar(BuildConfig.CORE_API_BASE_URL)

    val viaCep: String = normalizar(BuildConfig.VIA_CEP_URL)

    /** ms-ia, a Cerita. */
    val ia: String = normalizar(BuildConfig.IA_API_BASE_URL)

    /** Historico e cadastro de instalacoes push. */
    val notify: String = normalizar(BuildConfig.NOTIFY_API_BASE_URL)

    /** Falso quando a chave nao foi preenchida no local.properties. */
    fun configurada(url: String): Boolean = url != RESERVA

    fun normalizar(bruto: String?): String {
        val limpo = bruto?.trim().orEmpty()

        if (limpo.isEmpty() || limpo.startsWith(NAO_CONFIGURADA)) {
            return RESERVA
        }

        val comEsquema =
            if (limpo.startsWith("http://") || limpo.startsWith("https://")) {
                limpo
            } else {
                "https://$limpo"
            }

        return if (comEsquema.endsWith("/")) comEsquema else "$comEsquema/"
    }
}
