package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.seunome.finlo.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryExpenseTotal(
    val categoryId: String,
    val totalCents: Long
)

data class MonthlyEvolutionItem(
    val monthYear: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long
)

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        WHERE deletedAt IS NULL 
          AND (:accountId IS NULL OR accountId = :accountId OR targetAccountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:type IS NULL OR type = :type)
          AND (:startDateEpoch IS NULL OR dateEpochMs >= :startDateEpoch)
          AND (:endDateEpoch IS NULL OR dateEpochMs <= :endDateEpoch)
        ORDER BY dateEpochMs DESC
    """)
    fun getFilteredTransactionsFlow(
        accountId: String?,
        categoryId: String?,
        type: String?,
        startDateEpoch: Long?,
        endDateEpoch: Long?
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE deletedAt IS NULL 
          AND description LIKE '%' || :query || '%'
        ORDER BY dateEpochMs DESC
    """)
    fun searchTransactionsFlow(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY dateEpochMs DESC")
    fun getAllActiveTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY dateEpochMs DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(txList: List<TransactionEntity>)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, deletedAt: Long, updatedAt: Long)

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE deletedAt IS NULL AND type = 'INCOME' 
          AND dateEpochMs >= :startEpoch AND dateEpochMs <= :endEpoch
    """)
    fun getTotalIncomeFlow(startEpoch: Long, endEpoch: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE deletedAt IS NULL AND type = 'EXPENSE' 
          AND dateEpochMs >= :startEpoch AND dateEpochMs <= :endEpoch
    """)
    fun getTotalExpenseFlow(startEpoch: Long, endEpoch: Long): Flow<Long>

    @Query("""
        SELECT categoryId, SUM(amountCents) AS totalCents 
        FROM transactions 
        WHERE deletedAt IS NULL AND type = 'EXPENSE' 
          AND dateEpochMs >= :startEpoch AND dateEpochMs <= :endEpoch
        GROUP BY categoryId
    """)
    fun getExpensesByCategoryFlow(startEpoch: Long, endEpoch: Long): Flow<List<CategoryExpenseTotal>>

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions 
        WHERE deletedAt IS NULL AND type = 'EXPENSE' AND categoryId = :categoryId
          AND dateEpochMs >= :startEpoch AND dateEpochMs <= :endEpoch
    """)
    fun getCategorySpentInPeriodFlow(categoryId: String, startEpoch: Long, endEpoch: Long): Flow<Long>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
