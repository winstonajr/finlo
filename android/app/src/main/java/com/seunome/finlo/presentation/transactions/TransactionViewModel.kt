package com.seunome.finlo.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.util.UuidV7
import com.seunome.finlo.data.repository.RecurringRuleRepositoryImpl
import com.seunome.finlo.domain.model.Account
import com.seunome.finlo.domain.model.Category
import com.seunome.finlo.domain.model.Frequency
import com.seunome.finlo.domain.model.RecurringRule
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType
import com.seunome.finlo.domain.repository.AccountRepository
import com.seunome.finlo.domain.repository.CategoryRepository
import com.seunome.finlo.domain.repository.RecurringRuleRepository
import com.seunome.finlo.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRuleRepository: RecurringRuleRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            recurringRuleRepository.processDueRecurringRules()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.getFilteredTransactionsFlow(),
        accountRepository.getAccountsFlow(),
        categoryRepository.getCategoriesFlow(),
        _searchQuery,
        _selectedType,
        _selectedAccountId,
        _selectedCategoryId
    ) { txList, accounts, categories, query, type, accId, catId ->
        var filtered = txList
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.description.contains(query, ignoreCase = true) }
        }
        if (type != null) {
            filtered = filtered.filter { it.type == type }
        }
        if (accId != null) {
            filtered = filtered.filter { it.accountId == accId || it.targetAccountId == accId }
        }
        if (catId != null) {
            filtered = filtered.filter { it.categoryId == catId }
        }

        TransactionListUiState(
            transactions = filtered,
            accounts = accounts,
            categories = categories,
            searchQuery = query,
            selectedType = type,
            selectedAccountId = accId,
            selectedCategoryId = catId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionListUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    suspend fun getTransactionById(id: String): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun saveTransaction(
        id: String? = null,
        type: TransactionType,
        amountCents: Long,
        accountId: String,
        targetAccountId: String? = null,
        categoryId: String,
        description: String,
        dateEpochMs: Long,
        receiptAttachment: String? = null,
        isInstallment: Boolean = false,
        totalInstallments: Int = 1,
        isRecurring: Boolean = false,
        recurringFrequency: Frequency = Frequency.MONTHLY
    ) {
        viewModelScope.launch {
            val txId = id ?: UuidV7.generate(dateEpochMs)
            var ruleId: String? = null

            if (isRecurring && id == null) {
                val cal = Calendar.getInstance().apply { timeInMillis = dateEpochMs }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val nextDue = RecurringRuleRepositoryImpl.calculateNextDueDate(dateEpochMs, recurringFrequency, dayOfMonth)

                ruleId = recurringRuleRepository.createRule(
                    RecurringRule(
                        id = UuidV7.generate(),
                        accountId = accountId,
                        categoryId = categoryId,
                        type = type,
                        amountCents = amountCents,
                        frequency = recurringFrequency,
                        dayOfMonth = dayOfMonth,
                        nextDueDateEpochMs = nextDue
                    )
                )
            }

            val baseTx = Transaction(
                id = txId,
                accountId = accountId,
                targetAccountId = targetAccountId,
                categoryId = categoryId,
                type = type,
                amountCents = amountCents,
                dateEpochMs = dateEpochMs,
                description = description,
                receiptAttachment = receiptAttachment,
                recurringRuleId = ruleId
            )

            if (isInstallment && totalInstallments > 1 && id == null) {
                transactionRepository.createInstallmentTransactions(
                    baseTransaction = baseTx,
                    totalInstallments = totalInstallments,
                    totalAmountCents = amountCents
                )
            } else {
                if (id == null) {
                    transactionRepository.createTransaction(baseTx)
                } else {
                    transactionRepository.updateTransaction(baseTx)
                }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }
}
