package br.dia23.nodo.feature.stats.data

import br.dia23.nodo.feature.flashcards.data.DeckDao
import br.dia23.nodo.feature.flashcards.data.ReviewDao
import br.dia23.nodo.feature.pomodoro.data.FocusSessionDao
import br.dia23.nodo.feature.pomodoro.data.SubjectDao
import br.dia23.nodo.feature.stats.domain.StatsCalculator
import br.dia23.nodo.feature.stats.domain.StudyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Junta dados de duas funcionalidades (flashcards e pomodoro) para as estatísticas.
 * Só lê: quem grava revisões e sessões são os repositórios de cada funcionalidade.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val reviewDao: ReviewDao,
    private val focusSessionDao: FocusSessionDao,
    private val subjectDao: SubjectDao,
    private val deckDao: DeckDao,
) {
    /** Recalcula sempre que uma das tabelas muda (ex.: um pomodoro termina com a tela aberta). */
    fun observeStats(days: Int): Flow<StudyStats> = flow {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(days - 1L).atStartOfDay(zone).toInstant().toEpochMilli()

        emitAll(
            combine(
                focusSessionDao.observeSince(from),
                reviewDao.observeSince(from),
                subjectDao.observeAll(),
                deckDao.observeAllIncludingDeleted(),
            ) { sessions, reviews, subjects, decks ->
                StatsCalculator.compute(
                    today = today,
                    days = days,
                    zone = zone,
                    sessions = sessions,
                    reviews = reviews,
                    subjects = subjects,
                    deckNames = decks.associate { it.id to it.name },
                )
            },
        )
    }
}
