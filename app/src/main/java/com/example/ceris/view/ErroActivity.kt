package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.view.utils.hideNavigationBar
import com.google.android.material.button.MaterialButton

/**
 * Tela de falha geral.
 *
 * Existe para o caso em que não há tela nenhuma para mostrar: o serviço não
 * respondeu e qualquer coisa que o app desenhasse seria um vão branco. Nos
 * erros que a tela consegue absorver — um item que não salvou, uma lista que
 * não carregou — a mensagem continua dentro da própria tela; aqui é só quando
 * o app inteiro não tem como seguir.
 *
 * Voltar para cá não empilha: a tela sai com `finish()` nos dois botões, e a
 * de origem recomeça do zero.
 */
class ErroActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MENSAGEM = "mensagem"
        private const val EXTRA_DESTINO = "destino"

        /**
         * @param destino classe que o "Tentar novamente" deve reabrir. Em geral
         *  a própria tela que falhou; sem ela o botão volta para a splash, que
         *  refaz a verificação desde o começo.
         */
        fun abrir(contexto: Context, mensagem: String, destino: Class<*>? = null): Intent =
            Intent(contexto, ErroActivity::class.java)
                .putExtra(EXTRA_MENSAGEM, mensagem)
                .putExtra(EXTRA_DESTINO, destino?.name)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_erro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mensagem = intent.getStringExtra(EXTRA_MENSAGEM)

        if (!mensagem.isNullOrBlank()) {
            findViewById<TextView>(R.id.txt_erro_mensagem).text = mensagem
        }

        findViewById<MaterialButton>(R.id.btn_tentar).setOnClickListener {
            startActivity(Intent(this, destinoDoRetorno()))
            finish()
        }

        findViewById<TextView>(R.id.btn_inicio).setOnClickListener {
            startActivity(Intent(this, SplashScreen::class.java))
            finish()
        }
    }

    private fun destinoDoRetorno(): Class<*> {
        val nome = intent.getStringExtra(EXTRA_DESTINO) ?: return SplashScreen::class.java

        return runCatching { Class.forName(nome) }.getOrDefault(SplashScreen::class.java)
    }
}
