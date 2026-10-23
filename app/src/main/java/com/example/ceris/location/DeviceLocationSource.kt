package com.example.ceris.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat

data class DeviceCoordinates(val latitude: Double, val longitude: Double)

sealed interface DeviceLocationResult {
    data class Available(val coordinates: DeviceCoordinates) : DeviceLocationResult

    data object PermissionRequired : DeviceLocationResult

    data object Unavailable : DeviceLocationResult
}

fun interface LocationSource {
    fun observe(deliver: (DeviceLocationResult) -> Unit): () -> Unit
}

/** Acompanha a posicao enquanto Locais esta visivel; cancelar remove todos os listeners. */
class DeviceLocationSource(context: Context) : LocationSource {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission") // Ambas as permissoes sao verificadas antes da consulta.
    override fun observe(deliver: (DeviceLocationResult) -> Unit): () -> Unit {
        val precise = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!precise && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            deliver(DeviceLocationResult.PermissionRequired)
            return {}
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val available =
            manager.allProviders.filter {
                it == LocationManager.NETWORK_PROVIDER || it == "fused" || (precise && it == LocationManager.GPS_PROVIDER)
            }
        val providers = if ("fused" in available) listOf("fused") else available
        if (providers.isEmpty()) {
            deliver(DeviceLocationResult.Unavailable)
            return {}
        }
        var closed = false
        var best: Location? = null
        var deliveredPosition = false
        val listeners = mutableListOf<LocationListenerCompat>()

        fun age(location: Location): Long = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L

        fun publish() {
            val current = best
            if (LocationManagerCompat.isLocationEnabled(manager) && current != null && age(current) in 0..30_000) {
                deliveredPosition = true
                deliver(DeviceLocationResult.Available(DeviceCoordinates(current.latitude, current.longitude)))
            } else {
                best = null
                deliveredPosition = false
                deliver(DeviceLocationResult.Unavailable)
            }
        }

        val freshness =
            object : Runnable {
                override fun run() {
                    if (closed) return
                    // Usa a aproximada se nao chegou uma leitura precisa; nunca mantem uma posicao velha indefinidamente.
                    publish()
                    handler.postDelayed(this, 20_000L)
                }
            }
        val request =
            LocationRequestCompat.Builder(10_000L)
                .setMinUpdateIntervalMillis(5_000L)
                .setQuality(
                    if (precise) LocationRequestCompat.QUALITY_HIGH_ACCURACY else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY,
                )
                .build()
        providers.forEach { provider ->
            if (closed) return@forEach
            val listener =
                object : LocationListenerCompat {
                    override fun onLocationChanged(location: Location) {
                        if (closed || age(location) !in 0..30_000 ||
                            !location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
                            !location.longitude.isFinite() || location.longitude !in -180.0..180.0
                        ) {
                            return
                        }
                        val previous = best
                        if (previous != null && age(previous) < 10_000 && previous.accuracy < location.accuracy) return
                        best = location
                        if (!precise || location.accuracy <= 100f || deliveredPosition) publish()
                    }

                    override fun onProviderDisabled(provider: String) {
                        if (!closed && !LocationManagerCompat.isLocationEnabled(manager)) {
                            best = null
                            publish()
                        }
                    }
                }
            listeners += listener
            try {
                LocationManagerCompat.requestLocationUpdates(manager, provider, request, ContextCompat.getMainExecutor(context), listener)
            } catch (_: SecurityException) {
                closed = true
                listeners.forEach { manager.removeUpdates(it) }
                deliver(DeviceLocationResult.PermissionRequired)
            } catch (_: IllegalArgumentException) {
                listeners.remove(listener)
            }
        }
        if (!closed) {
            if (listeners.isEmpty() || !LocationManagerCompat.isLocationEnabled(manager)) deliver(DeviceLocationResult.Unavailable)
            if (listeners.isNotEmpty()) handler.postDelayed(freshness, 20_000L)
        }
        return {
            closed = true
            handler.removeCallbacks(freshness)
            listeners.forEach { manager.removeUpdates(it) }
        }
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
