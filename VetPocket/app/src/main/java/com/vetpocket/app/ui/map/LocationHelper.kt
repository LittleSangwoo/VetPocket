package com.vetpocket.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun current(context: Context, onResult: (Location?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val lm = context.getSystemService(LocationManager::class.java)

        val usable = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { it != LocationManager.GPS_PROVIDER || fine }
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        if (usable.isEmpty()) {
            onResult(lastKnown(lm))
            return
        }
        try {
            LocationManagerCompat.getCurrentLocation(
                lm,
                usable.first(),
                null as android.os.CancellationSignal?, // Явно указываем тип для устранения неоднозначности
                ContextCompat.getMainExecutor(context),
                androidx.core.util.Consumer<Location> { location ->
                    onResult(location ?: lastKnown(lm))
                }
            )
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
}
