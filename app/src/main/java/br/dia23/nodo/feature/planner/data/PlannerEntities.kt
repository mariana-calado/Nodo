package br.dia23.nodo.feature.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class GoalType {
    /** Minutos de foco no Pomodoro (total ou de uma matéria). */
    FOCUS_MINUTES,

    /** Número de revisões de flashcards. */
    REVIEWS,
}

/** Meta que vale para toda semana (segunda a domingo); o progresso é calculado a partir dos registros. */
@Entity(tableName = "weekly_goals", indices = [Index("subjectId")])
data class WeeklyGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: GoalType,
    /** Minutos (FOCUS_MINUTES) ou revisões (REVIEWS) por semana. */
    val target: Int,
    /** Só em metas de foco: null = foco total, em qualquer matéria. */
    val subjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val isDeleted: Boolean = false,
)

enum class EventType { EXAM, DEADLINE }

@Entity(tableName = "planner_events", indices = [Index("dateEpochDay"), Index("subjectId")])
data class PlannerEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: EventType,
    /**
     * O dia do evento como `LocalDate.toEpochDay()` (dias desde 01/01/1970).
     * Um dia de calendário, sem hora nem fuso: a prova é "dia 12" em qualquer lugar do mundo.
     */
    val dateEpochDay: Long,
    val subjectId: String? = null,
    /** Avisar quantos dias antes (0 = no próprio dia); null = sem lembrete. */
    val reminderDaysBefore: Int? = null,
    /** Só faz sentido em prazos: marcado como entregue. */
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val isDeleted: Boolean = false,
)
