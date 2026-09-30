package br.dia23.nodo.feature.pomodoro.data

import br.dia23.nodo.feature.pomodoro.domain.FocusRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroRepository @Inject constructor(
    private val focusSessionDao: FocusSessionDao,
) {
    suspend fun saveFocusSession(record: FocusRecord) {
        focusSessionDao.insert(
            FocusSessionEntity(
                subjectId = record.subjectId,
                startedAt = record.startedAt,
                endedAt = record.endedAt,
                focusedMs = record.focusedMs,
                plannedMs = record.plannedMs,
                completed = record.completed,
            ),
        )
    }

    /** Tempo focado hoje (recalculado a cada vez que alguém passa a observar, como no repositório de flashcards). */
    fun observeFocusedToday(): Flow<Long> = flow {
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        emitAll(focusSessionDao.observeFocusedMsSince(startOfToday))
    }
}
