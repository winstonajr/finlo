package com.seunome.finlo.domain.repository

import com.seunome.finlo.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalsFlow(): Flow<List<Goal>>
    suspend fun getGoalById(id: String): Goal?
    suspend fun createGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun updateGoalProgress(id: String, newAmountCents: Long)
    suspend fun deleteGoal(id: String)
}
