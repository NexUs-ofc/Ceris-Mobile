package com.example.ceris.notification

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ceris.CerisApplication
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.NotifyAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionKeys
import com.example.ceris.local.SessionManager
import com.example.ceris.repository.PushInstallationRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PushNotifications(private val context: Context) {
    private val sessionManager = SessionManager(context)
    val state = PushState(context)
    private val publisher = PushNotificationPublisher(context, state)
    private val messages = PushMessageHandler(sessionManager, state, publisher::permitted, publisher::publish)
    private val scheduler = context.getSystemService(JobScheduler::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val sync =
        PushSynchronizer(
            PushInstallationRepository(
                RetrofitClient.getApiAutenticada(ApiBaseUrl.notify, NotifyAPI::class.java),
                sessionManager,
                revocationApi = RetrofitClient.getApi(ApiBaseUrl.notify, NotifyAPI::class.java),
            ),
            sessionManager,
            state,
            publisher::permitted,
            ::report,
        )
    private var lastSession = sessionManager.snapshot()
    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Nunca adquire outro lock dentro do listener do SessionManager.
            if (key == SessionKeys.SESSION_GENERATION) handler.post { updateSession() }
        }

    fun start() {
        context.getSharedPreferences("ceris_preferences", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener)
        refresh()
    }

    fun refresh() {
        updateSession()
        if (canSynchronize()) {
            state.requestRegistration()
            schedule()
        }
    }

    private fun updateSession() {
        val current = sessionManager.snapshot()
        val previous = lastSession
        val binding = state.binding()
        if (previous?.generation != current?.generation || binding?.generation?.let { it != current?.generation } == true) {
            scheduler?.cancel(JOB_ID)
            if (current == null) {
                publisher.clear()
                state.clearSession()
            } else {
                sessionManager.withCurrentSession(current) {
                    publisher.clear(current.generation)
                    state.clearSession(current.generation)
                }
            }
            if (previous != null && binding != null && binding.generation == previous.generation) execute { sync.revoke(binding, previous) }
        }
        lastSession = current
        if (current == null || !publisher.permitted()) {
            disable()
        } else {
            schedule()
        }
    }

    private fun disable() {
        scheduler?.cancel(JOB_ID)
        val binding = state.binding()
        val session = sessionManager.snapshot()
        publisher.clear()
        state.clearSession()
        if (binding != null && session != null) execute { sync.revoke(binding, session) }
    }

    fun logout() {
        val session = sessionManager.snapshot()
        val binding = state.binding()
        // A limpeza e imediata. O DELETE usa a credencial capturada da conta antiga.
        sessionManager.clear()
        scheduler?.cancel(JOB_ID)
        publisher.clear()
        state.clearSession()
        lastSession = null
        if (session != null && binding != null) execute { sync.revoke(binding, session) }
    }

    fun registered(fid: String) {
        if (state.registered(fid) && canSynchronize()) schedule()
    }

    fun receive(data: Map<String, String>): Boolean = messages.receive(data)

    fun canSynchronize(): Boolean =
        ApiBaseUrl.configurada(ApiBaseUrl.notify) && sessionManager.snapshot()?.profileId != null && publisher.permitted()

    private fun schedule() {
        if (!canSynchronize() || scheduler?.getPendingJob(JOB_ID) != null) return
        val job =
            JobInfo.Builder(JOB_ID, ComponentName(context, PushSyncJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()
        try {
            if (scheduler?.schedule(job) == JobScheduler.RESULT_FAILURE) report("Sincronizacao push aguardando nova abertura do app")
        } catch (_: RuntimeException) {
            report("Nao foi possivel agendar a sincronizacao push agora")
        }
    }

    internal fun execute(action: () -> Unit) {
        executor.execute {
            try {
                action()
            } catch (_: RuntimeException) {
                report("Nao foi possivel concluir a operacao push agora")
            }
        }
    }

    internal fun synchronize(): Boolean {
        if (!canSynchronize()) return false
        try {
            if (state.registrationPending || state.installationId == null) {
                Tasks.await(FirebaseMessaging.getInstance().register(), 30, TimeUnit.SECONDS)
            }
            if (!canSynchronize()) return false
            if (state.registrationPending || state.installationId == null) return true
            return sync.synchronize() || state.registrationPending
        } catch (_: ExecutionException) {
            report("Registro no Firebase aguardando nova tentativa")
        } catch (_: TimeoutException) {
            report("Registro no Firebase aguardando nova tentativa")
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: RuntimeException) {
            report("Nao foi possivel sincronizar o push agora")
            return false
        }
        return canSynchronize()
    }

    private fun report(message: String) {
        Log.w("CerisPush", message)
    }

    companion object {
        private const val JOB_ID = 0xCE215

        fun get(context: Context): PushNotifications = (context.applicationContext as CerisApplication).pushNotifications
    }
}
