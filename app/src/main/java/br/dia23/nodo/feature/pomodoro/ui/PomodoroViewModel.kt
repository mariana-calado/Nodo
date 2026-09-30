package br.dia23.nodo.feature.pomodoro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.subjects.SubjectRepository
import br.dia23.nodo.feature.pomodoro.PomodoroController
import br.dia23.nodo.feature.pomodoro.data.PomodoroRepository
import br.dia23.nodo.feature.pomodoro.domain.PomodoroPhase
import br.dia23.nodo.feature.pomodoro.domain.PomodoroSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PomodoroUiState(
    val isLoading: Boolean = true,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val remainingMs: Long = 0,
    val durationMs: Long = 1,
    val isStarted: Boolean = false,
    val isRunning: Boolean = false,
    val completedFocuses: Int = 0,
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubject: SubjectEntity? = null,
    val settings: PomodoroSettings = PomodoroSettings(),
    val focusedTodayMs: Long = 0,
) {
    /** Fração que ainda falta (1 = cheio, 0 = acabou): o anel "esvazia" com o tempo. */
    val remainingFraction: Float get() = (remainingMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val controller: PomodoroController,
    private val repository: PomodoroRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    /**
     * Relógio da tela: emite o horário atual a cada virada de segundo.
     * Só roda enquanto a tela observa (WhileSubscribed abaixo): com a tela fechada, nada fica "tiquetaqueando".
     * O tempo restante é sempre calculado a partir dos horários salvos, nunca contado aqui.
     */
    private val clock = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(1_000 - now % 1_000)
        }
    }

    val uiState: StateFlow<PomodoroUiState> = combine(
        controller.state.filterNotNull(),
        controller.settings.filterNotNull(),
        subjectRepository.observeSubjects(),
        repository.observeFocusedToday(),
        clock,
    ) { state, settings, subjects, focusedToday, now ->
        PomodoroUiState(
            isLoading = false,
            phase = state.phase,
            remainingMs = state.remainingMs(now),
            durationMs = state.durationMs,
            isStarted = state.isStarted,
            isRunning = state.isRunning,
            completedFocuses = state.completedFocuses,
            subjects = subjects,
            selectedSubject = subjects.find { it.id == state.subjectId },
            settings = settings,
            focusedTodayMs = focusedToday,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PomodoroUiState(),
    )

    // As ações vão para o controller, que roda no escopo do app (sobrevivem à saída da tela).
    fun onStartOrResume() = controller.start()

    fun onPause() = controller.pause()

    fun onStop() = controller.stop()

    fun onSkipBreak() = controller.skipBreak()

    fun onSelectSubject(subjectId: String?) = controller.selectSubject(subjectId)

    fun onCreateSubject(name: String, colorIndex: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val subject = subjectRepository.createSubject(name, colorIndex)
            controller.selectSubject(subject.id) // já deixa a matéria nova selecionada
        }
    }

    fun onSaveSettings(settings: PomodoroSettings) = controller.updateSettings(settings)
}
