package com.example.ceris.repository

import com.example.ceris.api.NotificationSourceAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.EventNotificationSource
import com.example.ceris.model.dto.StockNotificationSource
import com.example.ceris.model.dto.StoreNotificationSource
import com.example.ceris.notification.NotificationLink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed interface NotificationContent {
    data class Product(val value: StockNotificationSource) : NotificationContent

    data class Store(val value: StoreNotificationSource) : NotificationContent

    data class Event(val value: EventNotificationSource) : NotificationContent
}

sealed interface NotificationSourceResult {
    data class Loaded(val content: NotificationContent) : NotificationSourceResult

    data object Unavailable : NotificationSourceResult

    data object AuthenticationRequired : NotificationSourceResult

    data object WrongAccount : NotificationSourceResult

    data object SessionChanged : NotificationSourceResult

    data object Failure : NotificationSourceResult
}

class NotificationSourceRepository(
    private val api: NotificationSourceAPI,
    private val sessions: SessionManager,
) {
    fun load(
        link: NotificationLink,
        deliver: (NotificationSourceResult) -> Unit,
    ): Call<*>? {
        val session = sessions.snapshot()
        if (session == null) {
            deliver(NotificationSourceResult.AuthenticationRequired)
            return null
        }
        if (!link.belongsTo(session.profileId)) {
            deliver(NotificationSourceResult.WrongAccount)
            return null
        }
        val authorization = "Bearer ${session.accessToken}"
        return when (link.destination) {
            NotificationLink.Destination.PRODUCT ->
                request(api.stock(authorization, session), session, deliver) { body ->
                    body.firstOrNull { it.id?.toString() == link.sourceId }?.let { NotificationContent.Product(it) }
                }
            NotificationLink.Destination.STORE ->
                request(api.store(authorization, link.sourceId, session), session, deliver) { body ->
                    require(body.id?.toString() == link.sourceId) { "Resposta de outra loja" }
                    NotificationContent.Store(body)
                }
            NotificationLink.Destination.EVENT ->
                request(api.event(authorization, link.sourceId, session), session, deliver) { body ->
                    require(body.id == link.sourceId) { "Resposta de outro evento" }
                    NotificationContent.Event(body)
                }
        }
    }

    private fun <T> request(
        call: Call<T>,
        session: SessionSnapshot,
        deliver: (NotificationSourceResult) -> Unit,
        convert: (T) -> NotificationContent?,
    ): Call<T> {
        call.enqueue(
            object : Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: Response<T>,
                ) {
                    response.errorBody()?.close()
                    if (call.isCanceled) return
                    val result =
                        when {
                            !sessions.isCurrent(session) -> NotificationSourceResult.SessionChanged
                            response.code() == 401 -> NotificationSourceResult.AuthenticationRequired
                            response.code() in setOf(403, 404, 410) -> NotificationSourceResult.Unavailable
                            !response.isSuccessful || response.body() == null -> NotificationSourceResult.Failure
                            else ->
                                runCatching {
                                    convert(requireNotNull(response.body()))?.let { NotificationSourceResult.Loaded(it) }
                                        ?: NotificationSourceResult.Unavailable
                                }.getOrDefault(NotificationSourceResult.Failure)
                        }
                    deliver(result)
                }

                override fun onFailure(
                    call: Call<T>,
                    t: Throwable,
                ) {
                    if (!call.isCanceled) {
                        deliver(
                            if (sessions.isCurrent(session)) NotificationSourceResult.Failure else NotificationSourceResult.SessionChanged,
                        )
                    }
                }
            },
        )
        return call
    }
}
