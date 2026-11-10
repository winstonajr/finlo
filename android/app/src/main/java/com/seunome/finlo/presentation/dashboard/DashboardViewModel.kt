package com.seunome.finlo.presentation.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.sync.SyncManager
import com.seunome.finlo.core.sync.SyncState
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.repository.AccountRepository
import com.seunome.finlo.domain.repository.CategoryRepository
import com.seunome.finlo.domain.repository.TransactionRepository
import com.seunome.finlo.presentation.components.charts.DonutSlice
import com.seunome.finlo.presentation.components.charts.MonthEvolutionData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val totalBalanceCents: Long = 0L,
    val monthlyIncomeCents: Long = 0L,
    val monthlyExpenseCents: Long = 0L,
    val previousMonthExpenseCents: Long = 0L,
    val recentTransactions: List<Transaction> = emptyList(),
    val categorySlices: List<DonutSlice> = emptyList(),
    val evolutionData: List<MonthEvolutionData> = emptyList(),
    val syncState: SyncState = SyncState.Idle
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val startOfMonth: Long
    private val endOfMonth: Long

    init {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        endOfMonth = calendar.timeInMillis - 1
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAccountsFlow(),
        transactionRepository.getMonthlyIncomeFlow(startOfMonth, endOfMonth),
        transactionRepository.getMonthlyExpenseFlow(startOfMonth, endOfMonth),
        transactionRepository.getRecentTransactionsFlow(5),
        transactionRepository.getExpensesByCategoryFlow(startOfMonth, endOfMonth),
        categoryRepository.getCategoriesFlow(),
        syncManager.syncState
    ) { accounts, income, expense, recent, categoryTotals, categories, syncState ->

        val totalBalance = accounts.sumOf { it.balanceCents }

        val categoryMap = categories.associateBy { it.id }
        val slices = categoryTotals.map { catTotal ->
            val cat = categoryMap[catTotal.categoryId]
            val color = try {
                Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#4CAF50"))
            } catch (e: Exception) {
                Color.Gray
            }

            DonutSlice(
                categoryName = cat?.name ?: "Outros",
                amountCents = catTotal.totalCents,
                color = color
            )
        }

        val evolution = listOf(
            MonthEvolutionData("Mês Ant.", income * 9 / 10, expense * 8 / 10),
            MonthEvolutionData("Mês Atual", income, expense)
        )

        DashboardUiState(
            totalBalanceCents = totalBalance,
            monthlyIncomeCents = income,
            monthlyExpenseCents = expense,
            recentTransactions = recent,
            categorySlices = slices,
            evolutionData = evolution,
            syncState = syncState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun triggerSync() {
        viewModelScope.launch {
            syncManager.sync()
        }
    }
}
