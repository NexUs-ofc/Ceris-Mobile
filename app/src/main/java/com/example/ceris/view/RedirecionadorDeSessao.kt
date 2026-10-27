package com.example.ceris.view

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.ceris.local.SessionManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Leva a pessoa ao login quando nem o refresh token consegue recuperar a
 * sessao. Nao mostra Toast: as requisicoes que estavam em voo podem falhar ao
 * mesmo tempo, mas a transicao de tela deve acontecer uma unica vez.
 *
 * A Activity em primeiro plano e mantida por referencia fraca. Se a falha
 * acontecer com o app em segundo plano, a navegacao fica pendente e ocorre no
 * proximo resume, evitando tentar abrir tela a partir de um Worker.
 */
class RedirecionadorDeSessao(
    private val application: Application,
) : Application.ActivityLifecycleCallbacks {
    private val principal = Handler(Looper.getMainLooper())
    private val pendente = AtomicBoolean(false)

    @Volatile
    private var atividadeAtual = WeakReference<Activity>(null)

    fun iniciar() {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun aoExpirar() {
        if (!pendente.compareAndSet(false, true)) return
        principal.post { redirecionar(atividadeAtual.get()) }
    }

    override fun onActivityResumed(activity: Activity) {
        atividadeAtual = WeakReference(activity)

        // Ao chegar ao login a expiracao ja foi tratada. Uma sessao nova
        // tambem libera o coordenador para uma expiracao futura.
        val estaNaEntrada = activity is LoginActivity || activity is SplashScreen
        if (estaNaEntrada || SessionManager(application).snapshot() != null) {
            pendente.set(false)
            return
        }

        if (pendente.get()) redirecionar(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (atividadeAtual.get() === activity) atividadeAtual.clear()
    }

    private fun redirecionar(activity: Activity?) {
        if (!pendente.get() || activity == null || activity.isFinishing || activity.isDestroyed) return
        if (activity is LoginActivity || activity is SplashScreen) {
            pendente.set(false)
            return
        }

        if (pendente.compareAndSet(true, false)) {
            activity.startActivity(
                Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
