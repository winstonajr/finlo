package com.seunome.finlo.core.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.AccountEntity
import com.seunome.finlo.core.database.entity.CategoryEntity
import com.seunome.finlo.core.database.entity.GoalEntity
import com.seunome.finlo.core.database.entity.RecurringRuleEntity
import com.seunome.finlo.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first

data class FinloBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val goals: List<GoalEntity>,
    val recurringRules: List<RecurringRuleEntity>
)

class BackupManager(
    private val database: AppDatabase,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {
    suspend fun createBackupJson(): String {
        val accounts = database.accountDao().getAllAccountsFlow().first()
        val categories = database.categoryDao().getActiveCategoriesFlow().first()
        val transactions = database.transactionDao().getAllActiveTransactionsFlow().first()
        val goals = database.goalDao().getActiveGoalsFlow().first()
        val rules = database.recurringRuleDao().getActiveRulesFlow().first()

        val backup = FinloBackupData(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            goals = goals,
            recurringRules = rules
        )

        return gson.toJson(backup)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val backup = gson.fromJson(jsonString, FinloBackupData::class.java)

            database.accountDao().upsertAccounts(backup.accounts)
            database.categoryDao().upsertCategories(backup.categories)
            database.transactionDao().upsertTransactions(backup.transactions)
            database.goalDao().upsertGoals(backup.goals)
            database.recurringRuleDao().upsertRules(backup.recurringRules)

            true
        } catch (e: Exception) {
            false
        }
    }
}
