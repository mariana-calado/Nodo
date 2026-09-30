package br.dia23.nodo.feature.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.dia23.nodo.R
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.subjects.ui.NewSubjectDialog
import br.dia23.nodo.core.subjects.ui.SubjectPicker
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.GoalType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.ReminderSettings
import br.dia23.nodo.feature.planner.data.WeeklyGoalEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/** Mostra o diálogo aberto no momento (o estado de qual está aberto vem do ViewModel). */
@Composable
fun PlannerDialogs(uiState: PlannerUiState, actions: PlannerActions) {
    when (val dialog = uiState.dialog) {
        PlannerDialog.None -> Unit
        is PlannerDialog.Goal -> GoalDialog(
            goal = dialog.goal,
            subjects = uiState.subjects,
            onDismiss = actions.onDismissDialog,
            onSave = actions.onSaveGoal,
            onDelete = actions.onDeleteGoal.takeIf { dialog.goal != null },
            onCreateSubject = actions.onCreateSubject,
        )
        is PlannerDialog.Event -> EventDialog(
            event = dialog.event,
            subjects = uiState.subjects,
            onDismiss = actions.onDismissDialog,
            onSave = actions.onSaveEvent,
            onDelete = actions.onDeleteEvent.takeIf { dialog.event != null },
            onCreateSubject = actions.onCreateSubject,
        )
        PlannerDialog.Reminders -> ReminderSettingsDialog(
            settings = uiState.reminderSettings,
            onDismiss = actions.onDismissDialog,
            onSave = actions.onSaveReminders,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    goal: WeeklyGoalEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (GoalType, Int, String?) -> Unit,
    onDelete: (() -> Unit)?,
    onCreateSubject: (String, Int, (String) -> Unit) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(goal?.type ?: GoalType.FOCUS_MINUTES) }
    var focusHours by rememberSaveable {
        mutableIntStateOf(goal?.takeIf { it.type == GoalType.FOCUS_MINUTES }?.let { (it.target / 60).coerceAtLeast(1) } ?: 5)
    }
    var reviews by rememberSaveable { mutableIntStateOf(goal?.takeIf { it.type == GoalType.REVIEWS }?.target ?: 100) }
    var subjectId by rememberSaveable { mutableStateOf(goal?.subjectId) }
    var showNewSubject by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (goal == null) R.string.goal_dialog_title_new else R.string.goal_dialog_title_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    GoalType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index, GoalType.entries.size),
                        ) {
                            Text(
                                stringResource(
                                    if (option == GoalType.FOCUS_MINUTES) R.string.goal_type_focus else R.string.goal_type_reviews,
                                ),
                            )
                        }
                    }
                }
                if (type == GoalType.FOCUS_MINUTES) {
                    Text(stringResource(R.string.goal_focus_target, focusHours), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = focusHours.toFloat(),
                        onValueChange = { focusHours = it.roundToInt() },
                        valueRange = 1f..40f,
                        steps = 38, // 1 a 40 de 1 em 1: 38 paradas entre as pontas
                    )
                    SubjectPicker(
                        selectedId = subjectId,
                        subjects = subjects,
                        onSelect = { subjectId = it },
                        onNewSubject = { showNewSubject = true },
                        noneLabel = stringResource(R.string.goal_all_subjects),
                    )
                } else {
                    Text(stringResource(R.string.goal_reviews_target, reviews), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = reviews.toFloat(),
                        onValueChange = { reviews = ((it / 10).roundToInt() * 10).coerceIn(10, 500) },
                        valueRange = 10f..500f,
                        steps = 48, // de 10 em 10
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = if (type == GoalType.FOCUS_MINUTES) focusHours * 60 else reviews
                    onSave(type, target, subjectId)
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { DeleteAndCancelButtons(onDelete, onDismiss) },
    )

    if (showNewSubject) {
        NewSubjectDialog(
            onDismiss = { showNewSubject = false },
            onCreate = { name, color ->
                showNewSubject = false
                onCreateSubject(name, color) { subjectId = it }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(
    event: PlannerEventEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (String, EventType, LocalDate, String?, Int?) -> Unit,
    onDelete: (() -> Unit)?,
    onCreateSubject: (String, Int, (String) -> Unit) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(event?.title.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(event?.type ?: EventType.EXAM) }
    var dateEpochDay by rememberSaveable {
        mutableLongStateOf(event?.dateEpochDay ?: LocalDate.now().plusDays(7).toEpochDay())
    }
    var subjectId by rememberSaveable { mutableStateOf(event?.subjectId) }
    // Evento novo já vem com "1 dia antes"; editando, mantém o que estava (inclusive "sem lembrete").
    var reminder by rememberSaveable { mutableStateOf(if (event == null) 1 else event.reminderDaysBefore) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNewSubject by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (event == null) R.string.planner_add_event else R.string.event_dialog_title_edit))
        },
        text = {
            // Rolável: com o teclado aberto, o conteúdo pode não caber na tela.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EventType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index, EventType.entries.size),
                        ) { Text(stringResource(option.labelRes())) }
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_calendar_month), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        LocalDate.ofEpochDay(dateEpochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    )
                }
                SubjectPicker(
                    selectedId = subjectId,
                    subjects = subjects,
                    onSelect = { subjectId = it },
                    onNewSubject = { showNewSubject = true },
                )
                ReminderPicker(selected = reminder, onSelect = { reminder = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, type, LocalDate.ofEpochDay(dateEpochDay), subjectId, reminder) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { DeleteAndCancelButtons(onDelete, onDismiss) },
    )

    if (showDatePicker) {
        // O DatePicker trabalha com meia-noite em UTC; convertemos de/para LocalDate nessa mesma base
        // para a data não "pular" um dia por causa do fuso.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(dateEpochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showNewSubject) {
        NewSubjectDialog(
            onDismiss = { showNewSubject = false },
            onCreate = { name, color ->
                showNewSubject = false
                onCreateSubject(name, color) { subjectId = it }
            },
        )
    }
}

/** Opções de lembrete: sem, no dia, 1, 3 ou 7 dias antes. */
@Composable
private fun ReminderPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(reminderLabel(selected)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_notifications), contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(null, 0, 1, 3, 7).forEach { option ->
                DropdownMenuItem(
                    text = { Text(reminderLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun reminderLabel(daysBefore: Int?): String = when (daysBefore) {
    null -> stringResource(R.string.event_reminder_none)
    0 -> stringResource(R.string.event_reminder_same_day)
    else -> pluralStringResource(R.plurals.event_reminder_days_before, daysBefore, daysBefore)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSettingsDialog(
    settings: ReminderSettings,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, minuteOfDay: Int) -> Unit,
) {
    var enabled by rememberSaveable { mutableStateOf(settings.dailyEnabled) }
    val timeState = rememberTimePickerState(
        initialHour = settings.dailyMinuteOfDay / 60,
        initialMinute = settings.dailyMinuteOfDay % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.planner_reminders)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reminders_daily),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Text(
                    stringResource(R.string.reminders_daily_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // TimeInput: versão compacta (digitar hora e minuto) do seletor de horário do Material 3.
                if (enabled) TimeInput(state = timeState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(enabled, timeState.hour * 60 + timeState.minute) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** "Excluir" (só ao editar) + "Cancelar", lado a lado no lugar do botão secundário do diálogo. */
@Composable
private fun DeleteAndCancelButtons(onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    Row {
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    }
}
