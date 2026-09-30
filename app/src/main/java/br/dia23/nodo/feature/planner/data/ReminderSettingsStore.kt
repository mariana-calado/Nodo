package br.dia23.nodo.feature.planner.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import br.dia23.nodo.core.di.PlannerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderSettings(
    val dailyEnabled: Boolean = false,
    /** Horário do lembrete diário em minutos desde a meia-noite (19h = 1140). */
    val dailyMinuteOfDay: Int = 19 * 60,
) {
    val dailyTime: LocalTime get() = LocalTime.of(dailyMinuteOfDay / 60, dailyMinuteOfDay % 60)
}

@Singleton
class ReminderSettingsStore @Inject constructor(
    @param:PlannerPreferences private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        val defaults = ReminderSettings()
        ReminderSettings(
            dailyEnabled = prefs[DAILY_ENABLED] ?: defaults.dailyEnabled,
            dailyMinuteOfDay = prefs[DAILY_MINUTE_OF_DAY] ?: defaults.dailyMinuteOfDay,
        )
    }

    suspend fun save(settings: ReminderSettings) {
        dataStore.edit { prefs ->
            prefs[DAILY_ENABLED] = settings.dailyEnabled
            prefs[DAILY_MINUTE_OF_DAY] = settings.dailyMinuteOfDay
        }
    }

    private companion object {
        val DAILY_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_MINUTE_OF_DAY = intPreferencesKey("daily_reminder_minute_of_day")
    }
}
