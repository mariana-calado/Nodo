package br.dia23.nodo.feature.flashcards.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object: a interface com as operações do banco.
 * Você só declara as funções e o SQL; o Room gera a implementação em tempo de compilação.
 *
 * É uma `abstract class` (e não interface) porque precisamos de uma função com corpo
 * marcada com @Transaction, que junta várias operações numa só.
 */
@Dao
abstract class DeckDao {

    // `Flow` = fluxo que emite a lista de novo sempre que a tabela muda.
    // A tela (Compose) só observa; não precisa "recarregar" manualmente.
    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE")
    abstract fun observeActiveDecks(): Flow<List<DeckEntity>>

    /**
     * Decks com contagem de cartas, calculada no próprio SQL (subqueries) para não
     * precisarmos carregar todas as cartas só para contar.
     * Uma carta conta como "para revisar" se `dueAt <= dueUntil` (o repositório passa o fim do dia de hoje).
     */
    @Query(
        "SELECT d.*, " +
            "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.isDeleted = 0) AS cardCount, " +
            "(SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.isDeleted = 0 AND c.dueAt <= :dueUntil) AS dueCount " +
            "FROM decks d WHERE d.isDeleted = 0 ORDER BY d.name COLLATE NOCASE",
    )
    abstract fun observeDecksWithStats(dueUntil: Long): Flow<List<DeckWithStats>>

    /** Emite null se o deck não existir ou tiver sido excluído. */
    @Query("SELECT * FROM decks WHERE id = :id AND isDeleted = 0")
    abstract fun observeById(id: String): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE id = :id")
    abstract suspend fun getById(id: String): DeckEntity?

    // @Upsert = insere se o id não existe, atualiza se já existe.
    // `suspend` = roda fora da thread principal, dentro de uma coroutine (parecido com async/await do Python).
    @Upsert
    abstract suspend fun upsert(deck: DeckEntity)

    @Query("UPDATE decks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    protected abstract suspend fun markDeleted(id: String, now: Long)

    @Query("UPDATE cards SET isDeleted = 1, updatedAt = :now WHERE deckId = :deckId")
    protected abstract suspend fun markCardsDeleted(deckId: String, now: Long)

    /**
     * Exclusão lógica do deck E das suas cartas.
     * O CASCADE do banco só age em DELETE real, então na exclusão lógica fazemos à mão.
     * @Transaction garante "tudo ou nada": não fica deck apagado com cartas vivas.
     */
    @Transaction
    open suspend fun softDelete(id: String, now: Long = System.currentTimeMillis()) {
        markDeleted(id, now)
        markCardsDeleted(id, now)
    }
}
