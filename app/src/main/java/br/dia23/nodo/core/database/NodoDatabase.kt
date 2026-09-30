package br.dia23.nodo.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.dia23.nodo.feature.flashcards.data.CardDao
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.data.DeckDao
import br.dia23.nodo.feature.flashcards.data.DeckEntity
import br.dia23.nodo.feature.flashcards.data.ReviewDao
import br.dia23.nodo.feature.flashcards.data.ReviewLogEntity
import br.dia23.nodo.feature.pomodoro.data.FocusSessionDao
import br.dia23.nodo.feature.pomodoro.data.FocusSessionEntity
import br.dia23.nodo.feature.pomodoro.data.SubjectDao
import br.dia23.nodo.feature.pomodoro.data.SubjectEntity

/**
 * Ponto único de entrada do banco. Novas fases (sessões de Pomodoro, metas...)
 * adicionam suas entidades aqui e sobem o `version` com uma migração.
 *
 * Histórico de versões:
 * 1 - decks e cards
 * 2 - review_logs (histórico do modo estudo)
 * 3 - subjects e focus_sessions (Pomodoro)
 */
@Database(
    entities = [
        DeckEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        SubjectEntity::class,
        FocusSessionEntity::class,
    ],
    version = 3,
    // Exporta o schema em JSON (app/schemas): o Room compara as versões para gerar as AutoMigrations.
    exportSchema = true,
    // Migração 1 -> 2 gerada pelo Room: ele compara 1.json com 2.json e vê que só entrou uma tabela nova.
    // Sem migração, o app travaria ao abrir para quem já tem o banco na versão 1.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class NodoDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun reviewDao(): ReviewDao
    abstract fun subjectDao(): SubjectDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        fun create(context: Context): NodoDatabase =
            Room.databaseBuilder(context.applicationContext, NodoDatabase::class.java, "nodo.db")
                .build()
    }
}
