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

    /** Cartas para o modo estudo: as que vencem até `dueUntil`, mais atrasadas primeiro. */
    @Query(
        "SELECT * FROM cards WHERE deckId = :deckId AND isDeleted = 0 AND dueAt <= :dueUntil " +
            "ORDER BY dueAt, createdAt",
    )
    suspend fun getDueCards(deckId: String, dueUntil: Long): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND isDeleted = 0 AND dueAt <= :dueUntil")
    fun observeDueCountByDeck(deckId: String, dueUntil: Long): Flow<Int>

    /** "Revisar todas": todas as cartas ativas do deck, as que vencem primeiro na frente. */
    @Query("SELECT * FROM cards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY dueAt, createdAt")
    suspend fun getAllActive(deckId: String): List<CardEntity>

    /**
     * Para o Planner: só o vencimento de cada carta ativa e a matéria do seu deck.
     * Projeção leve: não carrega frente/verso de milhares de cartas à toa.
     */
    @Query(
        "SELECT c.dueAt AS dueAt, d.subjectId AS subjectId FROM cards c " +
            "INNER JOIN decks d ON d.id = c.deckId WHERE c.isDeleted = 0 AND d.isDeleted = 0",
    )
    fun observeDueInfo(): Flow<List<CardDueInfo>>

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Query("UPDATE cards SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    /** Desfaz a exclusão lógica. Atualiza updatedAt para a sincronização saber que a carta "voltou". */
    @Query("UPDATE cards SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long = System.currentTimeMillis())
}

/** Resultado da consulta do Planner (não é tabela: só o formato das colunas do SELECT). */
data class CardDueInfo(val dueAt: Long, val subjectId: String?)
