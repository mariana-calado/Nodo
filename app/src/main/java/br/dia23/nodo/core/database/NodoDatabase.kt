package br.dia23.nodo.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.dia23.nodo.feature.flashcards.data.CardDao
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.data.DeckDao
import br.dia23.nodo.feature.flashcards.data.DeckEntity

/**
 * Ponto único de entrada do banco. Novas fases (sessões de Pomodoro, metas...)
 * adicionam suas entidades aqui e subimos o `version` com uma migração.
 */
@Database(
    entities = [DeckEntity::class, CardEntity::class],
    version = 1,
    // Exporta o schema em JSON (app/schemas) para versionarmos e testarmos migrações depois.
    exportSchema = true,
)
abstract class NodoDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao

    companion object {
        fun create(context: Context): NodoDatabase =
            Room.databaseBuilder(context.applicationContext, NodoDatabase::class.java, "nodo.db")
                .build()
    }
}
