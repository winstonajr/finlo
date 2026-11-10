package com.seunome.finlo.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.domain.model.Account
import com.seunome.finlo.domain.model.AccountType
import com.seunome.finlo.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val totalNetWorthCents: Long = 0L
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<AccountListUiState> = accountRepository.getAccountsFlow()
        .map { list ->
            AccountListUiState(
                accounts = list,
                totalNetWorthCents = list.sumOf { it.balanceCents }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountListUiState())

    fun saveAccount(id: String?, name: String, type: AccountType, colorHex: String) {
        viewModelScope.launch {
            val account = Account(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                type = type,
                colorHex = colorHex
            )
            if (id == null) {
                accountRepository.createAccount(account)
            } else {
                accountRepository.updateAccount(account)
            }
        }
    }

    fun archiveAccount(id: String, archived: Boolean) {
        viewModelScope.launch {
            accountRepository.archiveAccount(id, archived)
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(id)
        }
    }
}
