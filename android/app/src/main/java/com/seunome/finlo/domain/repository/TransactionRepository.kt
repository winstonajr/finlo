package com.seunome.finlo.domain.repository

import com.seunome.finlo.core.database.dao.CategoryExpenseTotal
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getFilteredTransactionsFlow(
        accountId: String? = null,
        categoryId: String? = null,
        type: TransactionType? = null,
        startDateEpoch: Long? = null,
        endDateEpoch: Long? = null
    ): Flow<List<Transaction>>

    fun searchTransactionsFlow(query: String): Flow<List<Transaction>>
    fun getRecentTransactionsFlow(limit: Int = 10): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun createTransaction(transaction: Transaction)
    suspend fun createInstallmentTransactions(
        baseTransaction: Transaction,
        totalInstallments: Int,
        totalAmountCents: Long
    )
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    fun getMonthlyIncomeFlow(startEpoch: Long, endEpoch: Long): Flow<Long>
    fun getMonthlyExpenseFlow(startEpoch: Long, endEpoch: Long): Flow<Long>
    fun getExpensesByCategoryFlow(startEpoch: Long, endEpoch: Long): Flow<List<CategoryExpenseTotal>>
    fun getCategorySpentInPeriodFlow(categoryId: String, startEpoch: Long, endEpoch: Long): Flow<Long>
}
