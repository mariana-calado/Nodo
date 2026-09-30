package br.dia23.nodo.core.subjects

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Matéria de estudo (ex.: "Cálculo"). Compartilhada entre funcionalidades: decks, sessões de foco,
 * provas e metas apontam para ela.
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Posição na paleta fixa de cores (ui/theme/SubjectColors). Índice, e não cor, para o tema poder mudar. */
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val isDeleted: Boolean = false,
)
