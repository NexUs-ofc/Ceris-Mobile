package com.example.ceris.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Limita HTTP sem perder a nova origem: movimento de 100 m ou renovacao apos 1 minuto. */
class NearbyRefreshPolicy {
    private var last: DeviceCoordinates? = null
    private var queriedAt = 0L

    fun shouldQuery(
        point: DeviceCoordinates,
        nowMs: Long,
    ): Boolean {
        val previous = last
        val elapsed = nowMs - queriedAt
        if (previous != null && (elapsed < 15_000 || (elapsed < 60_000 && distanceMeters(previous, point) < 100))) return false
        last = point
        queriedAt = nowMs
        return true
    }

    fun reset() {
        last = null
    }

    private fun distanceMeters(
        first: DeviceCoordinates,
        second: DeviceCoordinates,
    ): Double {
        val lat = Math.toRadians(second.latitude - first.latitude)
        val lon = Math.toRadians(second.longitude - first.longitude)
        val a =
            sin(lat / 2) * sin(lat / 2) +
                cos(Math.toRadians(first.latitude)) * cos(Math.toRadians(second.latitude)) * sin(lon / 2) * sin(lon / 2)
        return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
