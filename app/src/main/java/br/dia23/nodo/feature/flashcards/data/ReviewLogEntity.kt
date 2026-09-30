package br.dia23.nodo.feature.flashcards.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import java.util.UUID

/**
 * Uma revisão feita no modo estudo (só a primeira resposta de cada carta na sessão).
 * É a matéria-prima das estatísticas da fase 2: acertos por dia, por deck, sequência de estudo...
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId"), Index("deckId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    // Repetido da carta de propósito: as estatísticas por deck não precisam de JOIN.
    val deckId: String,
    // O Room guarda enums como texto ("GOOD", "HARD"...) automaticamente.
    val grade: ReviewGrade,
    val reviewedAt: Long,
    val updatedAt: Long = reviewedAt,
    val isDeleted: Boolean = false,
)
