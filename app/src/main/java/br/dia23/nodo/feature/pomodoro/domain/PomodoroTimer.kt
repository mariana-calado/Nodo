package br.dia23.nodo.feature.pomodoro.domain

enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    /** A cada N focos concluídos, a pausa é longa. */
    val focusesBeforeLongBreak: Int = 4,
) {
    fun durationMs(phase: PomodoroPhase): Long = MINUTE_MS * when (phase) {
        PomodoroPhase.FOCUS -> focusMinutes
        PomodoroPhase.SHORT_BREAK -> shortBreakMinutes
        PomodoroPhase.LONG_BREAK -> longBreakMinutes
    }
}

/**
 * Estado do timer guardado como HORÁRIOS (timestamps), não como "segundos restantes".
 *
 * Um contador que diminui a cada segundo para quando o app fecha. Guardando "quando começou" e
 * "quanto já tinha corrido antes da última pausa", o tempo restante é sempre calculável
 * comparando com o relógio, mesmo que o app tenha ficado horas fechado.
 */
data class TimerState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val durationMs: Long = PomodoroSettings().durationMs(PomodoroPhase.FOCUS),
    /** Quando a fase foi iniciada pela primeira vez; null = ainda não começou. */
    val startedAt: Long? = null,
    /** Momento do último "Iniciar/Retomar"; null = parado ou pausado. */
    val runningSince: Long? = null,
    /** Tempo corrido antes da última retomada (soma dos trechos entre pausas). */
    val elapsedBeforeMs: Long = 0,
    val subjectId: String? = null,
    /** Focos concluídos no ciclo atual (define quando vem a pausa longa). */
    val completedFocuses: Int = 0,
) {
    val isStarted: Boolean get() = startedAt != null
    val isRunning: Boolean get() = runningSince != null

    fun elapsedMs(now: Long): Long =
        (elapsedBeforeMs + (runningSince?.let { now - it } ?: 0)).coerceIn(0, durationMs)

    fun remainingMs(now: Long): Long = durationMs - elapsedMs(now)

    fun isFinished(now: Long): Boolean = isRunning && remainingMs(now) <= 0

    /** Horário em que a fase termina se continuar rodando (para o alarme); null se não está rodando. */
    fun endsAt(): Long? = runningSince?.let { it + durationMs - elapsedBeforeMs }
}

/** Uma sessão de foco pronta para ser salva no banco. */
data class FocusRecord(
    val subjectId: String?,
    val startedAt: Long,
    val endedAt: Long,
    val focusedMs: Long,
    val plannedMs: Long,
    /** false quando o foco foi interrompido antes do fim ("Parar"). */
    val completed: Boolean,
)

/**
 * Transições do Pomodoro como funções puras: recebem o estado atual e o horário, devolvem o novo estado
 * (e, quando for o caso, a sessão de foco a salvar). Nada de banco, relógio ou Android: testável em JVM.
 */
object PomodoroTimer {

    /** Focos interrompidos com menos que isso não entram nas estatísticas. */
    const val MIN_PARTIAL_FOCUS_MS = MINUTE_MS

    fun idle(phase: PomodoroPhase, settings: PomodoroSettings, subjectId: String?, completedFocuses: Int) =
        TimerState(
            phase = phase,
            durationMs = settings.durationMs(phase),
            subjectId = subjectId,
            completedFocuses = completedFocuses,
        )

    /** Iniciar ou retomar. */
    fun start(state: TimerState, now: Long): TimerState =
        if (state.isRunning) state else state.copy(startedAt = state.startedAt ?: now, runningSince = now)

    fun pause(state: TimerState, now: Long): TimerState =
        if (!state.isRunning) state else state.copy(elapsedBeforeMs = state.elapsedMs(now), runningSince = null)

    /** Troca de matéria só antes de começar (senão o tempo já corrido iria para a matéria errada). */
    fun selectSubject(state: TimerState, subjectId: String?): TimerState =
        if (state.isStarted) state else state.copy(subjectId = subjectId)

    /** Novas configurações valem para a fase atual apenas se ela ainda não começou. */
    fun applySettings(state: TimerState, settings: PomodoroSettings): TimerState =
        if (state.isStarted) state else state.copy(durationMs = settings.durationMs(state.phase))

    /**
     * A fase chegou ao fim. Se era foco, gera o registro completo.
     * A próxima fase fica pronta, mas parada: a pessoa decide quando começar a pausa.
     */
    fun finish(state: TimerState, settings: PomodoroSettings, now: Long): Pair<TimerState, FocusRecord?> {
        val record = if (state.phase == PomodoroPhase.FOCUS && state.startedAt != null) {
            FocusRecord(
                subjectId = state.subjectId,
                startedAt = state.startedAt,
                // Horário teórico do fim: se o app só percebeu o término depois, a sessão não "estica".
                endedAt = state.endsAt() ?: now,
                focusedMs = state.durationMs,
                plannedMs = state.durationMs,
                completed = true,
            )
        } else {
            null
        }

        val (nextPhase, nextCompleted) = when (state.phase) {
            PomodoroPhase.FOCUS -> {
                val done = state.completedFocuses + 1
                val phase = if (done >= settings.focusesBeforeLongBreak) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
                phase to done
            }
            PomodoroPhase.SHORT_BREAK -> PomodoroPhase.FOCUS to state.completedFocuses
            PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS to 0 // pausa longa encerra o ciclo
        }
        return idle(nextPhase, settings, state.subjectId, nextCompleted) to record
    }

    /**
     * "Parar": volta a fase atual para o início. Foco interrompido com pelo menos 1 minuto
     * é salvo como sessão incompleta (o tempo estudado conta nas estatísticas).
     */
    fun stop(state: TimerState, settings: PomodoroSettings, now: Long): Pair<TimerState, FocusRecord?> {
        val focusedMs = state.elapsedMs(now)
        val record = if (
            state.phase == PomodoroPhase.FOCUS && state.startedAt != null && focusedMs >= MIN_PARTIAL_FOCUS_MS
        ) {
            FocusRecord(
                subjectId = state.subjectId,
                startedAt = state.startedAt,
                endedAt = now,
                focusedMs = focusedMs,
                plannedMs = state.durationMs,
                completed = false,
            )
        } else {
            null
        }
        return idle(state.phase, settings, state.subjectId, state.completedFocuses) to record
    }

    /** Pular a pausa e voltar direto ao foco. Não existe "pular foco": para isso há o "Parar". */
    fun skipBreak(state: TimerState, settings: PomodoroSettings): TimerState = when (state.phase) {
        PomodoroPhase.FOCUS -> state
        PomodoroPhase.SHORT_BREAK -> idle(PomodoroPhase.FOCUS, settings, state.subjectId, state.completedFocuses)
        PomodoroPhase.LONG_BREAK -> idle(PomodoroPhase.FOCUS, settings, state.subjectId, 0)
    }
}

private const val MINUTE_MS = 60_000L
