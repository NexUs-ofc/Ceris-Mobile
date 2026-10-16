package com.example.ceris.view

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

        lista.adapter = adapter
        adapter.trocar(
            listOf(
                MensagemChat.Divisor(getString(R.string.chat_hoje)),
                MensagemChat.DaCerita(getString(R.string.chat_abertura))
            )
        )

        findViewById<View>(R.id.btn_voltar).setOnClickListener { finish() }

        btnEnviar.setOnClickListener { enviarOEscrito() }

        findViewById<ImageButton>(R.id.btn_encerrar).setOnClickListener { encerrar() }

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
    private fun encerrar() {
        if (!viewModel.temSessao) {
            return
        }

        viewModel.encerrarSessao {
            adapter.trocar(
                listOf(
                    MensagemChat.Divisor(getString(R.string.chat_hoje)),
                    MensagemChat.DaCerita(getString(R.string.chat_abertura))
                )
            )
            sugestoes.visibility = View.VISIBLE
            makeText(getString(R.string.chat_encerrada))
        }
    }

    override fun aguardando(aguardando: Boolean) {
        btnEnviar.isEnabled = !aguardando
        situacao.setText(if (aguardando) R.string.chat_digitando else R.string.chat_situacao)
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
