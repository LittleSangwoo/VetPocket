package com.vetpocket.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

/** Рисует маркеры кодом — без внешних ресурсов. */
private object MarkerIcons {
    fun clinic(context: Context, selected: Boolean, openNow: Boolean?): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = ((if (selected) 52 else 40) * density).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val fill = when {
            selected -> AndroidColor.parseColor("#FF6B4A")
            openNow == true -> AndroidColor.parseColor("#0F766E")
            openNow == false -> AndroidColor.parseColor("#7C8A8C")
            else -> AndroidColor.parseColor("#0F766E")
        }
        val r = size / 2f
        paint.color = AndroidColor.WHITE
        c.drawCircle(r, r, r, paint)
        paint.color = fill
        c.drawCircle(r, r, r - 3 * density, paint)
        // белый крест
        paint.color = AndroidColor.WHITE
        val arm = r * 0.5f
        val thick = r * 0.22f
        c.drawRoundRect(r - arm, r - thick / 2, r + arm, r + thick / 2, thick / 2, thick / 2, paint)
        c.drawRoundRect(r - thick / 2, r - arm, r + thick / 2, r + arm, thick / 2, thick / 2, paint)
        return bmp
    }

    fun user(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (28 * density).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size / 2f
        paint.color = AndroidColor.argb(70, 33, 150, 243)
        c.drawCircle(r, r, r, paint)
        paint.color = AndroidColor.WHITE
        c.drawCircle(r, r, r * 0.55f, paint)
        paint.color = AndroidColor.parseColor("#2196F3")
        c.drawCircle(r, r, r * 0.4f, paint)
        return bmp
    }
}

@Composable
fun YandexMap(
    userPoint: Point?,
    clinics: List<Clinic>,
    selectedId: String?,
    cameraTarget: CameraTarget?,
    onClinicTap: (Clinic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkTheme = isSystemInDarkTheme()
    val latestOnTap by rememberUpdatedState(onClinicTap)

    val mapView = remember { MapView(context) }
    val map = remember(mapView) { mapView.mapWindow.map }
    val clinicLayer = remember(map) { map.mapObjects.addCollection() }
    val userMark = remember(map) {
        map.mapObjects.addPlacemark().apply {
            setIcon(ImageProvider.fromBitmap(MarkerIcons.user(context)))
            isVisible = false
        }
    }
    // MapKit хранит только слабые ссылки на слушатели — держим сами.
    val tapListener = remember {
        MapObjectTapListener { mapObject, _ ->
            (mapObject.userData as? Clinic)?.let { latestOnTap(it) }
            true
        }
    }

    // Жизненный цикл: MapKit и MapView должны стартовать/останавливаться вместе с экраном.
    DisposableEffect(lifecycleOwner, mapView) {
        var started = false
        fun start() {
            if (!started) {
                MapKitFactory.getInstance().onStart()
                mapView.onStart()
                started = true
            }
        }
        fun stop() {
            if (started) {
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
                started = false
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    // Стартовая позиция (Москва), пока не определено местоположение пользователя.
    LaunchedEffect(map) {
        map.move(CameraPosition(Point(55.751244, 37.618423), 9f, 0f, 0f))
    }

    LaunchedEffect(darkTheme) { map.isNightModeEnabled = darkTheme }

    LaunchedEffect(userPoint) {
        if (userPoint != null) {
            userMark.geometry = userPoint
            userMark.isVisible = true
        } else {
            userMark.isVisible = false
        }
    }

    LaunchedEffect(clinics, selectedId) {
        clinicLayer.clear()
        clinics.forEach { clinic ->
            val icon = MarkerIcons.clinic(context, clinic.id == selectedId, clinic.isOpenNow)
            val placemark = clinicLayer.addPlacemark()
            placemark.geometry = clinic.point
            placemark.setIcon(ImageProvider.fromBitmap(icon))
            placemark.userData = clinic
            placemark.addTapListener(tapListener)
        }
    }

    LaunchedEffect(cameraTarget) {
        cameraTarget?.let {
            map.move(
                CameraPosition(it.point, it.zoom, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.6f),
                null,
            )
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
