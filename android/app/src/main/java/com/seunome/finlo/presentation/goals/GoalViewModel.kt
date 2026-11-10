package com.seunome.finlo.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.util.UuidV7
import com.seunome.finlo.domain.model.Goal
import com.seunome.finlo.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalUiState(
    val goals: List<Goal> = emptyList()
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val uiState: StateFlow<GoalUiState> = goalRepository.getGoalsFlow()
        .map { GoalUiState(goals = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalUiState())

    fun saveGoal(
        id: String?,
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long,
        targetDateEpochMs: Long
    ) {
        viewModelScope.launch {
            val goal = Goal(
                id = id ?: UuidV7.generate(),
                name = name,
                targetAmountCents = targetAmountCents,
                currentAmountCents = currentAmountCents,
                targetDateEpochMs = targetDateEpochMs
            )
            if (id == null) {
                goalRepository.createGoal(goal)
            } else {
                goalRepository.updateGoal(goal)
            }
        }
    }

    fun addProgress(goalId: String, currentAmountCents: Long, addedCents: Long) {
        viewModelScope.launch {
            goalRepository.updateGoalProgress(goalId, currentAmountCents + addedCents)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
        }
    }
}
