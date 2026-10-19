package com.example.ceris.notification

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

class PushSyncJobService : JobService() {
    private var active: AtomicBoolean? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val running = AtomicBoolean(true)
        active = running
        val push = PushNotifications.get(this)
        push.execute {
            val retry = push.synchronize()
            Handler(Looper.getMainLooper()).post { if (running.get()) jobFinished(params, retry) }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        active?.set(false)
        // O trabalho em curso verifica a sessao antes de aplicar seu resultado.
        return PushNotifications.get(this).canSynchronize()
    }
}
