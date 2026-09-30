package br.dia23.nodo.feature.planner.data

import br.dia23.nodo.feature.flashcards.data.CardDao
import br.dia23.nodo.feature.flashcards.data.ReviewDao
import br.dia23.nodo.feature.planner.domain.DailySuggestion
import br.dia23.nodo.feature.planner.domain.GoalProgress
import br.dia23.nodo.feature.planner.domain.PlannerCalculator
import br.dia23.nodo.feature.planner.reminders.ReminderScheduler
import br.dia23.nodo.feature.pomodoro.data.FocusSessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepository @Inject constructor(
    private val plannerDao: PlannerDao,
    private val cardDao: CardDao,
    private val focusSessionDao: FocusSessionDao,
    private val reviewDao: ReviewDao,
    private val reminderScheduler: ReminderScheduler,
) {
    // `flow { }` em volta: o "hoje" é recalculado cada vez que alguém começa a observar.

    fun observeDailySuggestion(): Flow<DailySuggestion> = flow {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        emitAll(
            combine(cardDao.observeDueInfo(), plannerDao.observeRelevantEvents(today.toEpochDay())) { cards, events ->
                PlannerCalculator.dailySuggestion(today, zone, cards, events)
            },
        )
    }

    fun observeGoalProgress(): Flow<List<GoalProgress>> = flow {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = PlannerCalculator.weekStart(today).atStartOfDay(zone).toInstant().toEpochMilli()
        emitAll(
            combine(
                plannerDao.observeGoals(),
                focusSessionDao.observeSince(weekStart),
                reviewDao.observeSince(weekStart),
            ) { goals, sessions, reviews ->
                PlannerCalculator.weekProgress(today, zone, goals, sessions, reviews)
            },
        )
    }

    fun observeEvents(): Flow<List<PlannerEventEntity>> = flow {
        emitAll(plannerDao.observeRelevantEvents(LocalDate.now().toEpochDay()))
    }

    suspend fun getEvent(id: String): PlannerEventEntity? = plannerDao.getEvent(id)

    // --- Metas ---

    suspend fun saveGoal(goal: WeeklyGoalEntity) {
        plannerDao.upsertGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteGoal(id: String) = plannerDao.softDeleteGoal(id)

    // --- Eventos: toda mudança também (re)agenda ou cancela o lembrete ---

    suspend fun saveEvent(event: PlannerEventEntity) {
        val saved = event.copy(title = event.title.trim(), updatedAt = System.currentTimeMillis())
        plannerDao.upsertEvent(saved)
        reminderScheduler.scheduleEvent(saved)
    }

    suspend fun deleteEvent(id: String) {
        plannerDao.softDeleteEvent(id)
        reminderScheduler.cancelEvent(id)
    }

    suspend fun setEventDone(id: String, done: Boolean) {
        plannerDao.setEventDone(id, done)
        plannerDao.getEvent(id)?.let { reminderScheduler.scheduleEvent(it) } // entregue: cancela; reaberto: reagenda
    }
}
