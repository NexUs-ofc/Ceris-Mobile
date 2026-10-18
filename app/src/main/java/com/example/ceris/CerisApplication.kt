package com.example.ceris

import android.app.Application
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.notification.NotificationChannels

class CerisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Services e Workers tambem iniciam o processo sem abrir a splash.
        RetrofitClient.configurarRenovacao(SessionManager(this))
        NotificationChannels.criar(this)
    }
}
