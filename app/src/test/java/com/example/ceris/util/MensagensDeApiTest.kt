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
    fun erro_401_usa_a_frase_do_servidor_e_so_sem_ela_fala_em_sessao() {
        // ms-ia recusando a chave do app: refazer o login nao resolve.
        assertEquals("Invalid API key", mensagemDeErro(401, "{\"detail\":\"Invalid API key\"}"))
        assertEquals("Sessão expirada. Entre novamente.", mensagemDeErro(401, null))
    }

    @Test
    fun erro_404_usa_a_frase_do_servidor_quando_ela_vem() {
        assertEquals(
            "Leitura não encontrada. Envie a nota de novo.",
            mensagemDeErro(404, "{\"detail\":\"Leitura não encontrada. Envie a nota de novo.\"}")
        )
        assertEquals("Essa lista não existe mais.", mensagemDeErro(404, "pagina html"))
    }

    @Test
    fun falha_de_rede_aponta_o_lugar_certo() {
        fun primeiraLinha(t: Throwable) = mensagemDeFalha(t).lineSequence().first()

        assertEquals("Sem conexão, ou o endereço da API mudou.", primeiraLinha(UnknownHostException("ceris")))
        assertEquals("Servidor fora do ar. Não foi possível conectar.", primeiraLinha(ConnectException("recusada")))
        // Conexao aceita e fechada sem resposta: e o servidor, nao a internet do aparelho.
        assertEquals(
            "O servidor não respondeu. O serviço parece estar fora do ar.",
            primeiraLinha(IOException("unexpected end of stream", EOFException()))
        )
    }
}
