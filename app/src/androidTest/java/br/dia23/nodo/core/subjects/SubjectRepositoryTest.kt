package br.dia23.nodo.core.subjects

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.dia23.nodo.core.database.NodoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubjectRepositoryTest {

    private lateinit var db: NodoDatabase
    private lateinit var repository: SubjectRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NodoDatabase::class.java).build()
        repository = SubjectRepository(db.subjectDao())
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
}
