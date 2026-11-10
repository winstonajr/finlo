package com.seunome.finlo.domain.repository

import com.seunome.finlo.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccountsFlow(): Flow<List<Account>>
    fun getAccountBalanceFlow(accountId: String): Flow<Long>
    suspend fun getAccountById(id: String): Account?
    suspend fun createAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(id: String, archived: Boolean)
    suspend fun deleteAccount(id: String)
}
