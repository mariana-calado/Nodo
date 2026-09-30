package br.dia23.nodo.feature.pomodoro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.dia23.nodo.core.database.NodoDatabase
import br.dia23.nodo.feature.pomodoro.domain.FocusRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class PomodoroRepositoryTest {

    private lateinit var db: NodoDatabase
    private lateinit var repository: PomodoroRepository
    private val minute = 60_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NodoDatabase::class.java).build()
        repository = PomodoroRepository(db.subjectDao(), db.focusSessionDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun criaMateriaEListaEmOrdemAlfabetica() = runBlocking {
        repository.createSubject("  Química ", colorIndex = 1)
        repository.createSubject("cálculo", colorIndex = 2)

        val names = repository.observeSubjects().first().map { it.name }

        assertEquals(listOf("cálculo", "Química"), names) // sem espaços extras, sem diferenciar maiúsculas
    }

    @Test
    fun salvaSessoesESomaOFocoDeHoje() = runBlocking {
        val subject = repository.createSubject("Cálculo", colorIndex = 0)
        // Horários a partir do início de hoje: o teste não quebra se rodar logo depois da meia-noite.
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.saveFocusSession(FocusRecord(subject.id, today + minute, today + 26 * minute, 25 * minute, 25 * minute, true))
        repository.saveFocusSession(FocusRecord(null, today + 30 * minute, today + 40 * minute, 10 * minute, 25 * minute, false))
        // Uma sessão de ontem não entra no "hoje".
        repository.saveFocusSession(FocusRecord(null, today - 60 * minute, today - 35 * minute, 25 * minute, 25 * minute, true))

        assertEquals(35 * minute, repository.observeFocusedToday().first())
    }

    @Test
    fun focoDeHojeEZeroSemSessoes() = runBlocking {
        assertEquals(0L, repository.observeFocusedToday().first())
    }
}
