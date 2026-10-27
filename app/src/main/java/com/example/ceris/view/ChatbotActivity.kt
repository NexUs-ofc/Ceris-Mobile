package com.example.ceris.view

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.adapter.ChatAdapter
import com.example.ceris.view.adapter.MensagemChat
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.ChatViewModel

/**
 * Cerita.
 *
 * A tela monta a conversa de abertura do Figma, manda o que a pessoa escreve
 * para o ms-ia e some com os atalhos depois do primeiro envio.
 *
 * A espera aparece no subtítulo do cabeçalho, que já existia, em vez de um
 * balão "digitando": balão falso entraria na lista e teria de ser removido
 * depois, e ao girar a tela voltaria como mensagem de verdade.
 *
 * O inset de baixo vai no campo de envio, e não na raiz: com padding na raiz o
 * fundo estampado pararia antes da borda da tela.
 */
class ChatbotActivity : AppCompatActivity(), ChatViewModel.Listener {

    private lateinit var lista: RecyclerView
    private lateinit var campo: EditText
    private lateinit var sugestoes: View
    private lateinit var btnEnviar: ImageButton
    private lateinit var situacao: TextView

    private val adapter = ChatAdapter()

    private val viewModel: ChatViewModel by viewModels()
    private var sairDepoisDaAvaliacao = false
    private var resetarDepoisDoFechamento = false
    private var fechamentoEmCurso = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_chatbot)

        lista = findViewById(R.id.lista_mensagens)
        campo = findViewById(R.id.campo_mensagem)
        sugestoes = findViewById(R.id.sugestoes)
        btnEnviar = findViewById(R.id.btn_enviar)
        situacao = findViewById(R.id.situacao_bot)

        aplicarInsets()

        viewModel.listener = this
        viewModel.init(SessionManager(this))
        sairDepoisDaAvaliacao = savedInstanceState?.getBoolean(STATE_EXIT_AFTER_RATING) ?: false
        resetarDepoisDoFechamento = savedInstanceState?.getBoolean(STATE_RESET_AFTER_CLOSE) ?: false

        supportFragmentManager.setFragmentResultListener(
            AvaliacaoSecaoDialog.RESULT_KEY, this
        ) { _, resultado ->
            val id = resultado.getString(AvaliacaoSecaoDialog.KEY_SESSION)
            val salvar = resultado.getBoolean(AvaliacaoSecaoDialog.KEY_SAVE)
            if (salvar && id != null) {
                val nota = resultado.getInt(AvaliacaoSecaoDialog.KEY_RATING)
                val feedback = resultado.getString(AvaliacaoSecaoDialog.KEY_COMMENT)
                viewModel.avaliarSessao(id, nota, feedback) {
                    if (sairDepoisDaAvaliacao) finish()
                }
            } else if (sairDepoisDaAvaliacao) {
                finish()
            }
        }

        lista.adapter = adapter
        adapter.trocar(
            listOf(
                MensagemChat.Divisor(getString(R.string.chat_hoje)),
                MensagemChat.DaCerita(getString(R.string.chat_abertura))
            )
        )

        findViewById<View>(R.id.btn_voltar).setOnClickListener { sairDaTela() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = sairDaTela()
        })

        btnEnviar.setOnClickListener { enviarOEscrito() }

        findViewById<ImageButton>(R.id.btn_encerrar).setOnClickListener {
            if (viewModel.temSessao) {
                sairDepoisDaAvaliacao = false
                resetarDepoisDoFechamento = true
                fecharSecao()
            }
        }

        campo.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEND) {
                enviarOEscrito()
                true
            } else {
                false
            }
        }

        listOf(R.id.sugestao_receita, R.id.sugestao_lista, R.id.sugestao_vencendo).forEach { id ->
            findViewById<TextView>(id).setOnClickListener { atalho ->
                enviar((atalho as TextView).text.toString())
            }
        }
    }

    /**
     * A barra do sistema empurra só o rodapé.
     *
     * O cabeçalho pega o inset de cima como padding próprio para continuar
     * colado no topo, com o desenho dele indo até debaixo da barra de status.
     */
    private fun aplicarInsets() {
        val cabecalho = findViewById<View>(R.id.cabecalho)
        val barra = findViewById<View>(R.id.barra_envio)
        val alturaCabecalho = cabecalho.layoutParams.height

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val sistema = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )

            cabecalho.setPadding(cabecalho.paddingLeft, sistema.top, cabecalho.paddingRight, 0)
            cabecalho.layoutParams = cabecalho.layoutParams.apply {
                height = alturaCabecalho + sistema.top
            }

            barra.setPadding(0, 0, 0, sistema.bottom)
            lista.setPadding(
                lista.paddingLeft,
                alturaCabecalho + sistema.top,
                lista.paddingRight,
                lista.paddingBottom
            )

            insets
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EXIT_AFTER_RATING, sairDepoisDaAvaliacao)
        outState.putBoolean(STATE_RESET_AFTER_CLOSE, resetarDepoisDoFechamento)
        super.onSaveInstanceState(outState)
    }

    private fun enviarOEscrito() {
        val texto = campo.text?.toString()?.trim().orEmpty()

        if (texto.isEmpty()) {
            return
        }

        // Só limpa depois de aceita: recusada por tamanho ou por falta de
        // configuração, o que a pessoa escreveu continua lá para ela corrigir.
        if (enviar(texto)) {
            campo.text?.clear()
        }
    }

    private fun enviar(texto: String): Boolean {
        if (!viewModel.enviar(texto)) {
            return false
        }

        // Os atalhos são de primeira conversa: depois que a pessoa fala, quem
        // conduz é ela.
        sugestoes.visibility = View.GONE

        lista.scrollToPosition(adapter.acrescentar(MensagemChat.DeQuemUsa(texto)))
        return true
    }

    override fun respostaRecebida(texto: String) {
        lista.scrollToPosition(adapter.acrescentar(MensagemChat.DaCerita(texto)))
    }

    override fun progressoDaEspera(texto: String) {
        situacao.text = texto
    }

    /**
     * Encerra a conversa e limpa a tela. O servidor usa isso para resumir o
     * que foi dito, entao vale mesmo quando a pessoa so quer recomecar.
     */
    private fun sairDaTela() {
        sairDepoisDaAvaliacao = true
        resetarDepoisDoFechamento = false
        fecharSecao()
    }

    private fun fecharSecao() {
        if (fechamentoEmCurso) return
        if (viewModel.estaAguardando) {
            makeText(getString(R.string.chat_aguarde_resposta))
            return
        }

        if (!viewModel.temSessao) {
            if (resetarDepoisDoFechamento) limparConversa()
            else finish()
            return
        }

        fechamentoEmCurso = true
        viewModel.encerrarSessao { sessionId, sucesso ->
            fechamentoEmCurso = false
            if (!sucesso) {
                makeText(getString(R.string.chat_falha_encerrar))
                return@encerrarSessao
            }

            if (resetarDepoisDoFechamento) limparConversa()
            if (sessionId != null) {
                AvaliacaoSecaoDialog.nova(sessionId)
                    .show(supportFragmentManager, "avaliacao_secao")
            } else if (sairDepoisDaAvaliacao) {
                finish()
            }
        }
    }

    private fun limparConversa() {
            adapter.trocar(
                listOf(
                    MensagemChat.Divisor(getString(R.string.chat_hoje)),
                    MensagemChat.DaCerita(getString(R.string.chat_abertura))
                )
            )
            sugestoes.visibility = View.VISIBLE
            makeText(getString(R.string.chat_encerrada))
    }

    override fun aguardando(aguardando: Boolean) {
        btnEnviar.isEnabled = !aguardando
        situacao.setText(if (aguardando) R.string.chat_digitando else R.string.chat_situacao)
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val STATE_EXIT_AFTER_RATING = "chat_exit_after_rating"
        const val STATE_RESET_AFTER_CLOSE = "chat_reset_after_close"
    }
}
