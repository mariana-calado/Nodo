package br.dia23.nodo.feature.flashcards.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class ReviewDao {

    @Upsert
    protected abstract suspend fun upsertCard(card: CardEntity)

    @Insert
    protected abstract suspend fun insertLog(log: ReviewLogEntity)

    /**
     * Salva o novo agendamento da carta E o registro da revisão juntos.
     * @Transaction: se o app fechar no meio, não fica carta atualizada sem histórico (nem o contrário).
     */
    @Transaction
    open suspend fun recordReview(card: CardEntity, log: ReviewLogEntity) {
        upsertCard(card)
        insertLog(log)
    }

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId AND isDeleted = 0 ORDER BY reviewedAt")
    abstract suspend fun getLogsForCard(cardId: String): List<ReviewLogEntity>
}
