package br.dia23.nodo.core.di

import android.content.Context
import br.dia23.nodo.core.database.NodoDatabase
import br.dia23.nodo.feature.flashcards.data.CardDao
import br.dia23.nodo.feature.flashcards.data.DeckDao
import br.dia23.nodo.feature.flashcards.data.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Ensina o Hilt a criar o que ele não consegue instanciar sozinho (classes de bibliotecas
 * como o Room). Cada @Provides é uma "receita": "quando alguém pedir um NodoDatabase, faça assim".
 */
@Module
@InstallIn(SingletonComponent::class) // vive enquanto o app estiver vivo
object DatabaseModule {

    // @Singleton: abrir o banco é caro, e duas instâncias brigariam entre si. Só existe uma.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NodoDatabase =
        NodoDatabase.create(context)

    // Sem @Singleton: o DAO é leve e já vem do banco (que é único).
    @Provides
    fun provideDeckDao(database: NodoDatabase): DeckDao = database.deckDao()

    @Provides
    fun provideCardDao(database: NodoDatabase): CardDao = database.cardDao()

    @Provides
    fun provideReviewDao(database: NodoDatabase): ReviewDao = database.reviewDao()
}
