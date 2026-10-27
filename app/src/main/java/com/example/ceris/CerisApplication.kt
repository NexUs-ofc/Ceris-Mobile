package com.example.ceris

import android.app.Application
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.notification.NotificationChannels
import com.example.ceris.notification.PushNotifications
import com.example.ceris.view.RedirecionadorDeSessao

class CerisApplication : Application() {
    val pushNotifications: PushNotifications by lazy { PushNotifications(this) }
    private val redirecionadorDeSessao by lazy { RedirecionadorDeSessao(this) }

    override fun onCreate() {
        super.onCreate()

        // Services e Workers tambem iniciam o processo sem abrir a splash.
        redirecionadorDeSessao.iniciar()
        RetrofitClient.configurarRenovacao(
            sessionManager = SessionManager(this),
            aoExpirar = redirecionadorDeSessao::aoExpirar,
        )
        NotificationChannels.criar(this)
        pushNotifications.start()
    }
}
