package br.dia23.nodo.feature.planner.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import br.dia23.nodo.feature.planner.data.PlannerRepository
import br.dia23.nodo.feature.planner.data.ReminderSettingsStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/*
 * Workers com Hilt: @HiltWorker + @AssistedInject.
 * "Assisted" = parte dos parâmetros vem do WorkManager na hora (Context e WorkerParameters)
 * e o resto vem do Hilt (repositórios). CoroutineWorker permite usar funções suspend no doWork().
 */

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val plannerRepository: PlannerRepository,
    private val settingsStore: ReminderSettingsStore,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsStore.settings.first()
        if (!settings.dailyEnabled) return Result.success()

        val suggestion = plannerRepository.observeDailySuggestion().first()
        // Nada para revisar: não incomoda ninguém à toa.
        if (suggestion.total > 0) notifier.showDailyReminder(suggestion.total)

        // Agenda o de amanhã. APPEND_OR_REPLACE: como ESTE trabalho ainda está rodando, o próximo entra
        // na fila depois dele (REPLACE cancelaria o trabalho atual no meio).
        scheduler.scheduleDaily(settings, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}

@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val plannerRepository: PlannerRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.success()
        val event = plannerRepository.getEvent(eventId) ?: return Result.success()
        // O evento pode ter sido excluído ou entregue depois do agendamento.
        if (event.isDeleted || event.isDone) return Result.success()

        val daysLeft = (event.dateEpochDay - LocalDate.now().toEpochDay()).toInt()
        notifier.showEventReminder(event, daysLeft)
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
    }
}
