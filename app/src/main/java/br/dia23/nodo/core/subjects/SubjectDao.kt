package br.dia23.nodo.core.subjects

import androidx.room.Dao
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
