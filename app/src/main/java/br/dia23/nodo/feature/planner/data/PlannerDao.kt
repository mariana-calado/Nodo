package br.dia23.nodo.feature.planner.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // --- Metas ---

    @Query("SELECT * FROM weekly_goals WHERE isDeleted = 0 ORDER BY createdAt")
    fun observeGoals(): Flow<List<WeeklyGoalEntity>>

    @Upsert
    suspend fun upsertGoal(goal: WeeklyGoalEntity)

    @Query("UPDATE weekly_goals SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteGoal(id: String, now: Long = System.currentTimeMillis())

    // --- Provas e prazos ---

    /** Eventos de hoje em diante + prazos atrasados ainda não entregues (esses não podem sumir da lista). */
    @Query(
        "SELECT * FROM planner_events WHERE isDeleted = 0 " +
            "AND (dateEpochDay >= :todayEpochDay OR (type = 'DEADLINE' AND isDone = 0)) " +
            "ORDER BY dateEpochDay, createdAt",
    )
    fun observeRelevantEvents(todayEpochDay: Long): Flow<List<PlannerEventEntity>>

    @Query("SELECT * FROM planner_events WHERE id = :id")
    suspend fun getEvent(id: String): PlannerEventEntity?

    @Upsert
    suspend fun upsertEvent(event: PlannerEventEntity)

    @Query("UPDATE planner_events SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteEvent(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE planner_events SET isDone = :done, updatedAt = :now WHERE id = :id")
    suspend fun setEventDone(id: String, done: Boolean, now: Long = System.currentTimeMillis())
}
