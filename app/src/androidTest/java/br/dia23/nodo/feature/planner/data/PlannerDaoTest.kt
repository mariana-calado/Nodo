package br.dia23.nodo.feature.planner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.dia23.nodo.core.database.NodoDatabase
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.data.DeckEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerDaoTest {

    private lateinit var db: NodoDatabase
    private lateinit var plannerDao: PlannerDao
    private val today = 20_000L // um dia qualquer, em epochDay

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NodoDatabase::class.java).build()
        plannerDao = db.plannerDao()
    }

    @After
    fun tearDown() = db.close()

    private fun event(title: String, type: EventType, day: Long, done: Boolean = false) =
        PlannerEventEntity(title = title, type = type, dateEpochDay = day, isDone = done)

    @Test
    fun listaMostraFuturosEPrazosAtrasadosNaoEntregues() = runBlocking {
        plannerDao.upsertEvent(event("prova futura", EventType.EXAM, today + 3))
        plannerDao.upsertEvent(event("prova de hoje", EventType.EXAM, today))
        plannerDao.upsertEvent(event("prova passada", EventType.EXAM, today - 2))
        plannerDao.upsertEvent(event("prazo atrasado", EventType.DEADLINE, today - 1))
        plannerDao.upsertEvent(event("prazo entregue", EventType.DEADLINE, today - 1, done = true))

        val titles = plannerDao.observeRelevantEvents(today).first().map { it.title }

        assertEquals(listOf("prazo atrasado", "prova de hoje", "prova futura"), titles) // ordem por data
    }

    @Test
    fun excluirEMarcarComoEntregue() = runBlocking {
        val prazo = event("relatório", EventType.DEADLINE, today + 1)
        val prova = event("P1", EventType.EXAM, today + 1)
        plannerDao.upsertEvent(prazo)
        plannerDao.upsertEvent(prova)

        plannerDao.setEventDone(prazo.id, done = true)
        plannerDao.softDeleteEvent(prova.id)

        val events = plannerDao.observeRelevantEvents(today).first()
        assertEquals(listOf("relatório"), events.map { it.title })
        assertEquals(true, events.single().isDone)
    }

    @Test
    fun metasIgnoramExcluidas() = runBlocking {
        val meta = WeeklyGoalEntity(type = GoalType.REVIEWS, target = 100)
        plannerDao.upsertGoal(meta)
        plannerDao.upsertGoal(WeeklyGoalEntity(type = GoalType.FOCUS_MINUTES, target = 300))
        plannerDao.softDeleteGoal(meta.id)

        assertEquals(listOf(GoalType.FOCUS_MINUTES), plannerDao.observeGoals().first().map { it.type })
    }

    @Test
    fun vencimentosDasCartasTrazemAMateriaDoDeckEIgnoramExcluidos() = runBlocking {
        val calculo = DeckEntity(name = "Cálculo", subjectId = "calc")
        val semMateria = DeckEntity(name = "Geral")
        val excluido = DeckEntity(name = "Velho", subjectId = "calc", isDeleted = true)
        listOf(calculo, semMateria, excluido).forEach { db.deckDao().upsert(it) }
        db.cardDao().upsert(CardEntity(deckId = calculo.id, front = "a", back = "b", dueAt = 10))
        db.cardDao().upsert(CardEntity(deckId = semMateria.id, front = "c", back = "d", dueAt = 20))
        db.cardDao().upsert(CardEntity(deckId = excluido.id, front = "e", back = "f", dueAt = 30))

        val info = db.cardDao().observeDueInfo().first().sortedBy { it.dueAt }

        assertEquals(listOf(10L to "calc", 20L to null), info.map { it.dueAt to it.subjectId })
    }

    @Test
    fun revisarTodasTrazTodasAsCartasAtivasDoDeckPeloVencimento() = runBlocking {
        val deck = DeckEntity(name = "D")
        db.deckDao().upsert(deck)
        db.cardDao().upsert(CardEntity(deckId = deck.id, front = "futura", back = "x", dueAt = 500))
        db.cardDao().upsert(CardEntity(deckId = deck.id, front = "vencida", back = "x", dueAt = 100))
        val excluida = CardEntity(deckId = deck.id, front = "excluída", back = "x", dueAt = 50)
        db.cardDao().upsert(excluida)
        db.cardDao().softDelete(excluida.id)

        assertEquals(listOf("vencida", "futura"), db.cardDao().getAllActive(deck.id).map { it.front })
    }
}
