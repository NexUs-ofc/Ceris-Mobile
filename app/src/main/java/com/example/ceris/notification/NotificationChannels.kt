package com.example.ceris.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.ceris.R

object NotificationChannels {
    const val PRODUTOS = "ceris_produtos"
    const val EVENTOS = "ceris_eventos"
    const val LOJAS = "ceris_lojas"

    fun criar(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(PRODUTOS, context.getString(R.string.canal_produtos), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(EVENTOS, context.getString(R.string.canal_eventos), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(LOJAS, context.getString(R.string.canal_lojas), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }
}
