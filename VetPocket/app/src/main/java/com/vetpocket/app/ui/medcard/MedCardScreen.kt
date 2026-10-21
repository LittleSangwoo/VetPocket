package com.vetpocket.app.ui.medcard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.vetpocket.app.AppContainer
import com.vetpocket.app.data.db.DiaryNoteEntity
import com.vetpocket.app.data.db.EventType
import com.vetpocket.app.data.db.MedicalEventEntity
import com.vetpocket.app.data.db.ProcedureType
import com.vetpocket.app.data.db.WeightRecordEntity
import com.vetpocket.app.ui.components.AppCard
import com.vetpocket.app.ui.components.AppDatePickerDialog
import com.vetpocket.app.ui.components.ConfirmDialog
import com.vetpocket.app.ui.components.EmptyHint
import com.vetpocket.app.ui.components.ScreenHeader
import com.vetpocket.app.ui.components.icon
import com.vetpocket.app.util.FullDateFormatter
import com.vetpocket.app.util.RuLocale
import com.vetpocket.app.util.ShortDateFormatter
import com.vetpocket.app.util.formatWeight
import com.vetpocket.app.util.untilText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val TAB_TITLES = listOf("Прививки", "Вес", "Хроника")
private val SYMPTOM_TAGS = listOf("Вялый", "Срыгнул шерстью", "Не ест", "Рвота", "Диарея", "Чихает", "Хромает", "Много пьёт")
private val INTERVALS = listOf(1 to "1 мес.", 3 to "3 мес.", 6 to "6 мес.", 12 to "1 год")

@Composable
fun MedCardScreen(container: AppContainer) {
    val vm: MedCardViewModel = viewModel(factory = MedCardViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Медкарта", "История прививок, вес и наблюдения")
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) },
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> VaccinationsTab(
                    events = state.events,
                    onAdd = vm::addEvent,
                    onDelete = { e -> pendingDelete = { vm.deleteEvent(e) } },
                )
                1 -> WeightTab(
                    weights = state.weights,
                    onDelete = { w -> pendingDelete = { vm.deleteWeight(w) } },
                )
                else -> DiaryTab(
                    notes = state.notes,
                    onAdd = vm::addNote,
                    onDelete = { n -> pendingDelete = { vm.deleteNote(n) } },
                )
            }
        }
    }

    pendingDelete?.let { action ->
        ConfirmDialog(
            text = "Это действие нельзя отменить.",
            onConfirm = action,
            onDismiss = { pendingDelete = null },
        )
    }
}

// =============================================================== Вкладка 1: прививки

@Composable
private fun VaccinationsTab(
    events: List<MedicalEventEntity>,
    onAdd: (ProcedureType, String, LocalDate, Boolean, Int) -> Unit,
    onDelete: (MedicalEventEntity) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (events.isEmpty()) {
                item { EmptyHint("Записей пока нет. Нажмите «Добавить», чтобы внести прививку или обработку.") }
            }
            items(events, key = { it.id }) { event ->
                EventCard(event = event, onDelete = { onDelete(event) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showSheet = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Добавить") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
    if (showSheet) {
        AddEventSheet(
            onDismiss = { showSheet = false },
            onSave = { procedure, drug, date, remind, months ->
                onAdd(procedure, drug, date, remind, months)
                showSheet = false
            },
        )
    }
}

@Composable
private fun EventCard(event: MedicalEventEntity, onDelete: () -> Unit) {
    val isVaccine = event.type == EventType.VACCINATION
    val accent = if (isVaccine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(event.procedure.icon(), contentDescription = null, tint = accent) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.procedure.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    event.type.title + if (event.drugName.isNotBlank()) " · ${event.drugName}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        LocalDate.ofEpochDay(event.dateEpochDay).format(FullDateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val next = event.nextDateEpochDay
                if (event.isScheduledReminder && next != null) {
                    Spacer(Modifier.height(8.dp))
                    val nextDate = LocalDate.ofEpochDay(next)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Следующая: ${nextDate.format(FullDateFormatter)} (${untilText(nextDate)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEventSheet(
    onDismiss: () -> Unit,
    onSave: (ProcedureType, String, LocalDate, Boolean, Int) -> Unit,
) {
    val context = LocalContext.current
    var procedure by remember { mutableStateOf(ProcedureType.COMPLEX_VACCINE) }
    var drug by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var remind by remember { mutableStateOf(true) }
    var interval by remember { mutableIntStateOf(ProcedureType.COMPLEX_VACCINE.defaultIntervalMonths) }
    var showDatePicker by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // Разрешение на уведомления нужно только для напоминаний — просим его, когда пользователь действительно их включает.
    LaunchedEffect(remind) {
        if (remind && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            Text("Новая запись", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))

            Text("Процедура", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProcedureType.entries.forEach { p ->
                    FilterChip(
                        selected = procedure == p,
                        onClick = {
                            procedure = p
                            interval = p.defaultIntervalMonths
                        },
                        label = { Text(p.title) },
                        leadingIcon = { Icon(p.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = drug,
                onValueChange = { drug = it },
                label = { Text("Название препарата") },
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
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Дата: ${date.format(FullDateFormatter)}")
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = remind, onCheckedChange = { remind = it })
                Text("Напомнить о следующей", style = MaterialTheme.typography.bodyLarge)
            }
            if (remind) {
                FlowRow(
                    Modifier.padding(start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    INTERVALS.forEach { (months, label) ->
                        FilterChip(
                            selected = interval == months,
                            onClick = { interval = months },
                            label = { Text("Через $label") },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Напомним ${date.plusMonths(interval.toLong()).format(FullDateFormatter)} в 10:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(procedure, drug, date, remind, interval) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Сохранить") }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initial = date,
            onDismiss = { showDatePicker = false },
            onPick = { date = it },
        )
    }
}

// =============================================================== Вкладка 2: вес

@Composable
private fun WeightTab(weights: List<WeightRecordEntity>, onDelete: (WeightRecordEntity) -> Unit) {
    // На графике — последние измерения: так подписи по оси X остаются читаемыми.
    val chartPoints = remember(weights) { weights.takeLast(12) }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(chartPoints) {
        if (chartPoints.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = chartPoints.indices.map { it.toDouble() },
                        y = chartPoints.map { it.weightValue.toDouble() },
                    )
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WeightSummaryCard(weights)
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Динамика веса", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Вес — важнейший индикатор здоровья кота",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (chartPoints.size < 2) {
                        Text(
                            "Для графика нужно минимум два взвешивания. Добавьте их кнопкой «Взвесить» на главном экране.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        val labels = remember(chartPoints) {
                            chartPoints.map {
                                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(ShortDateFormatter)
                            }
                        }
                        ProvideVicoTheme(rememberM3VicoTheme()) {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberLineCartesianLayer(),
                                    startAxis = VerticalAxis.rememberStart(),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                                            labels.getOrNull(value.toInt()).orEmpty()
                                        },
                                    ),
                                ),
                                modelProducer = modelProducer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                        }
                    }
                }
            }
        }
        item { Text("История", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
        if (weights.isEmpty()) {
            item { EmptyHint("Взвешиваний пока нет.") }
        }
        items(weights.asReversed(), key = { it.id }) { record ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${formatWeight(record.weightValue)} кг", style = MaterialTheme.typography.titleMedium)
                        Text(
                            Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(FullDateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(record) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSummaryCard(weights: List<WeightRecordEntity>) {
    val latest = weights.lastOrNull()
    val previous = weights.getOrNull(weights.size - 2)
    AppCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Текущий вес", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    latest?.let { "${formatWeight(it.weightValue)} кг" } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (latest != null && previous != null) {
                val delta = latest.weightValue - previous.weightValue
                val sign = if (delta > 0.004f) "+" else if (delta < -0.004f) "−" else ""
                Text(
                    "$sign${String.format(RuLocale, "%.2f", kotlin.math.abs(delta))} кг",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// =============================================================== Вкладка 3: хроника

@Composable
private fun DiaryTab(
    notes: List<DiaryNoteEntity>,
    onAdd: (String, List<String>, Long) -> Unit,
    onDelete: (DiaryNoteEntity) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (notes.isEmpty()) {
                item { EmptyHint("Здесь можно фиксировать симптомы и наблюдения: «Вялый», «Срыгнул шерстью» и т. д.") }
            }
            items(notes, key = { it.id }) { note -> NoteCard(note) { onDelete(note) } }
        }
        ExtendedFloatingActionButton(
            onClick = { showSheet = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Заметка") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
    if (showSheet) {
        AddNoteSheet(
            onDismiss = { showSheet = false },
            onSave = { text, tags, ts ->
                onAdd(text, tags, ts)
                showSheet = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(note: DiaryNoteEntity, onDelete: () -> Unit) {
    val date = Instant.ofEpochMilli(note.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val tags = note.tags.split("|").filter { it.isNotBlank() }
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    date.format(FullDateFormatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(note.symptomText, style = MaterialTheme.typography.bodyLarge)
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag ->
                            Text(
                                tag,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddNoteSheet(onDismiss: () -> Unit, onSave: (String, List<String>, Long) -> Unit) {
    var text by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

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
            Text("Новая заметка", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = { Text("Что произошло?") },
                isError = showError,
                supportingText = { if (showError) Text("Опишите наблюдение или выберите тег") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text("Состояние", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SYMPTOM_TAGS.forEach { tag ->
                    FilterChip(
                        selected = tag in selected,
                        onClick = {
                            selected = if (tag in selected) selected - tag else selected + tag
                            showError = false
                        },
                        label = { Text(tag) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Дата: ${date.format(FullDateFormatter)}")
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (text.isBlank() && selected.isEmpty()) {
                        showError = true
                    } else {
                        // Привязываем заметку к выбранной дате (время — текущее, чтобы записи за день сортировались по порядку добавления).
                        val now = java.time.LocalTime.now()
                        val ts = date.atTime(now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val body = text.ifBlank { selected.joinToString(", ") }
                        onSave(body, SYMPTOM_TAGS.filter { it in selected }, ts)
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
            initial = date,
            onDismiss = { showDatePicker = false },
            onPick = { date = if (it.isAfter(LocalDate.now())) LocalDate.now() else it },
        )
    }
}
