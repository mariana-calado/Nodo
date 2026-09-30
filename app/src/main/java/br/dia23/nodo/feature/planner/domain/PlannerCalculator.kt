package br.dia23.nodo.feature.planner.domain

import br.dia23.nodo.feature.flashcards.data.CardDueInfo
import br.dia23.nodo.feature.flashcards.data.ReviewLogEntity
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.GoalType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.WeeklyGoalEntity
import br.dia23.nodo.feature.pomodoro.data.FocusSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Preparação para uma prova que se aproxima. */
data class ExamPrep(
    val eventId: String,
    val title: String,
    val subjectId: String,
    val daysLeft: Int,
    /** Cartas da matéria que só venceriam no dia da prova ou depois: a revisão normal não as mostraria a tempo. */
    val cardsAhead: Int,
    /** Quantas dessas revisar por dia para passar por todas antes da prova. */
    val perDay: Int,
)

data class DailySuggestion(
    /** Cartas que vencem até o fim de hoje (a revisão normal do SM-2). */
    val dueToday: Int,
    val examPreps: List<ExamPrep>,
) {
    val total: Int get() = dueToday + examPreps.sumOf { it.perDay }
}

data class GoalProgress(
    val goal: WeeklyGoalEntity,
    /** Minutos de foco ou revisões feitos nesta semana. */
    val current: Int,
    /** Dias que restam na semana, contando hoje (segunda = 7, domingo = 1). */
    val daysLeftInWeek: Int,
) {
    val fraction: Float get() = (current.toFloat() / goal.target).coerceIn(0f, 1f)
    val isDone: Boolean get() = current >= goal.target

    /** Quanto fazer por dia, hoje incluído, para bater a meta até domingo. */
    val perDayToFinish: Int get() = if (isDone) 0 else ceilDiv(goal.target - current, daysLeftInWeek)
}

/**
 * As regras do Planner, como funções puras (o "hoje" e o fuso chegam como parâmetro).
 */
object PlannerCalculator {

    /** Provas mais distantes que isso ainda não geram sugestão de revisão extra. */
    const val EXAM_HORIZON_DAYS = 14

    /**
     * Sugestão do dia = cartas vencidas hoje + preparação para provas próximas.
     *
     * Para cada prova (com matéria) nos próximos 14 dias: as cartas da matéria que só venceriam
     * no dia da prova ou depois não apareceriam na revisão normal a tempo. Elas são divididas
     * pelos dias que faltam, para dar uma passada em todas antes da prova.
     * Se houver duas provas da mesma matéria, vale a mais próxima (evita contar as cartas duas vezes).
     */
    fun dailySuggestion(
        today: LocalDate,
        zone: ZoneId,
        cards: List<CardDueInfo>,
        events: List<PlannerEventEntity>,
    ): DailySuggestion {
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val dueToday = cards.count { it.dueAt <= endOfToday }

        val examPreps = events
            .filter { it.type == EventType.EXAM && !it.isDeleted && it.subjectId != null }
            .map { it to (it.dateEpochDay - today.toEpochDay()).toInt() }
            .filter { (_, daysLeft) -> daysLeft in 1..EXAM_HORIZON_DAYS } // prova hoje: não dá mais tempo
            .groupBy { (exam, _) -> exam.subjectId }
            .map { (_, examsOfSubject) -> examsOfSubject.minBy { (_, daysLeft) -> daysLeft } }
            .mapNotNull { (exam, daysLeft) ->
                val examStart = LocalDate.ofEpochDay(exam.dateEpochDay).atStartOfDay(zone).toInstant().toEpochMilli()
                val cardsAhead = cards.count { it.subjectId == exam.subjectId && it.dueAt >= examStart }
                if (cardsAhead == 0) return@mapNotNull null
                ExamPrep(
                    eventId = exam.id,
                    title = exam.title,
                    subjectId = exam.subjectId!!,
                    daysLeft = daysLeft,
                    cardsAhead = cardsAhead,
                    perDay = ceilDiv(cardsAhead, daysLeft),
                )
            }
            .sortedBy { it.daysLeft }

        return DailySuggestion(dueToday, examPreps)
    }

    /** Semana de segunda a domingo (padrão ISO). */
    fun weekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekProgress(
        today: LocalDate,
        zone: ZoneId,
        goals: List<WeeklyGoalEntity>,
        sessions: List<FocusSessionEntity>,
        reviews: List<ReviewLogEntity>,
    ): List<GoalProgress> {
        val start = weekStart(today)
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = start.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val daysLeft = 8 - today.dayOfWeek.value // segunda (1) -> 7 ... domingo (7) -> 1

        val weekSessions = sessions.filter { !it.isDeleted && it.startedAt in from until until }
        val weekReviews = reviews.count { !it.isDeleted && it.reviewedAt in from until until }

        return goals.filter { !it.isDeleted }.map { goal ->
            val current = when (goal.type) {
                GoalType.FOCUS_MINUTES -> weekSessions
                    .filter { goal.subjectId == null || it.subjectId == goal.subjectId }
                    .sumOf { it.focusedMs }
                    .let { (it / 60_000).toInt() }
                GoalType.REVIEWS -> weekReviews
            }
            GoalProgress(goal, current, daysLeft)
        }
    }
}

/** Divisão arredondada para cima: 10 cartas em 3 dias = 4 por dia (e não 3, que deixaria sobra). */
private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
