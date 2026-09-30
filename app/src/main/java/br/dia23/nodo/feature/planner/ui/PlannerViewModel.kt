package br.dia23.nodo.feature.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.subjects.SubjectRepository
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.GoalType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.PlannerRepository
import br.dia23.nodo.feature.planner.data.ReminderSettings
import br.dia23.nodo.feature.planner.data.ReminderSettingsStore
import br.dia23.nodo.feature.planner.data.WeeklyGoalEntity
import br.dia23.nodo.feature.planner.domain.DailySuggestion
import br.dia23.nodo.feature.planner.domain.GoalProgress
import br.dia23.nodo.feature.planner.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Diálogo aberto. `null` dentro de Goal/Event significa "criar novo". */
sealed interface PlannerDialog {
    data object None : PlannerDialog
    data class Goal(val goal: WeeklyGoalEntity?) : PlannerDialog
    data class Event(val event: PlannerEventEntity?) : PlannerDialog
    data object Reminders : PlannerDialog
}

data class PlannerUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val suggestion: DailySuggestion = DailySuggestion(dueToday = 0, examPreps = emptyList()),
    val goals: List<GoalProgress> = emptyList(),
    val events: List<PlannerEventEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val dialog: PlannerDialog = PlannerDialog.None,
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repository: PlannerRepository,
    private val subjectRepository: SubjectRepository,
    settingsStore: ReminderSettingsStore,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val dialog = MutableStateFlow<PlannerDialog>(PlannerDialog.None)

    // `combine` com tipos aceita no máximo 5 fluxos: juntamos os dados primeiro e o diálogo depois.
    private val data = combine(
        repository.observeDailySuggestion(),
        repository.observeGoalProgress(),
        repository.observeEvents(),
        subjectRepository.observeSubjects(),
        settingsStore.settings,
    ) { suggestion, goals, events, subjects, settings ->
        PlannerUiState(
            isLoading = false,
            today = LocalDate.now(),
            suggestion = suggestion,
            goals = goals,
            events = events,
            subjects = subjects,
            reminderSettings = settings,
        )
    }

    val uiState: StateFlow<PlannerUiState> = combine(data, dialog) { state, dialog -> state.copy(dialog = dialog) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    // --- Abrir e fechar diálogos ---

    fun onAddGoal() = dialog.update { PlannerDialog.Goal(null) }

    fun onEditGoal(goal: WeeklyGoalEntity) = dialog.update { PlannerDialog.Goal(goal) }

    fun onAddEvent() = dialog.update { PlannerDialog.Event(null) }

    fun onEditEvent(event: PlannerEventEntity) = dialog.update { PlannerDialog.Event(event) }

    fun onOpenReminders() = dialog.update { PlannerDialog.Reminders }

    fun onDismissDialog() = dialog.update { PlannerDialog.None }

    // --- Metas ---

    fun onSaveGoal(type: GoalType, target: Int, subjectId: String?) {
        val existing = (dialog.value as? PlannerDialog.Goal)?.goal
        dialog.update { PlannerDialog.None }
        viewModelScope.launch {
            val base = existing ?: WeeklyGoalEntity(type = type, target = target)
            // Matéria só faz sentido em meta de foco (revisões contam todos os decks).
            val newSubject = if (type == GoalType.FOCUS_MINUTES) subjectId else null
            repository.saveGoal(base.copy(type = type, target = target, subjectId = newSubject))
        }
    }

    fun onDeleteGoal() {
        val goal = (dialog.value as? PlannerDialog.Goal)?.goal ?: return
        dialog.update { PlannerDialog.None }
        viewModelScope.launch { repository.deleteGoal(goal.id) }
    }

    // --- Provas e prazos ---

    fun onSaveEvent(title: String, type: EventType, date: LocalDate, subjectId: String?, reminderDaysBefore: Int?) {
        if (title.isBlank()) return
        val existing = (dialog.value as? PlannerDialog.Event)?.event
        dialog.update { PlannerDialog.None }
        viewModelScope.launch {
            val base = existing ?: PlannerEventEntity(title = title, type = type, dateEpochDay = date.toEpochDay())
            repository.saveEvent(
                base.copy(
                    title = title,
                    type = type,
                    dateEpochDay = date.toEpochDay(),
                    subjectId = subjectId,
                    reminderDaysBefore = reminderDaysBefore,
                ),
            )
        }
    }

    fun onDeleteEvent() {
        val event = (dialog.value as? PlannerDialog.Event)?.event ?: return
        dialog.update { PlannerDialog.None }
        viewModelScope.launch { repository.deleteEvent(event.id) }
    }

    fun onToggleDone(event: PlannerEventEntity) {
        viewModelScope.launch { repository.setEventDone(event.id, !event.isDone) }
    }

    // --- Lembretes e matérias ---

    fun onSaveReminders(enabled: Boolean, minuteOfDay: Int) {
        dialog.update { PlannerDialog.None }
        viewModelScope.launch { reminderScheduler.updateDaily(ReminderSettings(enabled, minuteOfDay)) }
    }

    fun onCreateSubject(name: String, colorIndex: Int, onCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch { onCreated(subjectRepository.createSubject(name, colorIndex).id) }
    }
}
