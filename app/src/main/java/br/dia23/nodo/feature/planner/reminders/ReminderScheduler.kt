package br.dia23.nodo.feature.planner.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.dia23.nodo.core.di.ApplicationScope
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.ReminderSettings
import br.dia23.nodo.feature.planner.data.ReminderSettingsStore
import br.dia23.nodo.feature.planner.domain.ReminderTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agenda os lembretes no WorkManager.
 *
 * "Trabalho único" (unique work): cada lembrete tem um nome fixo ("daily_reminder", "event_<id>").
 * Agendar de novo com o mesmo nome SUBSTITUI o anterior, então editar um evento nunca gera avisos duplicados.
 * O WorkManager guarda os agendamentos em disco: eles sobrevivem ao app fechado e ao celular reiniciado.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsStore: ReminderSettingsStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Chamado ao abrir o app: garante que o lembrete diário está agendado (sem mexer se já estiver). */
    fun restoreDaily() {
        scope.launch {
            val settings = settingsStore.settings.first()
            if (settings.dailyEnabled) scheduleDaily(settings, ExistingWorkPolicy.KEEP)
        }
    }

    suspend fun updateDaily(settings: ReminderSettings) {
        settingsStore.save(settings)
        if (settings.dailyEnabled) {
            scheduleDaily(settings, ExistingWorkPolicy.REPLACE)
        } else {
            workManager.cancelUniqueWork(DAILY_WORK)
        }
    }

    /**
     * O WorkManager não tem "todo dia às 19h" exato: agendamos UMA execução para o próximo 19h,
     * e o próprio Worker agenda a do dia seguinte ao terminar (ver DailyReminderWorker).
     */
    fun scheduleDaily(settings: ReminderSettings, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now()
        val next = ReminderTimes.nextOccurrence(now, settings.dailyTime)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(millisBetween(now, next), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(DAILY_WORK, policy, request)
    }

    /** (Re)agenda o aviso de uma prova/prazo, ou cancela se não fizer mais sentido. */
    fun scheduleEvent(event: PlannerEventEntity) {
        val daysBefore = event.reminderDaysBefore
        if (daysBefore == null || event.isDone || event.isDeleted) {
            cancelEvent(event.id)
            return
        }
        val now = ZonedDateTime.now()
        val remindAt = ReminderTimes.eventReminderAt(event.dateEpochDay, daysBefore, ZoneId.systemDefault())
        if (!remindAt.isAfter(now)) { // o horário do aviso já passou
            cancelEvent(event.id)
            return
        }
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(millisBetween(now, remindAt), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(EventReminderWorker.KEY_EVENT_ID to event.id))
            .build()
        workManager.enqueueUniqueWork(eventWorkName(event.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelEvent(eventId: String) {
        workManager.cancelUniqueWork(eventWorkName(eventId))
    }

    private fun millisBetween(from: ZonedDateTime, to: ZonedDateTime): Long =
        to.toInstant().toEpochMilli() - from.toInstant().toEpochMilli()

    private fun eventWorkName(eventId: String) = "event_reminder_$eventId"

    companion object {
        const val DAILY_WORK = "daily_reminder"
    }
}
