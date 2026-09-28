package br.dia23.nodo.feature.flashcards.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Uma carta de um deck.
 *
 * Os campos de "repetição espaçada" seguem o algoritmo SM-2 (implementado na próxima etapa).
 * Já ficam aqui para não precisarmos de migração do banco depois.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            // Se um deck for apagado de verdade do banco, suas cartas vão junto.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Índice em deckId: acelera "todas as cartas do deck X" e evita um aviso do Room
    // (a chave estrangeira sem índice deixa o DELETE em cascata lento).
    indices = [Index("deckId")],
)
data class CardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,

    // --- SM-2 ---
    /** Fator de facilidade. Começa em 2.5; nunca deve ficar abaixo de 1.3. */
    val easeFactor: Double = 2.5,
    /** Dias até a próxima revisão. */
    val intervalDays: Int = 0,
    /** Quantas revisões corretas seguidas. Zera quando o usuário erra. */
    val repetitions: Int = 0,
    /** Quando a carta deve ser revisada (epoch ms). Padrão = agora, ou seja, carta nova já está "devida". */
    val dueAt: Long = System.currentTimeMillis(),

    // --- Sincronização ---
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val isDeleted: Boolean = false,
)
