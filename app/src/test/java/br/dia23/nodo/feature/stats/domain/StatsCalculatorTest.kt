package br.dia23.nodo.feature.stats.domain

import br.dia23.nodo.feature.flashcards.data.ReviewLogEntity
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import br.dia23.nodo.feature.pomodoro.data.FocusSessionEntity
import br.dia23.nodo.feature.pomodoro.data.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 9, 29)
    private val minute = 60_000L

    /** Horário local -> epoch ms, para escrever os testes em "dia/hora" legíveis. */
    private fun at(date: LocalDate, hour: Int, minuteOfHour: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minuteOfHour)).atZone(zone).toInstant().toEpochMilli()

    private fun session(start: Long, minutes: Long, subjectId: String? = null, deleted: Boolean = false) =
        FocusSessionEntity(
            subjectId = subjectId, startedAt = start, endedAt = start + minutes * minute,
            focusedMs = minutes * minute, plannedMs = 25 * minute, completed = true, isDeleted = deleted,
        )

    private fun review(time: Long, grade: ReviewGrade, deckId: String = "d1") =
        ReviewLogEntity(cardId = "c", deckId = deckId, grade = grade, reviewedAt = time)

    private fun compute(
        days: Int = 7,
        sessions: List<FocusSessionEntity> = emptyList(),
        reviews: List<ReviewLogEntity> = emptyList(),
        subjects: List<SubjectEntity> = emptyList(),
        deckNames: Map<String, String> = emptyMap(),
    ) = StatsCalculator.compute(today, days, zone, sessions, reviews, subjects, deckNames)

    @Test
    fun periodoTemUmDiaPorItemTerminandoHojeMesmoSemDados() {
        val stats = compute(days = 7)

        assertEquals(7, stats.days.size)
        assertEquals(today.minusDays(6), stats.days.first().date)
        assertEquals(today, stats.days.last().date)
        assertTrue(stats.isEmpty)
        assertNull(stats.accuracy) // sem revisões não existe porcentagem
    }

    @Test
    fun somaOFocoPorDiaEIgnoraForaDoPeriodoEExcluidas() {
        val stats = compute(
            sessions = listOf(
                session(at(today, 9), 25),
                session(at(today, 14), 20),
                session(at(today.minusDays(2), 10), 50),
                session(at(today.minusDays(10), 10), 25), // fora dos 7 dias
                session(at(today, 16), 25, deleted = true), // excluída
            ),
        )

        assertEquals(45 * minute, stats.days.last().focusedMs)
        assertEquals(50 * minute, stats.days[4].focusedMs)
        assertEquals(95 * minute, stats.totalFocusedMs)
        assertEquals(3, stats.sessionCount)
    }

    @Test
    fun usaODiaDoFusoDoAparelho() {
        // 23h30 em São Paulo já é "amanhã" em UTC; deve contar para o dia local.
        val stats = compute(sessions = listOf(session(at(today.minusDays(1), 23, 30), 25)))

        assertEquals(25 * minute, stats.days[5].focusedMs)
        assertEquals(0, stats.days[6].focusedMs)
    }

    @Test
    fun acertosContamDificilComoAcertoEErreiComoErro() {
        val stats = compute(
            reviews = listOf(
                review(at(today, 8), ReviewGrade.GOOD),
                review(at(today, 8), ReviewGrade.HARD),
                review(at(today, 8), ReviewGrade.EASY),
                review(at(today, 8), ReviewGrade.AGAIN),
            ),
        )

        assertEquals(4, stats.totalReviews)
        assertEquals(3, stats.correctReviews)
        assertEquals(0.75f, stats.accuracy!!, 1e-6f)
        assertEquals(3, stats.days.last().correctReviews)
    }

    @Test
    fun agrupaPorMateriaDoMaiorParaOMenorIncluindoSemMateria() {
        val calculo = SubjectEntity(id = "calc", name = "Cálculo", colorIndex = 2)
        val stats = compute(
            sessions = listOf(
                session(at(today, 9), 25, subjectId = "calc"),
                session(at(today, 10), 50, subjectId = "calc"),
                session(at(today, 11), 25, subjectId = null),
            ),
            subjects = listOf(calculo),
        )

        assertEquals(
            listOf(
                SubjectStats("calc", "Cálculo", 2, 75 * minute),
                SubjectStats(null, null, null, 25 * minute),
            ),
            stats.bySubject,
        )
    }

    @Test
    fun agrupaPorDeckComNomeEAcertos() {
        val stats = compute(
            reviews = listOf(
                review(at(today, 8), ReviewGrade.GOOD, deckId = "ing"),
                review(at(today, 8), ReviewGrade.AGAIN, deckId = "ing"),
                review(at(today, 8), ReviewGrade.GOOD, deckId = "sumiu"),
            ),
            deckNames = mapOf("ing" to "Inglês"),
        )

        assertEquals(DeckStats("ing", "Inglês", reviews = 2, correctReviews = 1), stats.byDeck[0])
        assertEquals(0.5f, stats.byDeck[0].accuracy, 1e-6f)
        assertNull(stats.byDeck[1].name) // deck que não existe mais
    }
}
