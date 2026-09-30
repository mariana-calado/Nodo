package br.dia23.nodo.feature.pomodoro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<SubjectEntity>>

    /** Inclui excluídas: as estatísticas ainda precisam do nome de matérias antigas. */
    @Query("SELECT * FROM subjects")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: String): SubjectEntity?

    @Upsert
    suspend fun upsert(subject: SubjectEntity)
}

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
