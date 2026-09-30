package br.dia23.nodo.feature.pomodoro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Uma sessão de foco do Pomodoro (as pausas não são salvas). */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            // Se a matéria sumir de verdade, a sessão fica "sem matéria" em vez de ser apagada.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("startedAt")],
)
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** null = foco sem matéria definida. */
    val subjectId: String?,
    val startedAt: Long,
    val endedAt: Long,
    /** Tempo realmente focado (sem as pausas do meio). */
    val focusedMs: Long,
    val plannedMs: Long,
    /** false quando a pessoa parou antes do fim. */
    val completed: Boolean,
    val updatedAt: Long = endedAt,
    val isDeleted: Boolean = false,
)
