package br.dia23.nodo.feature.pomodoro

import br.dia23.nodo.core.di.ApplicationScope
import br.dia23.nodo.core.subjects.SubjectRepository
import br.dia23.nodo.feature.pomodoro.data.PomodoroRepository
import br.dia23.nodo.feature.pomodoro.data.TimerStore
import br.dia23.nodo.feature.pomodoro.domain.FocusRecord
import br.dia23.nodo.feature.pomodoro.domain.PomodoroSettings
import br.dia23.nodo.feature.pomodoro.domain.PomodoroTimer
import br.dia23.nodo.feature.pomodoro.domain.TimerState
import br.dia23.nodo.feature.pomodoro.notification.PomodoroAlarmScheduler
import br.dia23.nodo.feature.pomodoro.notification.PomodoroNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O "motor" do Pomodoro no app. Vive enquanto o processo viver (@Singleton + escopo do app),
 * então o timer continua mesmo sem nenhuma tela aberta.
 *
 * Responsabilidades: aplicar as transições puras do PomodoroTimer, persistir o estado,
 * salvar as sessões de foco e manter alarme + notificação em dia.
 */
@Singleton
class PomodoroController @Inject constructor(
    private val timerStore: TimerStore,
    private val repository: PomodoroRepository,
    private val subjectRepository: SubjectRepository,
    private val notifier: PomodoroNotifier,
    private val alarms: PomodoroAlarmScheduler,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /** null até o DataStore entregar o primeiro valor (a tela mostra "carregando"). */
    val state: StateFlow<TimerState?> = timerStore.state.stateIn(scope, SharingStarted.Eagerly, null)
    val settings: StateFlow<PomodoroSettings?> = timerStore.settings.stateIn(scope, SharingStarted.Eagerly, null)

    // Toques na tela e o alarme podem chegar ao mesmo tempo: o Mutex faz as transições acontecerem uma de cada vez.
    private val mutex = Mutex()

    init {
        // Com o processo vivo, termina a fase no segundo exato (o alarme do sistema pode atrasar).
        // collectLatest: se o estado mudar (ex.: pausa), a espera anterior é cancelada.
        scope.launch {
            timerStore.state.collectLatest { state ->
                val endsAt = state.endsAt() ?: return@collectLatest
                var wait = endsAt - System.currentTimeMillis()
                while (wait > 0) {
                    delay(wait)
                    wait = endsAt - System.currentTimeMillis()
                }
                // Em outra coroutine: o finishIfDue muda o estado, e isso cancelaria este bloco no meio.
                scope.launch { finishIfDue() }
            }
        }
    }

    fun start() = transition { state, _, now -> PomodoroTimer.start(state, now) to null }

    fun pause() = transition { state, _, now -> PomodoroTimer.pause(state, now) to null }

    fun stop() = transition { state, settings, now -> PomodoroTimer.stop(state, settings, now) }

    fun skipBreak() = transition { state, settings, _ -> PomodoroTimer.skipBreak(state, settings) to null }

    fun selectSubject(subjectId: String?) =
        transition { state, _, _ -> PomodoroTimer.selectSubject(state, subjectId) to null }

    fun updateSettings(newSettings: PomodoroSettings) {
        scope.launch {
            mutex.withLock {
                timerStore.saveSettings(newSettings)
                timerStore.save(PomodoroTimer.applySettings(timerStore.state.first(), newSettings))
            }
        }
    }

    /** Chamado ao abrir o app: finaliza fases que venceram com o app fechado e refaz alarme/notificação. */
    fun restore() {
        scope.launch {
            finishIfDue()
            mutex.withLock { syncAlarmAndNotification(timerStore.state.first()) }
        }
    }

    /** Se a fase atual já acabou, salva a sessão (se era foco), prepara a próxima fase e avisa. */
    suspend fun finishIfDue() = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = timerStore.state.first()
        if (!current.isFinished(now)) return@withLock

        val (next, record) = PomodoroTimer.finish(current, timerStore.settings.first(), now)
        persist(next, record)
        alarms.cancel()
        notifier.showPhaseFinished(finished = current.phase, next = next.phase)
    }

    private fun transition(
        block: (state: TimerState, settings: PomodoroSettings, now: Long) -> Pair<TimerState, FocusRecord?>,
    ) {
        // Escopo do app, não da tela: a ação termina mesmo se a pessoa sair da tela logo após tocar.
        scope.launch {
            mutex.withLock {
                val (next, record) = block(timerStore.state.first(), timerStore.settings.first(), System.currentTimeMillis())
                persist(next, record)
                syncAlarmAndNotification(next)
            }
        }
    }

    private suspend fun persist(next: TimerState, record: FocusRecord?) {
        record?.let { repository.saveFocusSession(it) }
        timerStore.save(next)
    }

    private suspend fun syncAlarmAndNotification(state: TimerState) {
        val subjectName = state.subjectId?.let { subjectRepository.getSubject(it)?.name }
        val endsAt = state.endsAt()
        when {
            endsAt != null -> {
                alarms.schedule(endsAt)
                notifier.showRunning(state.phase, subjectName, endsAt)
            }
            state.isStarted -> { // pausado
                alarms.cancel()
                notifier.showPaused(state.phase, subjectName, state.remainingMs(System.currentTimeMillis()))
            }
            else -> {
                alarms.cancel()
                notifier.cancelTimer()
            }
        }
    }
}
