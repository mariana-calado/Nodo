package br.dia23.nodo.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garante que quem já usa o app não perde dados ao atualizar.
 * O helper cria um banco REAL na versão antiga (a partir de app/schemas/1.json), aplica a migração
 * e confere se o resultado bate exatamente com o schema da versão nova (2.json).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NodoDatabase::class.java,
    )

    @Test
    fun migracao1Para2PreservaDecksECartas() {
        // Versão 1: SQL "cru", porque as classes Kotlin de hoje já descrevem a versão 2.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO decks (id, name, description, createdAt, updatedAt, isDeleted) " +
                    "VALUES ('d1', 'Inglês', '', 1, 1, 0)",
            )
            execSQL(
                "INSERT INTO cards (id, deckId, front, back, easeFactor, intervalDays, repetitions, dueAt, " +
                    "createdAt, updatedAt, isDeleted) VALUES ('c1', 'd1', 'cat', 'gato', 2.5, 6, 2, 1, 1, 1, 0)",
            )
            close()
        }

        // Roda a AutoMigration 1 -> 2 e valida o schema resultante (falha se faltar coluna, índice...).
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT front, repetitions FROM cards WHERE id = 'c1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("cat", cursor.getString(0))
            assertEquals(2, cursor.getInt(1)) // o progresso SM-2 sobreviveu
        }
        db.query("SELECT COUNT(*) FROM review_logs").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0)) // tabela nova existe e começa vazia
        }
    }

    @Test
    fun migracao2Para3PreservaHistoricoECriaTabelasDoPomodoro() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO decks (id, name, description, createdAt, updatedAt, isDeleted) " +
                    "VALUES ('d1', 'Inglês', '', 1, 1, 0)",
            )
            execSQL(
                "INSERT INTO cards (id, deckId, front, back, easeFactor, intervalDays, repetitions, dueAt, " +
                    "createdAt, updatedAt, isDeleted) VALUES ('c1', 'd1', 'cat', 'gato', 2.5, 1, 1, 1, 1, 1, 0)",
            )
            execSQL(
                "INSERT INTO review_logs (id, cardId, deckId, grade, reviewedAt, updatedAt, isDeleted) " +
                    "VALUES ('r1', 'c1', 'd1', 'GOOD', 5, 5, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        db.query("SELECT grade FROM review_logs WHERE id = 'r1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("GOOD", cursor.getString(0)) // o histórico do modo estudo sobreviveu
        }
        // As tabelas novas existem e aceitam dados (inclusive sessão sem matéria).
        db.execSQL("INSERT INTO subjects (id, name, colorIndex, createdAt, updatedAt, isDeleted) VALUES ('s1', 'Cálculo', 0, 1, 1, 0)")
        db.execSQL(
            "INSERT INTO focus_sessions (id, subjectId, startedAt, endedAt, focusedMs, plannedMs, completed, " +
                "updatedAt, isDeleted) VALUES ('f1', NULL, 1, 2, 1500000, 1500000, 1, 2, 0)",
        )
        db.query("SELECT COUNT(*) FROM focus_sessions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun migracao3Para4AdicionaMateriaNosDecksSemPerderNada() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO decks (id, name, description, createdAt, updatedAt, isDeleted) " +
                    "VALUES ('d1', 'Inglês', '', 1, 1, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query("SELECT name, subjectId FROM decks WHERE id = 'd1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Inglês", cursor.getString(0))
            assertTrue(cursor.isNull(1)) // deck antigo fica sem matéria
        }
        db.execSQL(
            "INSERT INTO planner_events (id, title, type, dateEpochDay, subjectId, reminderDaysBefore, isDone, " +
                "createdAt, updatedAt, isDeleted) VALUES ('e1', 'P1', 'EXAM', 20000, NULL, 1, 0, 1, 1, 0)",
        )
        db.execSQL(
            "INSERT INTO weekly_goals (id, type, target, subjectId, createdAt, updatedAt, isDeleted) " +
                "VALUES ('g1', 'REVIEWS', 100, NULL, 1, 1, 0)",
        )
    }

    @Test
    fun migracaoDaVersao1DiretoParaAUltima() {
        // Quem ficou sem atualizar o app desde a versão 1 passa por todas as migrações em sequência.
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, 4, true).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
