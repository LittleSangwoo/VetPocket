package com.vetpocket.app.ui.map

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vetpocket.app.AppContainer
import com.vetpocket.app.ui.components.AppCard
import com.vetpocket.app.ui.components.ScreenHeader
import com.vetpocket.app.ui.theme.VerdictGreen
import com.vetpocket.app.util.formatDistance
import com.yandex.mapkit.geometry.Point

@Composable
fun MapScreen(container: AppContainer) {
    val context = LocalContext.current
    val ready = remember { MapKitSetup.ensureInitialized(context) }
    if (ready) {
        MapContent(container)
    } else {
        MapKeyMissing()
    }
}

@Composable
private fun MapKeyMissing() {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Карта", "Ветеринарные клиники рядом")
        AppCard(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Не задан ключ Yandex MapKit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Получите ключ в кабинете разработчика Яндекса (MapKit Mobile SDK), добавьте строку YANDEX_MAPKIT_KEY=… в файл local.properties и пересоберите проект.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MapContent(container: AppContainer) {
    val context = LocalContext.current
    val vm: MapViewModel = viewModel(factory = MapViewModel.factory())
    val state by vm.state.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (!hasPermission) vm.onLocation(null)
    }

    fun locate() {
        LocationHelper.current(context) { location ->
            vm.onLocation(location?.let { Point(it.latitude, it.longitude) })
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    LaunchedEffect(hasPermission) { if (hasPermission) locate() }

    // Кнопка SOS на главном экране сразу включает фильтр «Открыто прямо сейчас».
    LaunchedEffect(Unit) {
        container.sosRequested.collect { requested ->
            if (requested) {
                vm.setOpenNow(true)
                container.sosRequested.value = false
            }
        }
    }

    val visible = state.visibleClinics

    Box(Modifier.fillMaxSize()) {
        YandexMap(
            userPoint = state.userPoint,
            clinics = visible,
            selectedId = state.selectedId,
            cameraTarget = state.cameraTarget,
            onClinicTap = vm::select,
            modifier = Modifier.fillMaxSize(),
        )

        // Верхняя панель
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ветеринарные клиники", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.loading) "Ищем рядом…" else "Найдено: ${visible.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilterChip(
                        selected = state.openNowOnly,
                        onClick = { vm.setOpenNow(!state.openNowOnly) },
                        label = { Text("Открыто прямо сейчас") },
                        leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    if (state.loading) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                AppCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(
                        Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = {
                                if (!hasPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                } else if (state.userPoint == null) locate() else vm.refreshSearch()
                            },
                        ) { Text("Обновить") }
                    }
                }
            }
        }

        // Кнопка «где я»
        SmallFloatingActionButton(
            onClick = { if (state.userPoint != null) vm.recenter() else locate() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (state.selected != null) 220.dp else 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) { Icon(Icons.Rounded.MyLocation, contentDescription = "Моё местоположение") }

        // Карточка выбранной клиники
        AnimatedVisibility(
            visible = state.selected != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            state.selected?.let { clinic ->
                ClinicCard(
                    clinic = clinic,
                    onClose = { vm.select(null) },
                    onRoute = { openRoute(context, clinic) },
                    onCall = { clinic.phone?.let { dial(context, it) } },
                )
            }
        }
    }
}

@Composable
private fun ClinicCard(clinic: Clinic, onClose: () -> Unit, onRoute: () -> Unit, onCall: () -> Unit) {
    AppCard(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(clinic.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    if (clinic.address.isNotBlank()) {
                        Text(
                            clinic.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDistance(clinic.distanceMeters), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val status = clinic.workingText
                if (status != null) {
                    Text("  ·  ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (clinic.isOpenNow) {
                            true -> VerdictGreen
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            clinic.phone?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onRoute,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Маршрут")
                }
                Button(
                    onClick = onCall,
                    enabled = clinic.phone != null,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Позвонить")
                }
            }
        }
    }
}

/** Маршрут строится в приложении «Яндекс Карты»; если его нет — открывается веб-версия. */
private fun openRoute(context: Context, clinic: Clinic) {
    val lat = clinic.point.latitude
    val lon = clinic.point.longitude
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("yandexmaps://maps.yandex.ru/?rtext=~$lat,$lon&rtt=auto"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(app)
    } catch (e: ActivityNotFoundException) {
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://yandex.ru/maps/?rtext=~$lat%2C$lon&rtt=auto"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(web) }
    }
}

private fun dial(context: Context, phone: String) {
    val number = phone.filter { it.isDigit() || it == '+' }
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
