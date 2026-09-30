package br.dia23.nodo.feature.stats.domain

import br.dia23.nodo.feature.flashcards.data.ReviewLogEntity
import br.dia23.nodo.feature.pomodoro.data.FocusSessionEntity
import br.dia23.nodo.feature.pomodoro.data.SubjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStats(
    val date: LocalDate,
    val focusedMs: Long,
    val reviews: Int,
    val correctReviews: Int,
)

/** subjectId/name/colorIndex null = focos sem matéria. */
data class SubjectStats(
    val subjectId: String?,
    val name: String?,
    val colorIndex: Int?,
    val focusedMs: Long,
)

/** name null = deck que não existe mais. */
data class DeckStats(
    val deckId: String,
    val name: String?,
    val reviews: Int,
    val correctReviews: Int,
) {
    val accuracy: Float get() = if (reviews == 0) 0f else correctReviews.toFloat() / reviews
}

data class StudyStats(
    /** Um item por dia do período, do mais antigo até hoje (dias sem nada aparecem zerados). */
    val days: List<DayStats>,
    val sessionCount: Int,
    val bySubject: List<SubjectStats>,
    val byDeck: List<DeckStats>,
) {
    val totalFocusedMs: Long = days.sumOf { it.focusedMs }
    val totalReviews: Int = days.sumOf { it.reviews }
    val correctReviews: Int = days.sumOf { it.correctReviews }

    /** null quando não houve revisões (0% seria mentira: não é que errou tudo). */
    val accuracy: Float? = if (totalReviews == 0) null else correctReviews.toFloat() / totalReviews

    val isEmpty: Boolean get() = sessionCount == 0 && totalReviews == 0
}

/**
 * Transforma registros "crus" (sessões de foco e revisões) nos números da tela de estatísticas.
 * Função pura: o fuso e o "hoje" chegam como parâmetro, então o teste controla tudo.
 */
object StatsCalculator {

    fun compute(
        today: LocalDate,
        days: Int,
        zone: ZoneId,
        sessions: List<FocusSessionEntity>,
        reviews: List<ReviewLogEntity>,
        subjects: List<SubjectEntity>,
        deckNames: Map<String, String>,
    ): StudyStats {
        val firstDay = today.minusDays(days - 1L)
        val period = firstDay..today

        // O "dia" de um registro é o dia no fuso do aparelho (uma revisão às 23h30 conta para hoje, não amanhã).
        fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

        val periodSessions = sessions.filter { !it.isDeleted && it.startedAt.toLocalDate() in period }
        val periodReviews = reviews.filter { !it.isDeleted && it.reviewedAt.toLocalDate() in period }

        val focusByDay = periodSessions
            .groupBy { it.startedAt.toLocalDate() }
            .mapValues { (_, sessionsOfDay) -> sessionsOfDay.sumOf { it.focusedMs } }
        val reviewsByDay = periodReviews.groupBy { it.reviewedAt.toLocalDate() }

        val dayStats = (0 until days).map { offset ->
            val date = firstDay.plusDays(offset.toLong())
            val reviewsOfDay = reviewsByDay[date].orEmpty()
            DayStats(
                date = date,
                focusedMs = focusByDay[date] ?: 0,
                reviews = reviewsOfDay.size,
                correctReviews = reviewsOfDay.count { it.grade.isCorrect },
            )
        }

        val subjectsById = subjects.associateBy { it.id }
        val bySubject = periodSessions
            .groupBy { it.subjectId }
            .map { (subjectId, sessionsOfSubject) ->
                val subject = subjectId?.let { subjectsById[it] }
                SubjectStats(subjectId, subject?.name, subject?.colorIndex, sessionsOfSubject.sumOf { it.focusedMs })
            }
            .sortedByDescending { it.focusedMs }

        val byDeck = periodReviews
            .groupBy { it.deckId }
            .map { (deckId, reviewsOfDeck) ->
                DeckStats(deckId, deckNames[deckId], reviewsOfDeck.size, reviewsOfDeck.count { it.grade.isCorrect })
            }
            .sortedByDescending { it.reviews }

        return StudyStats(dayStats, periodSessions.size, bySubject, byDeck)
    }
}
