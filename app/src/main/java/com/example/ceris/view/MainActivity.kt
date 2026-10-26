package com.example.ceris.view

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.ceris.location.DeviceLocationSource
import com.example.ceris.viewmodel.LocaisViewModel
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.notification.NotificationLink
import com.example.ceris.notification.PendingNotificationLink
import com.example.ceris.notification.PushNotifications
import com.example.ceris.view.fragment.EstoqueFragment
import com.example.ceris.view.fragment.ListasFragment
import com.example.ceris.view.fragment.LocaisFragment
import com.example.ceris.view.fragment.NotaFiscalFragment
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
    private val locaisViewModel: LocaisViewModel by viewModels()
    private var notificationLinkConsumed = false
    private var perfilAberto = false
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            PushNotifications.get(this).refresh()
        }

    private val destinos: Map<Int, () -> Fragment> =
        mapOf(
            R.id.nav_estoque to { EstoqueFragment() },
            R.id.nav_locais to { LocaisFragment() },
            R.id.nav_lista to { ListasFragment() },
            R.id.nav_receitas to { ReceitasFragment() },
        )

    private lateinit var barra: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationLinkConsumed = savedInstanceState?.getBoolean(LINK_CONSUMED) == true
        if (notificationLinkConsumed) intent.data = null
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_main)
        perfilAberto = supportFragmentManager.fragments.any { it.tag == PERFIL.toString() && it.isVisible }

        val areaNavegacao = findViewById<View>(R.id.navegacao)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            // Teclado aberto: a barra some em vez de subir junto e esmagar a tela.
            areaNavegacao.visibility =
                if (perfilAberto || insets.isVisible(WindowInsetsCompat.Type.ime())) View.GONE else View.VISIBLE
            insets
        }

        locaisViewModel.init(SessionManager(this), DeviceLocationSource(applicationContext))
        if (listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            locaisViewModel.preload()
        }

        barra = findViewById(R.id.nav_destinos)

        // A BottomNavigationView soma sozinha o recuo da barra do sistema ao
        // proprio padding. Como a raiz ja recuou, a soma empurrava icones e
        // rotulos para fora da faixa de altura fixa: em aparelho com botoes de
        // navegacao (e no tablet, com a barra de tarefas) os destinos sumiam ou
        // estouravam. Consumindo aqui, ela nao recua de novo.
        ViewCompat.setOnApplyWindowInsetsListener(barra) { _, _ -> WindowInsetsCompat.CONSUMED }

        // O botao central e a leitura de nota: o primeiro toque abre a camera
        // aqui mesmo, entre as abas, e com ela aberta o toque tira a foto.
        findViewById<FloatingActionButton>(R.id.nav_scan).setOnClickListener {
            val nota = supportFragmentManager.findFragmentByTag(NOTA.toString()) as? NotaFiscalFragment

            if (nota != null && nota.isVisible) {
                nota.capturar()
            } else {
                limparSelecaoDaNavegacao()
                mostrar(NOTA) { NotaFiscalFragment() }
            }
        }

        barra.setOnItemReselectedListener { }
        barra.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_vao_scan) return@setOnItemSelectedListener true
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
        } else if (supportFragmentManager.fragments.any { it.tag == NOTA.toString() && it.isVisible }) {
            limparSelecaoDaNavegacao()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        val pending = PendingNotificationLink(this)
        val rawLink = intent.data?.toString()
        val incoming = NotificationLink.parse(rawLink)
        // Consumir antes de navegar evita reabrir o detalhe ao voltar ou girar a tela.
        intent.data = null
        if (rawLink != null) notificationLinkConsumed = true
        val session = SessionManager(this).snapshot()
        // O toque num aviso pode acontecer depois de a sessao ter sido encerrada.
        if (session == null) {
            if (rawLink != null) {
                pending.clear()
                if (incoming != null) pending.save(incoming)
            }
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
            return
        }
        PushNotifications.get(this).refresh()
        val link =
            if (rawLink != null) {
                pending.clear()
                incoming
            } else {
                pending.take()
            }
        if (rawLink != null && incoming == null) {
            Toast.makeText(this, R.string.notificacao_link_invalido, Toast.LENGTH_LONG).show()
        }
        if (link != null) {
            if (!link.belongsTo(session.profileId)) {
                Toast.makeText(this, R.string.notificacao_outra_conta, Toast.LENGTH_LONG).show()
            } else {
                abrirNotificacao(link)
                return
            }
        }
        requestNotifications()
    }

    override fun onStop() {
        locaisViewModel.onNotVisible()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationLinkConsumed = false
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(LINK_CONSUMED, notificationLinkConsumed)
        super.onSaveInstanceState(outState)
    }

    private fun abrirNotificacao(link: NotificationLink) {
        val isStore = link.destination == NotificationLink.Destination.STORE
        val tab = if (isStore) R.id.nav_locais else R.id.nav_estoque
        val barra = findViewById<BottomNavigationView>(R.id.nav_destinos)
        if (barra.selectedItemId == tab) {
            // O perfil pode estar visivel mesmo com a aba de estoque selecionada.
            mostrar(tab, destinos.getValue(tab))
        } else {
            barra.selectedItemId = tab
        }
        val activity =
            when (link.destination) {
                // O detalhe do produto sera integrado quando a tela correta estiver disponivel.
                NotificationLink.Destination.PRODUCT -> return
                NotificationLink.Destination.STORE -> DetalheLocalActivity::class.java
                NotificationLink.Destination.EVENT -> DetalheEventoActivity::class.java
            }
        startActivity(Intent(this, activity).setData(link.toUri().toUri()))
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT < 33 || SessionManager(this).snapshot() == null) return
        val push = PushNotifications.get(this)
        if (push.state.permissionRequested || NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        push.state.requestPermission()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Abre o perfil em tela inteira, como no desenho.
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
    private fun mostrar(
        chave: Int,
        criar: () -> Fragment,
    ) {
        val tag = chave.toString()
        val gerenciador = supportFragmentManager
        val atual = gerenciador.fragments.firstOrNull { it.isVisible }

        if (atual?.tag == tag) return

        perfilAberto = chave == PERFIL
        findViewById<View>(R.id.navegacao).visibility = if (perfilAberto) View.GONE else View.VISIBLE

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

    /**
     * Scan vive no botão central, fora do menu. Sem limpar o item marcado, tocar
     * na aba anterior é tratado como reseleção e não navega de volta.
     */
    private fun limparSelecaoDaNavegacao() {
        val vao = barra.menu.findItem(R.id.nav_vao_scan)
        // O vão não desenha ícone nem rótulo. Selecioná-lo muda o ID interno
        // da BottomNavigationView, para a aba anterior deixar de ser reseleção.
        vao.isEnabled = true
        barra.selectedItemId = R.id.nav_vao_scan
        vao.isEnabled = false
    }

    private companion object {
        const val LINK_CONSUMED = "notification_link_consumed"

        /** O perfil não está em nav_menu.xml, então usa uma chave própria. */
        const val PERFIL = -1

        /** A leitura de nota tambem fica fora do menu, presa ao botao central. */
        const val NOTA = -2
    }
}
