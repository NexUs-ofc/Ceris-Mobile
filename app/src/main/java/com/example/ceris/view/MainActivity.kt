package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.ceris.R
import com.example.ceris.view.fragment.EstoqueFragment
import com.example.ceris.view.fragment.ListasFragment
import com.example.ceris.view.fragment.LocaisFragment
import com.example.ceris.view.fragment.PerfilFragment
import com.example.ceris.view.fragment.ReceitasFragment
import com.example.ceris.view.utils.hideNavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Casca das telas com barra de navegação.
 *
 * A barra pertence a esta Activity, e não aos fragments, então ela não é
 * recriada a cada troca: o item ativo nunca fica fora de sincronia e não há
 * animação interrompida no meio.
 *
 * Os destinos são mantidos vivos e alternados por show/hide em vez de
 * substituídos. Assim a posição de rolagem, o filtro escolhido e o texto
 * digitado sobrevivem à ida e volta entre abas - com replace, cada retorno
 * recriaria a tela do zero.
 */
class MainActivity : AppCompatActivity() {

    private val destinos: Map<Int, () -> Fragment> = mapOf(
        R.id.nav_estoque to { EstoqueFragment() },
        R.id.nav_locais to { LocaisFragment() },
        R.id.nav_lista to { ListasFragment() },
        R.id.nav_receitas to { ReceitasFragment() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val barra = findViewById<BottomNavigationView>(R.id.nav_destinos)

        findViewById<FloatingActionButton>(R.id.nav_scan).setOnClickListener {
            startActivity(Intent(this, QrCodeActivity::class.java))
        }

        barra.setOnItemReselectedListener { }
        barra.setOnItemSelectedListener { item ->
            val destino = destinos[item.itemId] ?: return@setOnItemSelectedListener false
            mostrar(item.itemId, destino)
            true
        }

        // Só na primeira criação: em recriação o FragmentManager já restaurou tudo.
        if (savedInstanceState == null) {
            barra.selectedItemId = R.id.nav_estoque
        }
    }

    /**
     * Abre o perfil, que exibe a barra sem ser um dos destinos dela.
     *
     * Fica aqui porque quem toca no ícone de perfil é um fragment, e ele não
     * deve conhecer o FragmentManager da Activity.
     */
    fun abrirPerfil() {
        mostrar(PERFIL) { PerfilFragment() }
    }

    /**
     * Traz o destino à frente, criando-o na primeira vez e apenas reexibindo-o
     * nas seguintes.
     */
    private fun mostrar(chave: Int, criar: () -> Fragment) {
        val tag = chave.toString()
        val gerenciador = supportFragmentManager
        val atual = gerenciador.fragments.firstOrNull { it.isVisible }

        if (atual?.tag == tag) return

        gerenciador.beginTransaction().apply {
            setReorderingAllowed(true)

            atual?.let { hide(it) }

            val alvo = gerenciador.findFragmentByTag(tag)
            if (alvo == null) {
                add(R.id.container, criar(), tag)
            } else {
                show(alvo)
            }

            commit()
        }
    }

    private companion object {
        /** O perfil não está em nav_menu.xml, então usa uma chave própria. */
        const val PERFIL = -1
    }
}
