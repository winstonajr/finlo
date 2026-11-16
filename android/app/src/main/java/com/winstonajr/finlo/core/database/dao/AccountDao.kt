package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seunome.finlo.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

data class AccountWithCalculatedBalance(
    val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val colorHex: String,
    val isArchived: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String,
    val calculatedBalanceCents: Long
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getActiveAccountsFlow(): Flow<List<AccountEntity>>

    @Query("""
        SELECT a.id, a.name, a.type, a.currency, a.colorHex, a.isArchived, a.updatedAt, a.deletedAt, a.syncStatus,
        COALESCE(SUM(
            CASE 
                WHEN t.type = 'INCOME' THEN t.amountCents 
                WHEN t.type = 'EXPENSE' THEN -t.amountCents 
                WHEN t.type = 'TRANSFER' AND t.targetAccountId = a.id THEN t.amountCents 
                WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amountCents 
                ELSE 0 
            END
        ), 0) AS calculatedBalanceCents
        FROM accounts a
        LEFT JOIN transactions t ON (t.accountId = a.id OR t.targetAccountId = a.id) AND t.deletedAt IS NULL
        WHERE a.deletedAt IS NULL
        GROUP BY a.id
        ORDER BY a.name ASC
    """)
    fun getActiveAccountsWithBalanceFlow(): Flow<List<AccountWithCalculatedBalance>>

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN type = 'INCOME' THEN amountCents 
                WHEN type = 'EXPENSE' THEN -amountCents 
                WHEN type = 'TRANSFER' AND targetAccountId = :accountId THEN amountCents 
                WHEN type = 'TRANSFER' AND accountId = :accountId THEN -amountCents 
                ELSE 0 
            END
        ), 0) 
        FROM transactions 
        WHERE (accountId = :accountId OR targetAccountId = :accountId) AND deletedAt IS NULL
    """)
    fun getAccountBalanceFlow(accountId: String): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN type = 'INCOME' THEN amountCents 
                WHEN type = 'EXPENSE' THEN -amountCents 
                WHEN type = 'TRANSFER' AND targetAccountId = :accountId THEN amountCents 
                WHEN type = 'TRANSFER' AND accountId = :accountId THEN -amountCents 
                ELSE 0 
            END
        ), 0) 
        FROM transactions 
        WHERE (accountId = :accountId OR targetAccountId = :accountId) AND deletedAt IS NULL
    """)
    suspend fun getAccountBalance(accountId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteAccount(id: String, deletedAt: Long, updatedAt: Long, syncStatus: String = "PENDING")

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
