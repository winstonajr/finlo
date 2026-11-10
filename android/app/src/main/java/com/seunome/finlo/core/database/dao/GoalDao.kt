package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seunome.finlo.core.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY targetDateEpochMs ASC")
    fun getActiveGoalsFlow(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentAmountCents = :newAmount, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateGoalProgress(id: String, newAmount: Long, updatedAt: Long)

    @Query("UPDATE goals SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteGoal(id: String, deletedAt: Long, updatedAt: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
