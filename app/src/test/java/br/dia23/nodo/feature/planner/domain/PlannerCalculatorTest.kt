package br.dia23.nodo.feature.planner.domain

import br.dia23.nodo.feature.flashcards.data.CardDueInfo
import br.dia23.nodo.feature.flashcards.data.ReviewLogEntity
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import br.dia23.nodo.feature.planner.data.EventType
import br.dia23.nodo.feature.planner.data.GoalType
import br.dia23.nodo.feature.planner.data.PlannerEventEntity
import br.dia23.nodo.feature.planner.data.WeeklyGoalEntity
import br.dia23.nodo.feature.pomodoro.data.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PlannerCalculatorTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 9, 30) // uma quarta-feira
    private val minute = 60_000L

    private fun at(date: LocalDate, hour: Int = 12): Long =
        date.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun card(dueDate: LocalDate, subject: String? = "calc") = CardDueInfo(at(dueDate), subject)

    private fun exam(inDays: Long, subject: String? = "calc", title: String = "P1") = PlannerEventEntity(
        title = title, type = EventType.EXAM, dateEpochDay = today.plusDays(inDays).toEpochDay(), subjectId = subject,
    )

    // --- Sugestão do dia ---

    @Test
    fun venceHojeIncluiAtrasadasENaoIncluiAmanha() {
        val cards = listOf(card(today.minusDays(3)), card(today), card(today.plusDays(1)))

        val suggestion = PlannerCalculator.dailySuggestion(today, zone, cards, events = emptyList())

        assertEquals(2, suggestion.dueToday)
        assertEquals(2, suggestion.total)
    }

    @Test
    fun provaProximaDistribuiAsCartasQueSoVenceriamDepoisDela() {
        val cards = listOf(
            card(today), // vence hoje: já está na revisão normal
            card(today.plusDays(2)), // vence antes da prova: aparece sozinha a tempo
            card(today.plusDays(5)), // vence no dia da prova: tarde demais
            card(today.plusDays(30)),
            card(today.plusDays(60)),
            card(today.plusDays(90)),
            card(today.plusDays(30), subject = "outra"), // outra matéria
        )

        val suggestion = PlannerCalculator.dailySuggestion(today, zone, cards, listOf(exam(inDays = 5)))

        val prep = suggestion.examPreps.single()
        assertEquals(5, prep.daysLeft)
        assertEquals(4, prep.cardsAhead)
        assertEquals(1, prep.perDay) // 4 cartas em 5 dias = 1 por dia (arredondando para cima)
        assertEquals(1 + 1, suggestion.total)
    }

    @Test
    fun ignoraProvaSemMateriaDeHojeDistanteEPrazos() {
        val cards = List(10) { card(today.plusDays(100)) }
        val events = listOf(
            exam(inDays = 5, subject = null),
            exam(inDays = 0), // hoje: não dá mais tempo de se preparar
            exam(inDays = 20), // além do horizonte de 14 dias
            exam(inDays = 3).copy(type = EventType.DEADLINE),
        )

        assertTrue(PlannerCalculator.dailySuggestion(today, zone, cards, events).examPreps.isEmpty())
    }

    @Test
    fun duasProvasDaMesmaMateriaUsamAMaisProxima() {
        val cards = List(9) { card(today.plusDays(100)) }

        val preps = PlannerCalculator.dailySuggestion(
            today, zone, cards, listOf(exam(inDays = 10, title = "P2"), exam(inDays = 3, title = "P1")),
        ).examPreps

        assertEquals(listOf("P1"), preps.map { it.title })
        assertEquals(3, preps.single().perDay) // 9 cartas em 3 dias
    }

    // --- Metas da semana ---

    private fun goal(type: GoalType, target: Int, subject: String? = null) =
        WeeklyGoalEntity(type = type, target = target, subjectId = subject)

    private fun session(date: LocalDate, minutes: Long, subject: String? = "calc") = FocusSessionEntity(
        subjectId = subject, startedAt = at(date), endedAt = at(date) + minutes * minute,
        focusedMs = minutes * minute, plannedMs = 25 * minute, completed = true,
    )

    @Test
    fun metaDeFocoSomaSoASemanaAtualEFiltraPorMateria() {
        val monday = today.minusDays(2)
        val sessions = listOf(
            session(monday, 50),
            session(today, 25),
            session(today, 30, subject = "quimica"),
            session(monday.minusDays(1), 100), // domingo passado: semana anterior
        )

        val (calculo, total) = PlannerCalculator.weekProgress(
            today, zone, listOf(goal(GoalType.FOCUS_MINUTES, 300, "calc"), goal(GoalType.FOCUS_MINUTES, 600)),
            sessions, reviews = emptyList(),
        )

        assertEquals(75, calculo.current)
        assertEquals(105, total.current)
        assertEquals(5, calculo.daysLeftInWeek) // quarta a domingo
        assertEquals(45, calculo.perDayToFinish) // faltam 225 min em 5 dias
    }

    @Test
    fun metaDeRevisoesContaAsRevisoesDaSemanaEFicaCompleta() {
        val reviews = List(12) { ReviewLogEntity(cardId = "c", deckId = "d", grade = ReviewGrade.GOOD, reviewedAt = at(today)) }

        val progress = PlannerCalculator.weekProgress(today, zone, listOf(goal(GoalType.REVIEWS, 10)), emptyList(), reviews).single()

        assertEquals(12, progress.current)
        assertTrue(progress.isDone)
        assertEquals(1f, progress.fraction)
        assertEquals(0, progress.perDayToFinish)
    }

    @Test
    fun semanaComecaNaSegunda() {
        assertEquals(LocalDate.of(2026, 9, 28), PlannerCalculator.weekStart(today))
        assertEquals(LocalDate.of(2026, 9, 28), PlannerCalculator.weekStart(LocalDate.of(2026, 9, 28)))
        assertEquals(LocalDate.of(2026, 9, 28), PlannerCalculator.weekStart(LocalDate.of(2026, 10, 4))) // domingo
    }
}
