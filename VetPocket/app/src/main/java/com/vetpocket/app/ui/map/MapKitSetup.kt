package com.vetpocket.app.ui.map

import android.content.Context
import com.vetpocket.app.BuildConfig
import com.yandex.mapkit.MapKitFactory

/** Ленивая инициализация MapKit: нативные библиотеки грузятся только при первом открытии карты. */
object MapKitSetup {
    private var initialized = false

    val isConfigured: Boolean get() = BuildConfig.YANDEX_MAPKIT_KEY.isNotBlank()

    fun ensureInitialized(context: Context): Boolean {
        if (!isConfigured) return false
        if (!initialized) {
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPKIT_KEY)
            MapKitFactory.initialize(context.applicationContext)
            initialized = true
        }
        return true
    }
}
