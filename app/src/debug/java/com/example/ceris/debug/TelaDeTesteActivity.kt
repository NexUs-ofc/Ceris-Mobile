package com.example.ceris.debug

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.ceris.view.fragment.EstoqueFragment
import com.example.ceris.view.fragment.NotaFiscalFragment

/**
 * So no build de debug: hospeda uma tela do app sem exigir sessao, para o
 * TelasTest tirar print dela no aparelho. A MainActivity manda para o login
 * quando nao ha sessao, e o teste nao deve usar conta real nem token falso.
 */
class TelaDeTesteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this).apply { id = CONTAINER }
        setContentView(container)

        if (savedInstanceState == null) {
            val tela = when (intent.getStringExtra(EXTRA_TELA)) {
                TELA_NOTA -> NotaFiscalFragment()
                else -> EstoqueFragment()
            }
            supportFragmentManager.beginTransaction().add(CONTAINER, tela, intent.getStringExtra(EXTRA_TELA)).commitNow()
        }
    }

    companion object {
        const val EXTRA_TELA = "tela"
        const val TELA_ESTOQUE = "estoque"
        const val TELA_NOTA = "nota"
        private const val CONTAINER = 0x0CE215
    }
}
