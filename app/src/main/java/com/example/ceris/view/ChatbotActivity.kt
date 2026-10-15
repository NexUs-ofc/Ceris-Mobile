package com.example.ceris.view

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.view.adapter.ChatAdapter
import com.example.ceris.view.adapter.MensagemChat
import com.example.ceris.view.utils.hideNavigationBar

/**
 * Cerita.
 *
 * Ainda sem a IA do outro lado: a tela monta a conversa de abertura do Figma,
 * manda o que a pessoa escreve para a lista e some com os atalhos depois do
 * primeiro envio. A resposta da Cerita entra quando a API existir — o
 * ChatAdapter já aceita linha nova sem mexer em layout.
 *
 * O inset de baixo vai no campo de envio, e não na raiz: com padding na raiz o
 * fundo estampado pararia antes da borda da tela.
 */
class ChatbotActivity : AppCompatActivity() {

    private lateinit var lista: RecyclerView
    private lateinit var campo: EditText
    private lateinit var sugestoes: View

    private val adapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_chatbot)

        lista = findViewById(R.id.lista_mensagens)
        campo = findViewById(R.id.campo_mensagem)
        sugestoes = findViewById(R.id.sugestoes)

        aplicarInsets()

        lista.adapter = adapter
        adapter.trocar(
            listOf(
                MensagemChat.Divisor(getString(R.string.chat_hoje)),
                MensagemChat.DaCerita(getString(R.string.chat_abertura))
            )
        )

        findViewById<View>(R.id.btn_voltar).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btn_enviar).setOnClickListener { enviarOEscrito() }

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

        campo.text?.clear()
        enviar(texto)
    }

    private fun enviar(texto: String) {
        // Os atalhos são de primeira conversa: depois que a pessoa fala, quem
        // conduz é ela.
        sugestoes.visibility = View.GONE

        lista.scrollToPosition(adapter.acrescentar(MensagemChat.DeQuemUsa(texto)))
    }
}
