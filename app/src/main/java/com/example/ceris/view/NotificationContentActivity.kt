package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.NotificationSourceAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.notification.NotificationLink
import com.example.ceris.repository.NotificationContent
import com.example.ceris.repository.NotificationSourceRepository
import com.example.ceris.repository.NotificationSourceResult
import com.example.ceris.view.utils.hideNavigationBar
import retrofit2.Call
import java.text.SimpleDateFormat
import java.util.Locale

/** Destinos do push usam o Core, que valida o acesso ao registro pelo JWT. */
abstract class NotificationContentActivity : AppCompatActivity() {
    protected abstract val destination: NotificationLink.Destination
    private lateinit var link: NotificationLink
    private lateinit var sessions: SessionManager
    private lateinit var openedSession: SessionSnapshot
    private var call: Call<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_notification_content)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        sessions = SessionManager(this)
        val parsed = NotificationLink.parse(intent.data?.toString())
        if (parsed == null || parsed.destination != destination) {
            Toast.makeText(this, R.string.notificacao_link_invalido, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        link = parsed
        findViewById<Button>(R.id.notification_back).apply {
            setText(
                when (destination) {
                    NotificationLink.Destination.PRODUCT -> R.string.notificacao_voltar_estoque
                    NotificationLink.Destination.STORE -> R.string.notificacao_voltar_locais
                    NotificationLink.Destination.EVENT -> R.string.notificacao_voltar_inicio
                },
            )
            setOnClickListener { finish() }
        }
        findViewById<Button>(R.id.notification_retry).setOnClickListener { load() }
    }

    override fun onResume() {
        super.onResume()
        if (!::link.isInitialized) return
        val current = sessions.snapshot()
        when {
            current == null -> authenticate()
            !link.belongsTo(current.profileId) -> wrongAccount()
            else -> {
                openedSession = current
                load()
            }
        }
    }

    override fun onStop() {
        call?.cancel()
        findViewById<TextView>(R.id.notification_title).text = ""
        findViewById<TextView>(R.id.notification_details).text = ""
        super.onStop()
    }

    private fun load() {
        call?.cancel()
        if (!sessions.isCurrent(openedSession)) {
            if (sessions.snapshot() == null) authenticate() else wrongAccount()
            return
        }
        findViewById<View>(R.id.notification_progress).visibility = View.VISIBLE
        findViewById<View>(R.id.notification_retry).visibility = View.GONE
        findViewById<TextView>(R.id.notification_title).setText(R.string.notificacao_carregando)
        findViewById<TextView>(R.id.notification_details).text = ""
        if (!ApiBaseUrl.configurada(ApiBaseUrl.core)) {
            showResult(NotificationSourceResult.Failure)
            return
        }
        val api = RetrofitClient.getApiAutenticada(ApiBaseUrl.core, NotificationSourceAPI::class.java)
        call = NotificationSourceRepository(api, sessions).load(link, ::showResult)
    }

    private fun showResult(result: NotificationSourceResult) {
        if (isFinishing || isDestroyed) return
        findViewById<View>(R.id.notification_progress).visibility = View.GONE
        if (!sessions.isCurrent(openedSession)) {
            if (sessions.snapshot() == null) authenticate() else wrongAccount()
            return
        }
        when (result) {
            is NotificationSourceResult.Loaded -> showContent(result.content)
            NotificationSourceResult.Unavailable -> {
                findViewById<TextView>(R.id.notification_title).setText(R.string.notificacao_indisponivel_titulo)
                findViewById<TextView>(R.id.notification_details).setText(R.string.notificacao_indisponivel_mensagem)
            }
            NotificationSourceResult.AuthenticationRequired -> {
                // O cliente ja tentou renovar antes de entregar o 401 definitivo.
                sessions.snapshot()?.takeIf { it.generation == openedSession.generation }?.let { sessions.discard(it) }
                authenticate()
            }
            NotificationSourceResult.WrongAccount, NotificationSourceResult.SessionChanged -> wrongAccount()
            NotificationSourceResult.Failure -> {
                findViewById<TextView>(R.id.notification_title).setText(R.string.notificacao_falha_titulo)
                findViewById<TextView>(R.id.notification_details).setText(R.string.notificacao_falha_mensagem)
                findViewById<View>(R.id.notification_retry).visibility = View.VISIBLE
            }
        }
    }

    private fun showContent(content: NotificationContent) {
        val (title, details) =
            when (content) {
                is NotificationContent.Product ->
                    with(content.value) {
                        (foodName ?: getString(R.string.notificacao_produto)) to
                            listOfNotNull(
                                productBrand?.takeIf { it.isNotBlank() },
                                quantity?.let { getString(R.string.notificacao_quantidade, it, unitOfMeasure.orEmpty()) },
                                expiryDate?.let { getString(R.string.notificacao_validade, formatDate(it)) },
                            ).joinToString("\n\n")
                    }
                is NotificationContent.Store ->
                    with(content.value) {
                        (name ?: getString(R.string.notificacao_loja)) to
                            listOfNotNull(
                                listOfNotNull(street, number, neighborhood, city, state).filter { it.isNotBlank() }.joinToString(", "),
                                if (openingTime != null && closingTime != null) {
                                    getString(R.string.notificacao_horario, openingTime, closingTime)
                                } else {
                                    null
                                },
                            ).filter { it.isNotBlank() }.joinToString("\n\n")
                    }
                is NotificationContent.Event ->
                    with(content.value) {
                        (title ?: getString(R.string.notificacao_evento)) to
                            listOfNotNull(
                                description?.takeIf { it.isNotBlank() },
                                date?.let { getString(R.string.notificacao_data_evento, formatDate(it)) },
                                location?.takeIf { it.isNotBlank() },
                                duration?.let { getString(R.string.notificacao_duracao, it) },
                                peopleCount?.let { getString(R.string.notificacao_pessoas, it) },
                            ).joinToString("\n\n")
                    }
            }
        findViewById<TextView>(R.id.notification_title).text = title
        findViewById<TextView>(R.id.notification_details).text = details
    }

    private fun authenticate() {
        // A Main guarda o destino antes do login e o valida de novo depois dele.
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                data = link.toUri().toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }

    private fun wrongAccount() {
        Toast.makeText(this, R.string.notificacao_outra_conta, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun formatDate(value: String): String =
        runCatching {
            val hasTime = value.contains('T')
            val input = SimpleDateFormat(if (hasTime) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
            val output = SimpleDateFormat(if (hasTime) "dd/MM/yyyy 'às' HH:mm" else "dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
            output.format(requireNotNull(input.parse(value)))
        }.getOrDefault(value)
}
