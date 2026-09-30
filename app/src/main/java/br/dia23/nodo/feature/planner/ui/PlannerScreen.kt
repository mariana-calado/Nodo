package br.dia23.nodo.feature.planner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.ui.ColorDot
import br.dia23.nodo.core.ui.TopLevelScreenInsets
import br.dia23.nodo.core.ui.formatDuration
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.GoalType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.WeeklyGoalEntity
import br.dia23.nodo.feature.planner.domain.DailySuggestion
import br.dia23.nodo.feature.planner.domain.ExamPrep
import br.dia23.nodo.feature.planner.domain.GoalProgress
import br.dia23.nodo.ui.theme.NodoTheme
import br.dia23.nodo.ui.theme.subjectColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlannerRoute(viewModel: PlannerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Lembretes viram notificações: no Android 13+ pedimos a permissão quando um lembrete é ligado.
    // O pedido não bloqueia nada: a configuração é salva de qualquer forma.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val askNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PlannerScreen(
        uiState = uiState,
        actions = PlannerActions(
            onAddGoal = viewModel::onAddGoal,
            onEditGoal = viewModel::onEditGoal,
            onAddEvent = viewModel::onAddEvent,
            onEditEvent = viewModel::onEditEvent,
            onToggleDone = viewModel::onToggleDone,
            onOpenReminders = viewModel::onOpenReminders,
            onDismissDialog = viewModel::onDismissDialog,
            onSaveGoal = viewModel::onSaveGoal,
            onDeleteGoal = viewModel::onDeleteGoal,
            onSaveEvent = { title, type, date, subjectId, reminder ->
                viewModel.onSaveEvent(title, type, date, subjectId, reminder)
                if (reminder != null) askNotificationPermission()
            },
            onDeleteEvent = viewModel::onDeleteEvent,
            onSaveReminders = { enabled, minuteOfDay ->
                viewModel.onSaveReminders(enabled, minuteOfDay)
                if (enabled) askNotificationPermission()
            },
            onCreateSubject = viewModel::onCreateSubject,
        ),
    )
}

/**
 * Todas as ações da tela num objeto só. Com muitos eventos, isso evita uma lista enorme de parâmetros
 * repetida em cada função (e nas previews).
 */
data class PlannerActions(
    val onAddGoal: () -> Unit = {},
    val onEditGoal: (WeeklyGoalEntity) -> Unit = {},
    val onAddEvent: () -> Unit = {},
    val onEditEvent: (PlannerEventEntity) -> Unit = {},
    val onToggleDone: (PlannerEventEntity) -> Unit = {},
    val onOpenReminders: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onSaveGoal: (type: GoalType, target: Int, subjectId: String?) -> Unit = { _, _, _ -> },
    val onDeleteGoal: () -> Unit = {},
    val onSaveEvent: (title: String, type: EventType, date: LocalDate, subjectId: String?, reminderDaysBefore: Int?) -> Unit =
        { _, _, _, _, _ -> },
    val onDeleteEvent: () -> Unit = {},
    val onSaveReminders: (enabled: Boolean, minuteOfDay: Int) -> Unit = { _, _ -> },
    val onCreateSubject: (name: String, colorIndex: Int, onCreated: (String) -> Unit) -> Unit = { _, _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(uiState: PlannerUiState, actions: PlannerActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planner_title)) },
                actions = {
                    IconButton(onClick = actions.onOpenReminders) {
                        Icon(
                            painterResource(R.drawable.ic_notifications),
                            contentDescription = stringResource(R.string.planner_reminders),
                        )
                    }
                },
            )
        },
        contentWindowInsets = TopLevelScreenInsets,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val subjectsById = uiState.subjects.associateBy { it.id }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TodayCard(uiState.suggestion) }

            item {
                SectionHeader(stringResource(R.string.planner_goals), stringResource(R.string.planner_add_goal), actions.onAddGoal)
            }
            if (uiState.goals.isEmpty()) {
                item { EmptyHint(stringResource(R.string.planner_goals_empty)) }
            }
            items(uiState.goals, key = { it.goal.id }) { progress ->
                GoalRow(progress, progress.goal.subjectId?.let(subjectsById::get), onClick = { actions.onEditGoal(progress.goal) })
            }

            item {
                SectionHeader(stringResource(R.string.planner_events), stringResource(R.string.planner_add_event), actions.onAddEvent)
            }
            if (uiState.events.isEmpty()) {
                item { EmptyHint(stringResource(R.string.planner_events_empty)) }
            }
            items(uiState.events, key = { it.id }) { event ->
                EventRow(
                    event = event,
                    subject = event.subjectId?.let(subjectsById::get),
                    today = uiState.today,
                    onClick = { actions.onEditEvent(event) },
                    onToggleDone = { actions.onToggleDone(event) },
                )
            }
        }
    }

    PlannerDialogs(uiState, actions)
}

@Composable
private fun TodayCard(suggestion: DailySuggestion) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.planner_today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (suggestion.total == 0) {
                    stringResource(R.string.planner_nothing_today)
                } else {
                    pluralStringResource(R.plurals.planner_suggestion_total, suggestion.total, suggestion.total)
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (suggestion.examPreps.isNotEmpty()) {
                if (suggestion.dueToday > 0) {
                    Text(
                        pluralStringResource(R.plurals.planner_due_today, suggestion.dueToday, suggestion.dueToday),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                suggestion.examPreps.forEach { prep -> ExamPrepLine(prep) }
                Text(
                    stringResource(R.string.planner_exam_prep_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExamPrepLine(prep: ExamPrep) {
    Text(
        stringResource(R.string.planner_exam_prep, prep.perDay, prep.title, countdownText(prep.daysLeft)),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SectionHeader(title: String, addLabel: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = addLabel)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GoalRow(progress: GoalProgress, subject: SubjectEntity?, onClick: () -> Unit) {
    val goal = progress.goal
    val title = when {
        goal.type == GoalType.REVIEWS -> stringResource(R.string.goal_reviews)
        subject != null -> stringResource(R.string.goal_focus_subject, subject.name)
        else -> stringResource(R.string.goal_focus_total)
    }
    val progressText = when (goal.type) {
        GoalType.FOCUS_MINUTES -> stringResource(
            R.string.goal_progress_of,
            formatDuration(progress.current * 60_000L),
            formatDuration(goal.target * 60_000L),
        )
        GoalType.REVIEWS -> stringResource(
            R.string.goal_progress_of,
            progress.current.toString(),
            pluralStringResource(R.plurals.goal_reviews_count, goal.target, goal.target),
        )
    }
    val footer = when {
        progress.isDone -> stringResource(R.string.goal_done)
        goal.type == GoalType.FOCUS_MINUTES ->
            stringResource(R.string.goal_per_day, formatDuration(progress.perDayToFinish * 60_000L))
        else -> stringResource(
            R.string.goal_per_day,
            pluralStringResource(R.plurals.goal_reviews_count, progress.perDayToFinish, progress.perDayToFinish),
        )
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(progressText, style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { progress.fraction },
                color = subject?.colorIndex?.let(::subjectColor) ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                footer,
                style = MaterialTheme.typography.labelMedium,
                color = if (progress.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventRow(
    event: PlannerEventEntity,
    subject: SubjectEntity?,
    today: LocalDate,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
) {
    val date = LocalDate.ofEpochDay(event.dateEpochDay)
    val daysLeft = (event.dateEpochDay - today.toEpochDay()).toInt()
    val isOverdue = daysLeft < 0 && !event.isDone

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Folhinha" com dia e mês.
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge)
                Text(
                    date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).removeSuffix("."),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Prazo entregue fica riscado.
                    textDecoration = if (event.isDone) TextDecoration.LineThrough else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subject != null) {
                        ColorDot(subject.colorIndex)
                        Text(
                            subject.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                    }
                    Text(
                        "${stringResource(event.type.labelRes())} · ${countdownText(daysLeft)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (event.reminderDaysBefore != null) {
                Icon(
                    painterResource(R.drawable.ic_notifications),
                    contentDescription = stringResource(R.string.event_has_reminder),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (event.type == EventType.DEADLINE) {
                Checkbox(checked = event.isDone, onCheckedChange = { onToggleDone() })
            } else {
                Box(Modifier.width(12.dp))
            }
        }
    }
}

/** "hoje", "amanhã", "em 5 dias" ou "atrasado há 2 dias". */
@Composable
fun countdownText(daysLeft: Int): String = when {
    daysLeft < 0 -> pluralStringResource(R.plurals.countdown_overdue, -daysLeft, -daysLeft)
    daysLeft == 0 -> stringResource(R.string.countdown_today)
    daysLeft == 1 -> stringResource(R.string.countdown_tomorrow)
    else -> pluralStringResource(R.plurals.countdown_in_days, daysLeft, daysLeft)
}

fun EventType.labelRes(): Int = if (this == EventType.EXAM) R.string.event_type_exam else R.string.event_type_deadline

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PlannerScreenPreview() {
    val today = LocalDate.of(2026, 9, 30)
    val calculo = SubjectEntity(id = "calc", name = "Cálculo", colorIndex = 0)
    NodoTheme {
        PlannerScreen(
            uiState = PlannerUiState(
                isLoading = false,
                today = today,
                suggestion = DailySuggestion(
                    dueToday = 18,
                    examPreps = listOf(ExamPrep("e", "P1 de Cálculo", "calc", daysLeft = 5, cardsAhead = 42, perDay = 9)),
                ),
                goals = listOf(
                    GoalProgress(WeeklyGoalEntity(type = GoalType.FOCUS_MINUTES, target = 600, subjectId = "calc"), 200, 5),
                    GoalProgress(WeeklyGoalEntity(type = GoalType.REVIEWS, target = 150), 160, 5),
                ),
                events = listOf(
                    PlannerEventEntity(
                        title = "P1 de Cálculo", type = EventType.EXAM, dateEpochDay = today.plusDays(5).toEpochDay(),
                        subjectId = "calc", reminderDaysBefore = 1,
                    ),
                    PlannerEventEntity(title = "Relatório de física", type = EventType.DEADLINE, dateEpochDay = today.minusDays(1).toEpochDay()),
                ),
                subjects = listOf(calculo),
            ),
            actions = PlannerActions(),
        )
    }
}
