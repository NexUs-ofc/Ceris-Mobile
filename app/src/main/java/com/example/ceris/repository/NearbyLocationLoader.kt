package com.example.ceris.repository

import com.example.ceris.location.DeviceCoordinates
import com.example.ceris.location.DeviceLocationResult
import com.example.ceris.location.LocationSource
import com.example.ceris.location.NearbyRefreshPolicy

/** Acompanha a origem da consulta e descarta respostas de uma origem ou sessao anterior. */
class NearbyLocationLoader(
    private val location: LocationSource,
    private val generation: () -> String?,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val fallback: (((LocaisResult) -> Unit) -> (() -> Unit))? = null,
    private val permissionFallbackAllowed: () -> Boolean = { false },
    private val nearby: (DeviceCoordinates, (LocaisResult) -> Unit) -> (() -> Unit),
) {
    fun load(deliver: (LocaisResult) -> Unit): () -> Unit {
        val owner = generation()
        if (owner == null) {
            deliver(LocaisResult.AuthenticationRequired)
            return {}
        }
        var canceled = false
        var sequence = 0L
        var usingProfile = false
        var profileQueriedAt: Long? = null
        val policy = NearbyRefreshPolicy()
        var cancelHttp: (() -> Unit)? = null

        fun query(
            profile: Boolean,
            source: ((LocaisResult) -> Unit) -> (() -> Unit),
        ) {
            val request = ++sequence
            cancelHttp?.invoke()
            cancelHttp = null
            usingProfile = profile
            deliver(LocaisResult.QueryStarted(profile))
            if (canceled) return
            val cancellation =
                source {
                    if (!canceled && request == sequence) {
                        deliver(if (generation() == owner) it else LocaisResult.SessionChanged)
                    }
                }
            if (canceled || request != sequence) cancellation() else cancelHttp = cancellation
        }

        val cancelLocation =
            location.observe { result ->
                if (!canceled) {
                    when {
                        generation() != owner -> {
                            sequence++
                            cancelHttp?.invoke()
                            cancelHttp = null
                            deliver(LocaisResult.SessionChanged)
                        }
                        result is DeviceLocationResult.Available -> {
                            if (usingProfile) policy.reset()
                            if (policy.shouldQuery(result.coordinates, nowMs())) {
                                profileQueriedAt = null
                                query(false) { callback -> nearby(result.coordinates, callback) }
                            }
                        }
                        fallback != null &&
                            (result == DeviceLocationResult.Unavailable || permissionFallbackAllowed()) -> {
                            val now = nowMs()
                            val previous = profileQueriedAt
                            if (!usingProfile || previous == null || now - previous >= 60_000) {
                                policy.reset()
                                profileQueriedAt = now
                                query(true, fallback)
                            }
                        }
                        else -> {
                            sequence++
                            cancelHttp?.invoke()
                            cancelHttp = null
                            policy.reset()
                            deliver(
                                if (result == DeviceLocationResult.PermissionRequired) {
                                    LocaisResult.LocationPermissionRequired
                                } else {
                                    LocaisResult.LocationUnavailable
                                },
                            )
                        }
                    }
                }
            }
        return {
            canceled = true
            sequence++
            cancelLocation()
            cancelHttp?.invoke()
        }
    }
}
