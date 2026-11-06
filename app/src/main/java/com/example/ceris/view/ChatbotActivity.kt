package com.example.ceris.view

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.local.ChatHistoryStore
import com.example.ceris.view.adapter.ChatAdapter
import com.example.ceris.view.adapter.MensagemChat
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.ChatViewModel

class ChatbotActivity : AppCompatActivity(), ChatViewModel.Listener {

    private lateinit var lista: RecyclerView
    private lateinit var campo: EditText
    private lateinit var sugestoes: View
    private lateinit var btnEnviar: ImageButton
    private lateinit var situacao: TextView

    private val adapter = ChatAdapter()
    private lateinit var historico: ChatHistoryStore

    private val viewModel: ChatViewModel by viewModels()
    private var sairDepoisDaAvaliacao = false
    private var resetarDepoisDoFechamento = false
    private var fechamentoEmCurso = false
    private var saidaSolicitada = false

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
        historico = ChatHistoryStore(this)
        saidaSolicitada = savedInstanceState?.getBoolean(STATE_EXIT_REQUESTED) ?: false
        if (saidaSolicitada) viewModel.bloquearEnvios()
        sairDepoisDaAvaliacao = savedInstanceState?.getBoolean(STATE_EXIT_AFTER_RATING) ?: false
        resetarDepoisDoFechamento = savedInstanceState?.getBoolean(STATE_RESET_AFTER_CLOSE) ?: false

        supportFragmentManager.setFragmentResultListener(
            AvaliacaoSecaoDialog.RESULT_KEY, this
        ) { _, resultado ->
            val id = resultado.getString(AvaliacaoSecaoDialog.KEY_SESSION)
            val salvar = resultado.getBoolean(AvaliacaoSecaoDialog.KEY_SAVE)
            if (salvar) {
                val nota = resultado.getInt(AvaliacaoSecaoDialog.KEY_RATING)
                val feedback = resultado.getString(AvaliacaoSecaoDialog.KEY_COMMENT)
                viewModel.avaliarSessao(id, nota, feedback) {
                    if (sairDepoisDaAvaliacao) finish()
                }
                if (saidaSolicitada) finish()
            } else if (saidaSolicitada) {
                viewModel.encerrarSessao { _, _ -> }
                finish()
            } else if (sairDepoisDaAvaliacao) {
                finish()
            }
        }

        lista.adapter = adapter
        mostrarConversaSalva()

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
        if (saidaSolicitada) desativarEnvio()
    }

    /** Aplica os insets das barras do sistema ao cabeçalho e ao campo de envio. */
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
        outState.putBoolean(STATE_EXIT_REQUESTED, saidaSolicitada)
        super.onSaveInstanceState(outState)
    }

    private fun enviarOEscrito() {
        val texto = campo.text?.toString()?.trim().orEmpty()

        if (texto.isEmpty()) {
            return
        }

        // Preserve o texto se o envio falhar.
        if (enviar(texto)) {
            campo.text?.clear()
        }
    }

    private fun enviar(texto: String): Boolean {
        if (saidaSolicitada) return false
        if (!viewModel.enviar(texto)) {
            return false
        }

        // Os atalhos iniciais desaparecem após o primeiro envio.
        sugestoes.visibility = View.GONE

        lista.scrollToPosition(adapter.acrescentar(MensagemChat.DeQuemUsa(texto)))
        salvarHistorico()
        return true
    }

    override fun respostaRecebida(texto: String, respostasRecomendadas: List<String>?) {
        if (saidaSolicitada || isFinishing) return
        lista.scrollToPosition(adapter.acrescentar(MensagemChat.DaCerita(texto)))
        salvarHistorico()
        mostrarSugestoes(respostasRecomendadas.orEmpty())
    }

    private fun mostrarSugestoes(respostas: List<String>) {
        val container = sugestoes as? ViewGroup ?: return
        container.removeAllViews()
        respostas.asSequence().map(String::trim).filter(String::isNotEmpty).take(3).forEach { resposta ->
            val chip = layoutInflater.inflate(R.layout.item_chat_sugestao, container, false) as TextView
            chip.apply {
                text = resposta
                maxWidth = resources.displayMetrics.widthPixels -
                    (resources.getDimensionPixelSize(R.dimen.screen_margin_horizontal) * 2)
                ellipsize = android.text.TextUtils.TruncateAt.END
                setOnClickListener {
                    enviar(resposta)
                }
            }
            container.addView(chip)
        }
        sugestoes.visibility = if (container.childCount > 0) View.VISIBLE else View.GONE
    }

    override fun progressoDaEspera(texto: String) {
        situacao.text = texto
    }

    /** Abre a avaliação de imediato; API e encerramento não bloqueiam a saída. */
    private fun sairDaTela() {
        if (saidaSolicitada) return
        saidaSolicitada = true
        resetarDepoisDoFechamento = false
        desativarEnvio()
        val estavaAguardando = viewModel.estaAguardando
        viewModel.bloquearEnvios()
        salvarHistorico()
        val sessionId = viewModel.sessionIdAtual
        if (sessionId != null || estavaAguardando) {
            AvaliacaoSecaoDialog.nova(sessionId)
                .show(supportFragmentManager, "avaliacao_saida")
        } else {
            finish()
        }
    }

    private fun desativarEnvio() {
        campo.isEnabled = false
        btnEnviar.isEnabled = false
        findViewById<View>(R.id.btn_voltar).isEnabled = false
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
        mostrarConversaSalva()
        sugestoes.visibility = View.VISIBLE
        makeText(getString(R.string.chat_encerrada))
    }

    private fun salvarHistorico() {
        historico.save(adapter.mensagens().mapNotNull { mensagem ->
            when (mensagem) {
                is MensagemChat.DeQuemUsa -> ChatHistoryStore.ChatHistoryMessage("user", mensagem.texto)
                is MensagemChat.DaCerita -> ChatHistoryStore.ChatHistoryMessage("assistant", mensagem.texto)
                is MensagemChat.Divisor -> null
            }
        })
    }

    private fun mostrarConversaSalva() {
        val salvas = historico.load()
        val linhas = buildList {
            if (salvas.isEmpty()) {
                add(MensagemChat.Divisor(getString(R.string.chat_hoje)))
                add(MensagemChat.DaCerita(getString(R.string.chat_abertura)))
            } else {
                add(MensagemChat.Divisor(getString(R.string.chat_historico_anterior)))
                salvas.forEach { mensagem ->
                    add(if (mensagem.type == "user") MensagemChat.DeQuemUsa(mensagem.text)
                    else MensagemChat.DaCerita(mensagem.text))
                }
                add(MensagemChat.Divisor(getString(R.string.chat_nova_conversa)))
            }
        }
        adapter.trocar(linhas)
        lista.scrollToPosition(adapter.itemCount - 1)
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
        const val STATE_EXIT_REQUESTED = "chat_exit_requested"
    }
}
