package br.dia23.nodo.feature.pomodoro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import br.dia23.nodo.feature.pomodoro.domain.PomodoroPhase
import br.dia23.nodo.feature.pomodoro.domain.PomodoroSettings
import br.dia23.nodo.feature.pomodoro.domain.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda o estado do timer e as configurações no DataStore (arquivo chave-valor do app).
 *
 * Por que não no Room? Não é um "dado do usuário" para histórico ou sincronização: é o estado
 * momentâneo da tela, um registro só, que precisa sobreviver ao app ser fechado.
 */
@Singleton
class TimerStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<PomodoroSettings> = dataStore.data.map { it.toSettings() }

    val state: Flow<TimerState> = dataStore.data.map { prefs ->
        val settings = prefs.toSettings()
        val phase = prefs[Keys.PHASE]?.let { runCatching { PomodoroPhase.valueOf(it) }.getOrNull() }
            ?: PomodoroPhase.FOCUS
        TimerState(
            phase = phase,
            durationMs = prefs[Keys.DURATION_MS] ?: settings.durationMs(phase),
            startedAt = prefs[Keys.STARTED_AT],
            runningSince = prefs[Keys.RUNNING_SINCE],
            elapsedBeforeMs = prefs[Keys.ELAPSED_BEFORE_MS] ?: 0,
            subjectId = prefs[Keys.SUBJECT_ID],
            completedFocuses = prefs[Keys.COMPLETED_FOCUSES] ?: 0,
        )
    }

    suspend fun save(state: TimerState) {
        dataStore.edit { prefs ->
            prefs[Keys.PHASE] = state.phase.name
            prefs[Keys.DURATION_MS] = state.durationMs
            prefs.setOrRemove(Keys.STARTED_AT, state.startedAt)
            prefs.setOrRemove(Keys.RUNNING_SINCE, state.runningSince)
            prefs[Keys.ELAPSED_BEFORE_MS] = state.elapsedBeforeMs
            prefs.setOrRemove(Keys.SUBJECT_ID, state.subjectId)
            prefs[Keys.COMPLETED_FOCUSES] = state.completedFocuses
        }
    }

    suspend fun saveSettings(settings: PomodoroSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.FOCUS_MINUTES] = settings.focusMinutes
            prefs[Keys.SHORT_BREAK_MINUTES] = settings.shortBreakMinutes
            prefs[Keys.LONG_BREAK_MINUTES] = settings.longBreakMinutes
        }
    }

    private fun Preferences.toSettings(): PomodoroSettings {
        val defaults = PomodoroSettings()
        return PomodoroSettings(
            focusMinutes = this[Keys.FOCUS_MINUTES] ?: defaults.focusMinutes,
            shortBreakMinutes = this[Keys.SHORT_BREAK_MINUTES] ?: defaults.shortBreakMinutes,
            longBreakMinutes = this[Keys.LONG_BREAK_MINUTES] ?: defaults.longBreakMinutes,
        )
    }

    /** DataStore não guarda null: a ausência da chave representa o null. */
    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private object Keys {
        val PHASE = stringPreferencesKey("phase")
        val DURATION_MS = longPreferencesKey("duration_ms")
        val STARTED_AT = longPreferencesKey("started_at")
        val RUNNING_SINCE = longPreferencesKey("running_since")
        val ELAPSED_BEFORE_MS = longPreferencesKey("elapsed_before_ms")
        val SUBJECT_ID = stringPreferencesKey("subject_id")
        val COMPLETED_FOCUSES = intPreferencesKey("completed_focuses")
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
    }
}
