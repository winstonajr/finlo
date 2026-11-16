package com.seunome.finlo.domain.repository

import com.seunome.finlo.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRuleRepository {
    fun getActiveRulesFlow(): Flow<List<RecurringRule>>
    suspend fun getRuleById(id: String): RecurringRule?
    suspend fun createRule(rule: RecurringRule): String
    suspend fun updateRule(rule: RecurringRule)
    suspend fun deleteRule(id: String)
    suspend fun processDueRecurringRules(): Int
}
