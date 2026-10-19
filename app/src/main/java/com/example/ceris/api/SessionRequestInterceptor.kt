package com.example.ceris.api

import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Liga cada chamada autenticada a sessao que a iniciou. */
class SessionRequestInterceptor(private val sessionManager: SessionManager) : Interceptor {
    /** Captura a sessao antes da fila do OkHttp, inclusive nas APIs sem @Tag. */
    internal fun vincularChamadas(factory: Call.Factory): Call.Factory =
        Call.Factory { request ->
            val authorization = request.header("Authorization")
            val session = if (authorization != null) sessionManager.snapshot() else null
            val prepared =
                if (request.tag(SessionSnapshot::class.java) == null && session != null &&
                    authorization == "Bearer ${session.accessToken}"
                ) {
                    request.newBuilder().tag(SessionSnapshot::class.java, session).build()
                } else {
                    request
                }
            factory.newCall(prepared)
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authorization = request.header("Authorization") ?: return chain.proceed(request)
        val session = sessionManager.snapshot() ?: throw IOException("Sessao encerrada antes do envio.")
        val origin = request.tag(SessionSnapshot::class.java)

        if (origin != null && origin.generation != session.generation) {
            throw IOException("A sessao mudou antes do envio. Tente novamente.")
        }
        if (authorization != "Bearer ${(origin ?: session).accessToken}") {
            throw IOException("A sessao mudou antes do envio. Tente novamente.")
        }

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .tag(SessionSnapshot::class.java, session).build(),
        )
    }
}
