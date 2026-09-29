package br.dia23.nodo.feature.flashcards.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Única porta de entrada para os dados de flashcards.
 * Hoje só fala com o Room; na fase 6 também sincroniza com o Firestore,
 * e quem usa o repositório (ViewModels) não precisa mudar.
 *
 * `@Inject constructor` + `@Singleton`: o Hilt sabe criar esta classe sozinho
 * (ele já sabe fornecer os DAOs, ver DatabaseModule) e reaproveita a mesma instância no app inteiro.
 */
@Singleton
class FlashcardRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
) {
    // --- Decks ---

    /**
     * `flow { }` adia o cálculo de endOfToday() para o momento em que alguém começa a observar.
     * Assim, se o app ficar aberto de um dia para o outro, a contagem é refeita ao voltar para a tela.
     */
    fun observeDecks(): Flow<List<DeckWithStats>> = flow {
        emitAll(deckDao.observeDecksWithStats(dueUntil = endOfToday()))
    }

    fun observeDeck(deckId: String): Flow<DeckEntity?> = deckDao.observeById(deckId)

    suspend fun createDeck(name: String, description: String) {
        deckDao.upsert(DeckEntity(name = name.trim(), description = description.trim()))
    }

    suspend fun updateDeck(id: String, name: String, description: String) {
        val current = deckDao.getById(id) ?: return
        deckDao.upsert(
            current.copy(
                name = name.trim(),
                description = description.trim(),
                // Toda edição atualiza updatedAt: é o que a sincronização usará para resolver conflitos.
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteDeck(id: String) = deckDao.softDelete(id)

    // --- Cartas ---

    fun observeCards(deckId: String): Flow<List<CardEntity>> = cardDao.observeByDeck(deckId)

    suspend fun getCard(id: String): CardEntity? = cardDao.getById(id)

    suspend fun createCard(deckId: String, front: String, back: String) {
        cardDao.upsert(CardEntity(deckId = deckId, front = front.trim(), back = back.trim()))
    }

    suspend fun updateCard(id: String, front: String, back: String) {
        val current = cardDao.getById(id) ?: return
        // copy() mantém os campos do SM-2: corrigir um erro de digitação não deve zerar o progresso da carta.
        cardDao.upsert(
            current.copy(front = front.trim(), back = back.trim(), updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun deleteCard(id: String) = cardDao.softDelete(id)

    suspend fun restoreCard(id: String) = cardDao.restore(id)

    /**
     * Último milissegundo de hoje, no fuso do aparelho.
     * "Para revisar" = vence até o fim do dia (como no Anki): os intervalos do SM-2 são em dias,
     * então não faz sentido esconder uma carta só porque ela vence às 23h.
     */
    private fun endOfToday(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
}
