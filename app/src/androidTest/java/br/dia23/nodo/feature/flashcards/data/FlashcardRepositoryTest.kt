package br.dia23.nodo.feature.flashcards.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.dia23.nodo.core.database.NodoDatabase
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardRepositoryTest {

    private lateinit var db: NodoDatabase
    private lateinit var repository: FlashcardRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NodoDatabase::class.java).build()
        // Sem Hilt no teste: criamos o repositório "na mão" com os DAOs do banco em memória.
        repository = FlashcardRepository(db.deckDao(), db.cardDao(), db.reviewDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun editarTextoDaCartaNaoZeraOProgressoSm2() = runBlocking {
        val deck = DeckEntity(name = "D")
        db.deckDao().upsert(deck)
        val card = CardEntity(
            deckId = deck.id, front = "cat", back = "gatp",
            easeFactor = 2.1, intervalDays = 6, repetitions = 3, dueAt = 42L, updatedAt = 1L,
        )
        db.cardDao().upsert(card)

        repository.updateCard(card.id, front = "cat", back = "gato")

        val updated = repository.getCard(card.id)!!
        assertEquals("gato", updated.back)
        assertEquals(2.1, updated.easeFactor, 0.0)
        assertEquals(6, updated.intervalDays)
        assertEquals(3, updated.repetitions)
        assertEquals(42L, updated.dueAt)
        assertTrue("updatedAt deve avançar", updated.updatedAt > 1L)
    }

    @Test
    fun cartaCriadaAgoraContaComoParaRevisarHoje() = runBlocking {
        val deck = DeckEntity(name = "D")
        db.deckDao().upsert(deck)

        // Observa os decks ANTES de criar a carta: o bug que o "fim do dia" corrige era exatamente esse caso.
        val decks = repository.observeDecks()
        repository.createCard(deck.id, "a", "b")

        assertEquals(1, decks.first().single().dueCount)
    }

    @Test
    fun acertarTiraACartaDaRevisaoDeHojeERegistraOHistorico() = runBlocking {
        val deck = DeckEntity(name = "D")
        db.deckDao().upsert(deck)
        repository.createCard(deck.id, "cat", "gato")
        val card = repository.getDueCards(deck.id).single()

        repository.reviewCard(card, ReviewGrade.GOOD)

        assertEquals(0, repository.getDueCards(deck.id).size) // agora só vence amanhã
        val updated = repository.getCard(card.id)!!
        assertEquals(1, updated.repetitions)
        assertEquals(1, updated.intervalDays)
        assertEquals(listOf(ReviewGrade.GOOD), db.reviewDao().getLogsForCard(card.id).map { it.grade })
    }

    @Test
    fun errarZeraASequenciaDaCarta() = runBlocking {
        val deck = DeckEntity(name = "D")
        db.deckDao().upsert(deck)
        val card = CardEntity(deckId = deck.id, front = "a", back = "b", repetitions = 4, intervalDays = 30)
        db.cardDao().upsert(card)

        repository.reviewCard(card, ReviewGrade.AGAIN)

        val updated = repository.getCard(card.id)!!
        assertEquals(0, updated.repetitions)
        assertEquals(1, updated.intervalDays)
    }
}
