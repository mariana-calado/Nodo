package br.dia23.nodo.feature.flashcards.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt")
    fun observeByDeck(deckId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    /** Cartas para o modo estudo: as que já venceram (dueAt <= agora), mais atrasadas primeiro. */
    @Query(
        "SELECT * FROM cards WHERE deckId = :deckId AND isDeleted = 0 AND dueAt <= :now " +
            "ORDER BY dueAt",
    )
    suspend fun getDueCards(deckId: String, now: Long): List<CardEntity>

    /** Quantas cartas estão devidas em todos os decks (o Planner vai usar isso na fase 3). */
    @Query("SELECT COUNT(*) FROM cards WHERE isDeleted = 0 AND dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Query("UPDATE cards SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())
}
