package com.example.ceris.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Frase que aparece na tela quando a API responde erro ou nao responde. */
class MensagensDeApiTest {

    @Test
    fun erro_401_nao_exibe_detalhe_tecnico_do_servidor() {
        assertEquals(
            "O serviço está temporariamente indisponível. Tente novamente em instantes.",
            mensagemDeErro(401, "{\"detail\":\"Invalid API key\"}")
        )
        assertEquals(
            "Sua sessão expirou. Entre novamente para continuar.",
            mensagemDeErro(401, null)
        )
    }

    @Test
    fun erro_404_usa_a_frase_do_servidor_quando_ela_vem() {
        assertEquals(
            "Leitura não encontrada. Envie a nota de novo.",
            mensagemDeErro(404, "{\"detail\":\"Leitura não encontrada. Envie a nota de novo.\"}")
        )
        assertEquals(
            "Este conteúdo não está mais disponível. Atualize a tela e tente novamente.",
            mensagemDeErro(404, "pagina html")
        )
    }

    @Test
    fun falha_de_rede_aponta_o_lugar_certo() {
        fun primeiraLinha(t: Throwable) = mensagemDeFalha(t).lineSequence().first()

        assertEquals("Confira sua conexão com a internet e tente novamente.", primeiraLinha(UnknownHostException("ceris")))
        assertEquals("Não foi possível conectar ao serviço. Tente novamente em instantes.", primeiraLinha(ConnectException("recusada")))
        // Conexao aceita e fechada sem resposta: e o servidor, nao a internet do aparelho.
        assertEquals(
            "O serviço não respondeu. Tente novamente em instantes.",
            primeiraLinha(IOException("unexpected end of stream", EOFException()))
        )
    }
}
