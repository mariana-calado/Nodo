package br.dia23.nodo.feature.flashcards.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.dia23.nodo.core.database.NodoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardsDaoTest {

    private lateinit var db: NodoDatabase
    private lateinit var deckDao: DeckDao
    private lateinit var cardDao: CardDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Banco em memória: some ao fechar, então cada teste começa do zero.
        db = Room.inMemoryDatabaseBuilder(context, NodoDatabase::class.java).build()
        deckDao = db.deckDao()
        cardDao = db.cardDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insereDeckECartaELeDeVolta() = runBlocking {
        val deck = DeckEntity(name = "Inglês")
        deckDao.upsert(deck)
        cardDao.upsert(CardEntity(deckId = deck.id, front = "cat", back = "gato"))

        assertEquals(listOf("Inglês"), deckDao.observeActiveDecks().first().map { it.name })
        assertEquals(listOf("cat"), cardDao.observeByDeck(deck.id).first().map { it.front })
    }

    @Test
    fun upsertComMesmoIdAtualizaEmVezDeDuplicar() = runBlocking {
        val deck = DeckEntity(name = "Antigo")
        deckDao.upsert(deck)
        deckDao.upsert(deck.copy(name = "Novo"))

        val decks = deckDao.observeActiveDecks().first()
        assertEquals(1, decks.size)
        assertEquals("Novo", decks.single().name)
    }

    @Test
    fun softDeleteDoDeckEscondeDeckECartasMasMantemALinha() = runBlocking {
        val deck = DeckEntity(name = "Temp")
        deckDao.upsert(deck)
        val card = CardEntity(deckId = deck.id, front = "a", back = "b")
        cardDao.upsert(card)

        deckDao.softDelete(deck.id)

        assertEquals(0, deckDao.observeActiveDecks().first().size)
        assertEquals(0, cardDao.observeByDeck(deck.id).first().size)
        // A linha continua no banco (marcada como excluída), pronta para sincronizar.
        assertNotNull(deckDao.getById(deck.id))
        assertEquals(true, cardDao.getById(card.id)!!.isDeleted)
    }

    @Test
    fun getDueCardsRetornaSoAsVencidas() = runBlocking {
        val deck = DeckEntity(name = "D")
        deckDao.upsert(deck)
        val now = 1_000_000L
        cardDao.upsert(CardEntity(deckId = deck.id, front = "vencida", back = "x", dueAt = now - 1))
        cardDao.upsert(CardEntity(deckId = deck.id, front = "futura", back = "x", dueAt = now + 86_400_000))

        assertEquals(listOf("vencida"), cardDao.getDueCards(deck.id, now).map { it.front })
    }
}
