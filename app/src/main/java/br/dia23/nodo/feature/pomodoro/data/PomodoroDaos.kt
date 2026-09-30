package br.dia23.nodo.feature.pomodoro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 0 AND startedAt >= :from ORDER BY startedAt")
    fun observeSince(from: Long): Flow<List<FocusSessionEntity>>

    /** Soma do tempo focado desde `from` (0 quando não há sessões: COALESCE troca o NULL do SUM por 0). */
    @Query("SELECT COALESCE(SUM(focusedMs), 0) FROM focus_sessions WHERE isDeleted = 0 AND startedAt >= :from")
    fun observeFocusedMsSince(from: Long): Flow<Long>
}
