package com.seunome.finlo.data.repository

import com.google.gson.Gson
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.dao.CategoryExpenseTotal
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.database.entity.TransactionEntity
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.util.UuidV7
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import com.seunome.finlo.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TransactionRepositoryImpl(
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) : TransactionRepository {

    override fun getFilteredTransactionsFlow(
        accountId: String?,
        categoryId: String?,
        type: TransactionType?,
        startDateEpoch: Long?,
        endDateEpoch: Long?
    ): Flow<List<Transaction>> {
        return database.transactionDao().getFilteredTransactionsFlow(
            accountId = accountId,
            categoryId = categoryId,
            type = type?.name,
            startDateEpoch = startDateEpoch,
            endDateEpoch = endDateEpoch
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> {
        return database.transactionDao().searchTransactionsFlow(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentTransactionsFlow(limit: Int): Flow<List<Transaction>> {
        return database.transactionDao().getRecentTransactionsFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return database.transactionDao().getTransactionById(id)?.toDomain()
    }

    override suspend fun createTransaction(transaction: Transaction) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val txId = if (transaction.id.isBlank()) UuidV7.generate(now) else transaction.id
        val entity = TransactionEntity.fromDomain(transaction.copy(id = txId, updatedAt = now))

        database.transactionDao().upsertTransaction(entity)
        enqueueSync("transaction", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun createInstallmentTransactions(
        baseTransaction: Transaction,
        totalInstallments: Int,
        totalAmountCents: Long
    ) {
        if (totalInstallments <= 0) return

        val baseCentsPerInstallment = totalAmountCents / totalInstallments
        val remainder = totalAmountCents % totalInstallments
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = baseTransaction.dateEpochMs

        for (i in 1..totalInstallments) {
            val amountForThisInstallment = if (i == 1) {
                baseCentsPerInstallment + remainder
            } else {
                baseCentsPerInstallment
            }

            val txId = UuidV7.generate(calendar.timeInMillis)
            val installmentDate = calendar.timeInMillis

            val installmentTx = baseTransaction.copy(
                id = txId,
                amountCents = amountForThisInstallment,
                dateEpochMs = installmentDate,
                installmentNumber = i,
                totalInstallments = totalInstallments,
                updatedAt = now
            )

            val entity = TransactionEntity.fromDomain(installmentTx)
            database.transactionDao().upsertTransaction(entity)
            enqueueSync("transaction", txId, "upsert", entity, now, deviceId, null)

            calendar.add(Calendar.MONTH, 1)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = TransactionEntity.fromDomain(transaction.copy(updatedAt = now))

        database.transactionDao().upsertTransaction(entity)
        enqueueSync("transaction", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun deleteTransaction(id: String) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        database.transactionDao().softDeleteTransaction(id, deletedAt = now, updatedAt = now)
        enqueueSync("transaction", id, "delete", null, now, deviceId, now)
    }

    override fun getMonthlyIncomeFlow(startEpoch: Long, endEpoch: Long): Flow<Long> {
        return database.transactionDao().getTotalIncomeFlow(startEpoch, endEpoch)
    }

    override fun getMonthlyExpenseFlow(startEpoch: Long, endEpoch: Long): Flow<Long> {
        return database.transactionDao().getTotalExpenseFlow(startEpoch, endEpoch)
    }

    override fun getExpensesByCategoryFlow(startEpoch: Long, endEpoch: Long): Flow<List<CategoryExpenseTotal>> {
        return database.transactionDao().getExpensesByCategoryFlow(startEpoch, endEpoch)
    }

    override fun getCategorySpentInPeriodFlow(categoryId: String, startEpoch: Long, endEpoch: Long): Flow<Long> {
        return database.transactionDao().getCategorySpentInPeriodFlow(categoryId, startEpoch, endEpoch)
    }

    private suspend fun enqueueSync(
        entityType: String,
        entityId: String,
        op: String,
        payload: Any?,
        updatedAt: Long,
        deviceId: String,
        deletedAt: Long?
    ) {
        val payloadJson = if (payload != null) gson.toJson(payload) else "{}"
        database.syncChangeDao().enqueue(
            SyncChangeEntity(
                id = UuidV7.generate(),
                entityType = entityType,
                entityId = entityId,
                operation = op,
                payloadJson = payloadJson,
                clientUpdated = updatedAt,
                deviceId = deviceId,
                deletedAt = deletedAt
            )
        )
    }
}
