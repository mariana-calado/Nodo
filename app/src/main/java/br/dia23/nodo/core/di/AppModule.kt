package br.dia23.nodo.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * "Etiqueta" (qualifier) para diferenciar ESTE CoroutineScope de outros que possam existir.
 * Uso: `@ApplicationScope scope: CoroutineScope` no construtor.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Cada DataStore é um arquivo separado; o qualifier diz ao Hilt qual entregar. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PomodoroPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlannerPreferences

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Escopo que vive enquanto o processo do app viver (diferente do viewModelScope, que morre com a tela).
     * Usado pelo Pomodoro: o timer precisa terminar e salvar a sessão mesmo que nenhuma tela esteja aberta.
     * SupervisorJob: a falha de uma tarefa não cancela as outras.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @PomodoroPreferences
    fun providePomodoroDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("pomodoro") })

    @Provides
    @Singleton
    @PlannerPreferences
    fun providePlannerDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("planner") })
}
