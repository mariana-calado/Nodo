package br.dia23.nodo.feature.planner.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimesTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day = LocalDate.of(2026, 9, 30)

    private fun now(hour: Int, minute: Int = 0) = LocalDateTime.of(day, LocalTime.of(hour, minute)).atZone(zone)

    @Test
    fun lembreteDiarioAindaHojeSeOHorarioNaoPassou() {
        assertEquals(now(19), ReminderTimes.nextOccurrence(now(8), LocalTime.of(19, 0)))
    }

    @Test
    fun lembreteDiarioAmanhaSeOHorarioJaPassouOuEAgora() {
        val tomorrowAt19 = LocalDateTime.of(day.plusDays(1), LocalTime.of(19, 0)).atZone(zone)

        assertEquals(tomorrowAt19, ReminderTimes.nextOccurrence(now(20), LocalTime.of(19, 0)))
        assertEquals(tomorrowAt19, ReminderTimes.nextOccurrence(now(19), LocalTime.of(19, 0)))
    }

    @Test
    fun lembreteDeEventoSaiDiasAntesAsNove() {
        val examDay = day.plusDays(10).toEpochDay()

        assertEquals(
            LocalDateTime.of(day.plusDays(7), LocalTime.of(9, 0)).atZone(zone),
            ReminderTimes.eventReminderAt(examDay, daysBefore = 3, zone = zone),
        )
        assertEquals(
            LocalDateTime.of(day.plusDays(10), LocalTime.of(9, 0)).atZone(zone),
            ReminderTimes.eventReminderAt(examDay, daysBefore = 0, zone = zone),
        )
    }
}
