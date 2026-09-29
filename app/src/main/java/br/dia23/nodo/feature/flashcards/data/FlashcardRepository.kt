package br.dia23.nodo.feature.flashcards.data

import kotlinx.coroutines.flow.Flow
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
) {
    fun observeDecks(): Flow<List<DeckWithStats>> =
        deckDao.observeDecksWithStats(now = System.currentTimeMillis())

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
}
