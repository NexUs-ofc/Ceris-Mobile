package com.example.ceris.view

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.notification.PushNotifications
import com.example.ceris.view.fragment.EstoqueFragment
import com.example.ceris.view.fragment.ListasFragment
import com.example.ceris.view.fragment.LocaisFragment
import com.example.ceris.view.fragment.NotaFiscalFragment
import com.example.ceris.view.fragment.PerfilFragment
import com.example.ceris.view.fragment.ReceitasFragment
import com.example.ceris.view.utils.hideNavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            PushNotifications.get(this).refresh()
            if (!granted) Toast.makeText(this, R.string.notificacoes_desativadas, Toast.LENGTH_LONG).show()
        }

    private val destinos: Map<Int, () -> Fragment> = mapOf(
        R.id.nav_estoque to { EstoqueFragment() },
        R.id.nav_locais to { LocaisFragment() },
        R.id.nav_lista to { ListasFragment() },
        R.id.nav_receitas to { ReceitasFragment() }
    )

    private lateinit var barra: BottomNavigationView

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

        barra = findViewById(R.id.nav_destinos)

        // O botao central e a leitura de nota: o primeiro toque abre a camera
        // aqui mesmo, entre as abas, e com ela aberta o toque tira a foto.
        findViewById<FloatingActionButton>(R.id.nav_scan).setOnClickListener {
            val nota = supportFragmentManager.findFragmentByTag(NOTA.toString()) as? NotaFiscalFragment

            if (nota != null && nota.isVisible) {
                nota.capturar()
            } else {
                mostrar(NOTA) { NotaFiscalFragment() }
            }
        }

        barra.setOnItemReselectedListener { }
        barra.setOnItemSelectedListener { item ->
            val destino = destinos[item.itemId] ?: return@setOnItemSelectedListener false
            mostrar(item.itemId, destino)
            true
        }

        // Só na primeira criação: em recriação o FragmentManager já restaurou tudo.
        //
        // A exibição é chamada direto, e não por selectedItemId: a barra já marca
        // o primeiro item do menu ao inflar, entao atribuir esse mesmo id nao
        // muda nada e o listener nunca dispara - o container ficaria vazio.
        if (savedInstanceState == null) {
            barra.selectedItemId = R.id.nav_estoque
            mostrar(R.id.nav_estoque) { EstoqueFragment() }
        }
        requestNotifications(savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        // O toque num aviso pode acontecer depois de a sessao ter sido encerrada.
        if (SessionManager(this).snapshot() == null) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
            return
        }
        PushNotifications.get(this).refresh()
    }

    private fun requestNotifications(firstCreation: Boolean) {
        if (Build.VERSION.SDK_INT < 33 || !firstCreation || SessionManager(this).snapshot() == null) return
        val push = PushNotifications.get(this)
        if (push.state.permissionRequested || NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        // Uma explicacao por instalacao; recusar nao impede usar o aplicativo.
        push.state.requestPermission()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notificacoes_permissao_titulo)
            .setMessage(R.string.notificacoes_permissao_mensagem)
            .setPositiveButton(R.string.notificacoes_ativar) { _, _ ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.notificacoes_agora_nao) { _, _ -> push.refresh() }
            .show()
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

    /** Depois de gravar a nota, a pessoa volta para ver o estoque atualizado. */
    fun voltarAoEstoque() {
        if (barra.selectedItemId == R.id.nav_estoque) {
            mostrar(R.id.nav_estoque) { EstoqueFragment() }
        } else {
            barra.selectedItemId = R.id.nav_estoque
        }
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

        /** A leitura de nota tambem fica fora do menu, presa ao botao central. */
        const val NOTA = -2
    }
}
