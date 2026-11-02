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
    val erro = ApiError.parse(errorBody)
    val doServidor = erro?.readableMessage()?.takeIf {
        it.isNotBlank() && !Regex(
            "(?i)\\b(invalid api key|bad request|not found|internal server error|unauthorized|forbidden|unprocessable entity|too many requests|service unavailable|gateway timeout|exception|traceback|must match|request failed)\\b"
        ).containsMatchIn(it)
    }

    return when (statusCode) {
        400 -> doServidor ?: "Confira os dados informados e tente novamente."
        401 -> if (erro?.readableMessage()?.contains("invalid api key", ignoreCase = true) == true) {
            "O serviço está temporariamente indisponível. Tente novamente em instantes."
        } else {
            "Sua sessão expirou. Entre novamente para continuar."
        }
        403 -> "Você não tem permissão para realizar esta ação."
        404 -> doServidor ?: "Este conteúdo não está mais disponível. Atualize a tela e tente novamente."
        409 -> doServidor ?: "Os dados foram alterados. Atualize a tela e tente novamente."
        422 -> "Confira os dados informados e tente novamente."
        429 -> "O serviço está ocupado. Aguarde um pouco e tente novamente."
        in 500..599 -> "O serviço está temporariamente indisponível. Tente novamente em instantes."
        else -> doServidor ?: "Não foi possível concluir agora. Tente novamente em instantes."
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
            "O serviço demorou para responder. Tente novamente."

        throwable.temNaCadeia(UnknownHostException::class.java) ->
            "Confira sua conexão com a internet e tente novamente."

        throwable.temNaCadeia(ConnectException::class.java) ->
            "Não foi possível conectar ao serviço. Tente novamente em instantes."

        throwable.temNaCadeia(UnknownServiceException::class.java) ->
            "Não foi possível estabelecer uma conexão segura. Tente novamente mais tarde."

        throwable.temNaCadeia(SSLException::class.java) ->
            "Não foi possível estabelecer uma conexão segura. Tente novamente mais tarde."

        // O servidor aceitou a conexão e desligou sem responder nada. É o que
        // um balanceador de pé sem nenhum backend saudável atrás faz.
        throwable.temNaCadeia(EOFException::class.java, ProtocolException::class.java) ->
            "O serviço não respondeu. Tente novamente em instantes."

        // "Connection reset", "Software caused connection abort": o socket foi
        // derrubado no meio, do outro lado.
        throwable.temNaCadeia(SocketException::class.java) ->
            "A conexão foi interrompida. Confira sua internet e tente novamente."

        throwable is IOException -> "Não foi possível concluir por causa da conexão. Tente novamente."

        else -> "Algo deu errado. Tente novamente em instantes."
    }

    // Detalhes tecnicos pertencem ao Logcat. Colocar o nome da excecao no
    // Toast fazia builds de teste exibirem mensagens como [IOException] em
    // todas as telas e quebrava a identidade visual.
    return frase
}
