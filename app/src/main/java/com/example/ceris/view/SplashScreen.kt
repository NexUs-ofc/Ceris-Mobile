package com.example.ceris.view

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.ServicoNoAr
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.hideNavigationBar

class SplashScreen : AppCompatActivity() {

    private companion object {
        /** Quanto a tela fica no ar. */
        const val DURACAO_MS = 4000L
    }

    /** Frase da falha do ping, ou null quando o servico respondeu. */
    private var falhaDoServico: String? = null

    private var servicoConferido = false

    /** O que fazer quando a resposta do ping chegar, se ela demorar mais que a animacao. */
    private var aoConferir: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        aplicarHuninn()

        val sessionManager = SessionManager(this)

        // Aqui porque e a primeira tela do app e o RenovadorDeSessao precisa de
        // um Context para ler os tokens guardados.
        RetrofitClient.configurarRenovacao(sessionManager)

        conferirServico(sessionManager)

        // A contagem comeca no primeiro quadro desenhado, nao no onCreate.
        //
        // Em partida fria o onCreate roda muito antes de a janela aparecer:
        // medido no aparelho, quase 9s ate a tela seguinte. Com o relogio
        // comecando no onCreate os 4 segundos acabavam antes de a splash chegar
        // a ser desenhada, e o que se via era branco.
        findViewById<View>(R.id.main).post {
            animateSplash()

            Handler(Looper.getMainLooper()).postDelayed({
                sair(sessionManager)
            }, DURACAO_MS)
        }
    }

    /**
     * A marca e a legenda em Huninn, aplicadas no codigo.
     *
     * O `android:fontFamily="@font/huninn"` esta no XML e mesmo assim os dois
     * TextViews subiam em Montserrat — conferido na tela do aparelho, letra por
     * letra, contra as duas fontes. O estilo TituloTela, que declara o
     * `fontFamily` nos dois namespaces, pega normalmente nas outras telas; o
     * atributo solto nao estava pegando nestes. Resolvendo o Typeface aqui a
     * tela nao depende de como o AppCompat le o atributo.
     */
    private fun aplicarHuninn() {
        val huninn: Typeface = ResourcesCompat.getFont(this, R.font.huninn) ?: return

        findViewById<TextView>(R.id.title).typeface = huninn
        findViewById<TextView>(R.id.legends).typeface = huninn
    }

    private fun animateSplash() {
        val background = findViewById<ImageView>(R.id.imageView)
        val logo = findViewById<ImageView>(R.id.logo)
        val title = findViewById<TextView>(R.id.title)

        background.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(1800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logo.animate()
            .alpha(1f)
            .setStartDelay(1000)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate()
            .alpha(1f)
            .setStartDelay(1200)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Pergunta ao serviço que a proxima tela vai usar se ele esta no ar.
     *
     * Roda junto com a animacao, nao depois: quando os 4 segundos acabam a
     * resposta ja costuma estar aqui e a troca de tela nao atrasa nada. Se
     * ainda nao chegou, a splash espera — e melhor segurar mais um instante do
     * que abrir uma tela que vai falhar.
     *
     * Qual servico depende de para onde a pessoa vai: sem sessao o proximo
     * passo e o login, que fala com a Auth; com sessao e o app, que fala com a
     * Core.
     */
    private fun conferirServico(sessionManager: SessionManager) {
        val aoResponder: (String?) -> Unit = { falha ->
            falhaDoServico = falha
            servicoConferido = true

            aoConferir?.let {
                aoConferir = null
                it()
            }
        }

        if (temSessao(sessionManager)) {
            ServicoNoAr.verificarCore(aoResponder)
        } else {
            ServicoNoAr.verificarAuth(aoResponder)
        }
    }

    private fun sair(sessionManager: SessionManager) {
        if (isFinishing || isDestroyed) {
            return
        }

        if (!servicoConferido) {
            aoConferir = { sair(sessionManager) }
            return
        }

        val falha = falhaDoServico

        if (falha != null) {
            // Sem servico nao ha o que desenhar na proxima tela. O
            // "Tentar novamente" volta para ca e refaz a verificacao.
            startActivity(ErroActivity.abrir(this, falha, SplashScreen::class.java))
            finish()
            return
        }

        startActivity(Intent(this, destinoInicial(sessionManager)))
        finish()
    }

    /**
     * Quem ja entrou volta direto para o app.
     *
     * Antes a splash mandava sempre para o login, mesmo com a sessao guardada:
     * abrir o app parecia sessao expirada, sem nada ter expirado.
     *
     * A decisao olha o refresh token, nao o access: o access vale 15 minutos e
     * quase sempre esta velho, enquanto o refresh vale 30 dias e e o que diz se
     * ainda da para seguir. Se ele tambem tiver morrido, a primeira chamada
     * toma 401, o RenovadorDeSessao descarta os dois e a proxima abertura cai
     * no login.
     */
    private fun destinoInicial(sessionManager: SessionManager): Class<*> =
        if (temSessao(sessionManager)) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }

    private fun temSessao(sessionManager: SessionManager): Boolean =
        sessionManager.getString(SessionKeys.REFRESH_TOKEN, "").isNotBlank()
}
