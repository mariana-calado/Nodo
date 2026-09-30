package br.dia23.nodo.feature.planner.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Cálculo dos horários dos lembretes (puro: recebe o "agora", não lê o relógio). */
object ReminderTimes {

    /** Avisos de provas e prazos saem às 9h do dia escolhido. */
    val EVENT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    /** Próxima vez que o relógio marca `time`: hoje, se ainda não passou; senão, amanhã. */
    fun nextOccurrence(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val todayAt = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    }

    /** Quando avisar sobre um evento: `daysBefore` dias antes da data, às 9h. */
    fun eventReminderAt(dateEpochDay: Long, daysBefore: Int, zone: ZoneId): ZonedDateTime =
        LocalDate.ofEpochDay(dateEpochDay).minusDays(daysBefore.toLong()).atTime(EVENT_REMINDER_TIME).atZone(zone)
}
