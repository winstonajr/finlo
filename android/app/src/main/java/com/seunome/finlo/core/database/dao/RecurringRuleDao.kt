package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seunome.finlo.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL AND active = 1 ORDER BY nextDueDateEpochMs ASC")
    fun getActiveRulesFlow(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE deletedAt IS NULL AND active = 1 AND nextDueDateEpochMs <= :currentTimeEpochMs")
    suspend fun getDueRules(currentTimeEpochMs: Long): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun updateRule(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: String): RecurringRuleEntity?

    @Query("UPDATE recurring_rules SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteRule(id: String, deletedAt: Long, updatedAt: Long, syncStatus: String = "PENDING")

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}
