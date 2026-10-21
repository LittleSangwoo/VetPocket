package com.vetpocket.app.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.UpcomingEvent
import com.vetpocket.app.ui.components.AppCard
import com.vetpocket.app.ui.components.AppDatePickerDialog
import com.vetpocket.app.ui.components.EmptyHint
import com.vetpocket.app.ui.components.PetAvatar
import com.vetpocket.app.ui.components.SectionTitle
import com.vetpocket.app.ui.components.icon
import com.vetpocket.app.ui.theme.SosRedDark
import com.vetpocket.app.ui.theme.SosRedLight
import com.vetpocket.app.ui.theme.Teal900
import com.vetpocket.app.util.FullDateFormatter
import com.vetpocket.app.util.formatAge
import com.vetpocket.app.util.formatWeight
import com.vetpocket.app.util.untilText
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(container: AppContainer, onSos: () -> Unit) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    var showWeightSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Первый запуск: сразу просим создать профиль питомца.
    LaunchedEffect(state.loaded, state.pet == null) {
        if (state.loaded && state.pet == null) showProfileSheet = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PetHeader(
            state = state,
            onEdit = { showProfileSheet = true },
        )

        Spacer(Modifier.height(20.dp))

        if (state.pet == null && state.loaded) {
            WelcomeCard(onCreate = { showProfileSheet = true })
            Spacer(Modifier.height(20.dp))
        }

        SectionTitle("Ближайшие события")
        if (state.upcoming.isEmpty()) {
            EmptyHint("Пока нет запланированных процедур. Добавьте прививку или обработку в Медкарте и включите «Напомнить о следующей» — здесь появится таймер.")
        } else {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.upcoming.forEach { UpcomingCard(it) }
            }
        }

        Spacer(Modifier.height(24.dp))
        SosCard(onClick = onSos)

        Spacer(Modifier.height(20.dp))
        SectionTitle("Быстрые действия")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { showWeightSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = state.pet != null,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Взвесить")
            }
            OutlinedButton(
                onClick = { showProfileSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Профиль")
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showWeightSheet) {
        WeightSheet(
            onDismiss = { showWeightSheet = false },
            onSave = { kg ->
                vm.addWeight(kg)
                showWeightSheet = false
            },
        )
    }

    if (showProfileSheet) {
        ProfileSheet(
            existing = state.pet,
            onDismiss = { showProfileSheet = false },
            onSave = { name, birth, neutered, breed, chronic, photo ->
                vm.saveProfile(name, birth, neutered, breed, chronic, photo)
                showProfileSheet = false
            },
        )
    }
}

// ---------------------------------------------------------------- Шапка

@Composable
private fun PetHeader(state: DashboardState, onEdit: () -> Unit) {
    val pet = state.pet
    val gradient = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Teal900))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(gradient)
    ) {
        // Декоративные отпечатки лап
        Icon(
            Icons.Rounded.Pets,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = 20.dp)
                .size(150.dp)
                .rotate(20f),
        )
        Icon(
            Icons.Rounded.Pets,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 24.dp)
                .size(100.dp)
                .rotate(-15f),
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .padding(3.dp)
                        .clickable(onClick = onEdit)
                ) {
                    PetAvatar(photoPath = pet?.photoPath, size = 88.dp)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pet?.name ?: "Ваш питомец",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = buildString {
                        if (pet != null) {
                            if (pet.breed.isNotBlank()) append(pet.breed) else append("Кот")
                            append(if (pet.isNeutered) " · кастрирован" else "")
                        } else {
                            append("Добавьте профиль, чтобы начать")
                        }
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(
                    label = "Возраст",
                    value = pet?.let { formatAge(LocalDate.ofEpochDay(it.birthDateEpochDay)) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Вес",
                    value = state.latestWeight?.let { "${formatWeight(it)} кг" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WelcomeCard(onCreate: () -> Unit) {
    AppCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Добро пожаловать в VetPocket", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(
                "Заполните профиль питомца: имя, возраст, статус кастрации и хронические состояния. Нейросеть будет учитывать их при оценке корма.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onCreate) { Text("Создать профиль") }
        }
    }
}

// ---------------------------------------------------------------- События

@Composable
private fun UpcomingCard(item: UpcomingEvent) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), item.date)
    val accent = when {
        days < 0 -> MaterialTheme.colorScheme.error
        days <= 7 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    AppCard(Modifier.width(184.dp)) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.procedure.icon(), contentDescription = null, tint = accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(item.procedure.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Spacer(Modifier.height(2.dp))
            Text(
                untilText(item.date).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    item.date.format(FullDateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- SOS

@Composable
private fun SosCard(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "sos")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(SosRedLight, SosRedDark)))
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .scale(pulse)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("SOS", color = SosRedDark, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Экстренная помощь", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Ближайшие работающие ветклиники на карте",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

// ---------------------------------------------------------------- Шторка веса

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightSheet(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Новое взвешивание", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6)
                    error = false
                },
                label = { Text("Вес") },
                suffix = { Text("кг") },
                singleLine = true,
                isError = error,
                supportingText = { if (error) Text("Введите вес от 0,1 до 60 кг") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val kg = text.replace(',', '.').toFloatOrNull()
                    if (kg == null || kg < 0.1f || kg > 60f) error = true else onSave(kg)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Сохранить") }
        }
    }
}

// ---------------------------------------------------------------- Шторка профиля

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(
    existing: com.vetpocket.app.data.db.PetProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, Boolean, String, String, android.net.Uri?) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var breed by remember { mutableStateOf(existing?.breed.orEmpty()) }
    var chronic by remember { mutableStateOf(existing?.chronicConditions.orEmpty()) }
    var neutered by remember { mutableStateOf(existing?.isNeutered ?: false) }
    var birth by remember { mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.birthDateEpochDay) }) }
    var newPhoto by remember { mutableStateOf<android.net.Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) newPhoto = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                if (existing == null) "Профиль питомца" else "Редактировать профиль",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PetAvatar(photoPath = newPhoto ?: existing?.photoPath, size = 72.dp)
                Spacer(Modifier.width(16.dp))
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Выбрать фото")
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                singleLine = true,
                isError = showErrors && name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Порода (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (showErrors && birth == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(birth?.format(FullDateFormatter)?.let { "Дата рождения: $it" } ?: "Дата рождения")
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { neutered = !neutered }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Кастрация / стерилизация", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = neutered, onCheckedChange = { neutered = it })
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = chronic,
                onValueChange = { chronic = it },
                label = { Text("Хронические заболевания") },
                supportingText = { Text("Например: склонность к МКБ. Учитывается при анализе корма.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val b = birth
                    if (name.isBlank() || b == null) {
                        showErrors = true
                    } else {
                        onSave(name, b, neutered, breed, chronic, newPhoto)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Сохранить") }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initial = birth,
            onDismiss = { showDatePicker = false },
            onPick = { picked -> birth = if (picked.isAfter(LocalDate.now())) LocalDate.now() else picked },
        )
    }
}
