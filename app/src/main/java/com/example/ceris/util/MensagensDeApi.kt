package com.example.ceris.util

import com.example.ceris.model.dto.ApiError
import com.example.ceris.repository.AcompanhamentoDeLeitura
import com.example.ceris.repository.ShoppingListRepository
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

// Repetir este `when` em cada ViewModel faria as mensagens divergirem.
fun mensagemDeErro(statusCode: Int, errorBody: String?): String {
    val doServidor = ApiError.parse(errorBody)?.readableMessage()

    return when (statusCode) {
        400 -> doServidor?.let { "Dados inválidos:\n$it" }
            ?: "Dados inválidos. Confira os campos e tente novamente."
        // Nem todo 401 e de sessao: o ms-ia responde 401 com
        // {"detail":"Invalid API key"} quando a chave do app nao esta na
        // lista dele. Dizer "sessao expirada" ali manda a pessoa refazer o
        // login para um problema que o login nao resolve.
        401 -> doServidor ?: "Sessão expirada. Entre novamente."
        403 -> doServidor ?: "Você não tem acesso a esta lista."
        404 -> doServidor ?: "Essa lista não existe mais."
        else -> doServidor ?: "Erro $statusCode. Algo deu errado, tente novamente!"
    }
}

/**
 * Procura um tipo de exceção na cadeia de causas, não só no topo.
 *
 * O OkHttp embrulha: quando o servidor fecha sem responder, o que chega é um
 * `IOException("unexpected end of stream on http://...")` com um `EOFException`
 * dentro. Olhando só o throwable de cima, esse caso caía no ramo genérico e
 * virava "a conexão caiu" — verdadeiro, mas sem dizer de quem é a culpa.
 */
private fun Throwable.temNaCadeia(vararg tipos: Class<out Throwable>): Boolean {
    var atual: Throwable? = this

    while (atual != null) {
        if (tipos.any { it.isInstance(atual) }) {
            return true
        }

        atual = atual.cause.takeIf { it !== atual }
    }

    return false
}

/**
 * Traduz a falha de rede para uma frase que aponte o lugar certo.
 *
 * A ordem não é estética: quase tudo aqui é IOException, e o ramo genérico no
 * fim engoliria o resto se viesse antes. ConnectException herda de
 * SocketException, então também precisa vir primeiro.
 *
 * "Sem conexão" ficou reservado para o UnknownHost, que é como a falta de rede
 * aparece de verdade — o DNS para antes de abrir qualquer socket. Conexão
 * aceita e encerrada sem resposta não é internet do aparelho: é o servidor do
 * outro lado. Foi exatamente o caso da Core — `unexpected end of stream`, com
 * `EOFException: limit=0` dentro — enquanto a Auth respondia normalmente, no
 * mesmo aparelho e na mesma rede.
 */
fun mensagemDeFalha(throwable: Throwable): String {
    val frase = when {
        throwable is com.example.ceris.repository.ChatRepository.RespostaCortada ->
            throwable.message.orEmpty()

        throwable is com.example.ceris.repository.NotaFiscalRepository.LeituraCortada ->
            throwable.message.orEmpty()

        throwable is ShoppingListRepository.ItemSemIdentificador -> throwable.message.orEmpty()

        throwable is ShoppingListRepository.SemSessao -> "Sessão expirada. Entre novamente."

        throwable is AcompanhamentoDeLeitura.LeituraDemorou -> throwable.message.orEmpty()

        // Tempo limite do cliente. O callTimeout do OkHttp lanca
        // InterruptedIOException("timeout"), que nao e SocketTimeoutException, e
        // ao cancelar fecha o socket: sem este ramo antes do de SocketException,
        // demora do servidor aparecia como "A conexao foi interrompida pelo
        // servidor" (provado no TempoLimiteTest).
        throwable is SocketTimeoutException ||
            (throwable is InterruptedIOException && throwable.message == "timeout") ->
            "O servidor demorou para responder. Tente de novo."

        throwable.temNaCadeia(UnknownHostException::class.java) ->
            "Sem conexão, ou o endereço da API mudou."

        throwable.temNaCadeia(ConnectException::class.java) ->
            "Servidor fora do ar. Não foi possível conectar."

        throwable.temNaCadeia(UnknownServiceException::class.java) ->
            "A API está em http e esta versão só aceita https."

        throwable.temNaCadeia(SSLException::class.java) ->
            "Falha de segurança na conexão com o servidor."

        // O servidor aceitou a conexão e desligou sem responder nada. É o que
        // um balanceador de pé sem nenhum backend saudável atrás faz.
        throwable.temNaCadeia(EOFException::class.java, ProtocolException::class.java) ->
            "O servidor não respondeu. O serviço parece estar fora do ar."

        // "Connection reset", "Software caused connection abort": o socket foi
        // derrubado no meio, do outro lado.
        throwable.temNaCadeia(SocketException::class.java) ->
            "A conexão foi interrompida pelo servidor."

        throwable is IOException -> "A conexão com o servidor caiu. Tente de novo."

        else -> "Um erro inesperado aconteceu: ${throwable.message}"
    }

    // Detalhes tecnicos pertencem ao Logcat. Colocar o nome da excecao no
    // Toast fazia builds de teste exibirem mensagens como [IOException] em
    // todas as telas e quebrava a identidade visual.
    return frase
}
