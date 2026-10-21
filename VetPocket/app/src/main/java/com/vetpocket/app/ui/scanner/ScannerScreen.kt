package com.vetpocket.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.remote.FoodAnalysis
import com.vetpocket.app.data.remote.Verdict
import com.vetpocket.app.ui.components.AppCard
import com.vetpocket.app.ui.components.ScreenHeader
import com.vetpocket.app.ui.theme.VerdictGreen
import com.vetpocket.app.ui.theme.VerdictRed
import com.vetpocket.app.ui.theme.VerdictYellow

@Composable
fun ScannerScreen(container: AppContainer) {
    val context = LocalContext.current // <-- 1. Получаем контекст экрана

    // 2. Передаем context в фабрику
    val vm: ScannerViewModel = viewModel(factory = ScannerViewModel.factory(container, context))

    val state by vm.state.collectAsStateWithLifecycle()
    val pet by vm.pet.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenHeader(
            "Анализатор корма",
            "Сфотографируйте состав — нейросеть оценит его именно для вашего питомца",
        )

        state.error?.let { ErrorBanner(it, onDismiss = vm::dismissError) }

        when (state.stage) {
            ScanStage.Camera -> CameraStage(
                onCaptured = vm::onPhotoCaptured,
                onCameraError = vm::onCameraError,
            )
            ScanStage.Recognizing -> LoadingStage("Распознаю текст на устройстве…", "Без интернета, фото никуда не отправляется")
            ScanStage.Review -> ReviewStage(
                text = state.text,
                petName = pet?.name,
                hasPet = pet != null,
                onTextChange = vm::onTextEdited,
                onRetake = vm::backToCamera,
                onAnalyze = vm::analyze,
            )
            ScanStage.Analyzing -> LoadingStage(
                "Нейросеть изучает состав…",
                pet?.name?.let { "Учитываем профиль: $it" } ?: "Учитываем профиль питомца",
            )
            ScanStage.Result -> state.analysis?.let { ResultStage(it, onAgain = vm::backToCamera, onEdit = vm::backToReview) }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    AppCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// --------------------------------------------------------------- Камера

@Composable
private fun CameraStage(onCaptured: (android.graphics.Bitmap) -> Unit, onCameraError: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Нужен доступ к камере", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Камера используется только для съёмки состава корма. Снимок обрабатывается на устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Разрешить") }
        }
        return
    }

    var controller by remember { mutableStateOf<CameraController?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            CameraCapture(
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(28.dp)),
                onReady = { controller = it },
            )
        }
        Text(
            "Наведите рамку на текст состава",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {
                    val c = controller ?: return@Button
                    busy = true
                    c.takePhoto(
                        onBitmap = { bmp ->
                            busy = false
                            onCaptured(bmp)
                        },
                        onFailure = { msg ->
                            busy = false
                            onCameraError(msg)
                        },
                    )
                },
                enabled = controller != null && !busy,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Сфотографировать", fontSize = 16.sp)
            }
        }
    }
}

// --------------------------------------------------------------- Загрузка

@Composable
private fun LoadingStage(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 5.dp)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// --------------------------------------------------------------- Подтверждение текста

@Composable
private fun ReviewStage(
    text: String,
    petName: String?,
    hasPet: Boolean,
    onTextChange: (String) -> Unit,
    onRetake: () -> Unit,
    onAnalyze: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text("Проверьте распознанный текст", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Исправьте ошибки распознавания и удалите всё лишнее (штрихкоды, адреса, акции) — в нейросеть уйдёт только этот текст.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Состав корма") },
            minLines = 8,
            maxLines = 14,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        )
        if (!hasPet) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Профиль питомца не заполнен. Создайте его на главном экране — оценка будет учитывать возраст и хронические состояния.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (petName != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Оценка будет учитывать профиль: $petName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Переснять")
            }
            Button(
                onClick = onAnalyze,
                enabled = text.isNotBlank() && hasPet,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Анализировать")
            }
        }
    }
}

// --------------------------------------------------------------- Результат

private data class VerdictStyle(val emoji: String, val title: String, val color: Color)

private fun Verdict.style() = when (this) {
    Verdict.GREAT -> VerdictStyle("🟢", "Отлично", VerdictGreen)
    Verdict.CAUTION -> VerdictStyle("🟡", "С осторожностью", VerdictYellow)
    Verdict.BAD -> VerdictStyle("🔴", "Не рекомендуется", VerdictRed)
}

@Composable
private fun ResultStage(analysis: FoodAnalysis, onAgain: () -> Unit, onEdit: () -> Unit) {
    val style = analysis.verdict.style()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        // Цветовой вердикт
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(style.color.copy(alpha = 0.14f))
                .border(2.dp, style.color.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(style.color),
                contentAlignment = Alignment.Center,
            ) { Text(style.emoji, fontSize = 26.sp) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Вердикт", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(style.title, style = MaterialTheme.typography.headlineSmall, color = style.color, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Резюме", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(analysis.summary, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (analysis.pros.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PointsCard("Плюсы", analysis.pros, VerdictGreen, "＋")
        }
        if (analysis.cons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PointsCard("Минусы", analysis.cons, VerdictRed, "−")
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Оценка сформирована нейросетью и носит справочный характер. Она не заменяет консультацию ветеринара — особенно при хронических заболеваниях.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("К тексту") }
            Button(
                onClick = onAgain,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Новый корм")
            }
        }
    }
}

@Composable
private fun PointsCard(title: String, points: List<String>, color: Color, marker: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Spacer(Modifier.height(8.dp))
            points.forEach { point ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(marker, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Text(point, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
