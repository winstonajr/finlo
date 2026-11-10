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

    private val startOfMonth: Long
    private val endOfMonth: Long
    private val prevMonthStart: Long
    private val prevMonthEnd: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startOfMonth = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        endOfMonth = cal.timeInMillis - 1

        cal.add(Calendar.MONTH, -2)
        prevMonthStart = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        prevMonthEnd = cal.timeInMillis - 1
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAccountsFlow(),
        transactionRepository.getMonthlyIncomeFlow(startOfMonth, endOfMonth),
        transactionRepository.getMonthlyExpenseFlow(startOfMonth, endOfMonth),
        transactionRepository.getMonthlyIncomeFlow(prevMonthStart, prevMonthEnd),
        transactionRepository.getMonthlyExpenseFlow(prevMonthStart, prevMonthEnd),
        transactionRepository.getRecentTransactionsFlow(5),
        transactionRepository.getExpensesByCategoryFlow(startOfMonth, endOfMonth),
        categoryRepository.getCategoriesFlow(),
        syncManager.syncState
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val accounts = args[0] as List<com.seunome.finlo.domain.model.Account>
        val income = args[1] as Long
        val expense = args[2] as Long
        val prevIncome = args[3] as Long
        val prevExpense = args[4] as Long
        @Suppress("UNCHECKED_CAST")
        val recent = args[5] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val categoryTotals = args[6] as List<com.seunome.finlo.core.database.dao.CategoryExpenseTotal>
        @Suppress("UNCHECKED_CAST")
        val categories = args[7] as List<com.seunome.finlo.domain.model.Category>
        val syncState = args[8] as SyncState

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
            MonthEvolutionData("Mês Ant.", prevIncome, prevExpense),
            MonthEvolutionData("Mês Atual", income, expense)
        )

        DashboardUiState(
            totalBalanceCents = totalBalance,
            monthlyIncomeCents = income,
            monthlyExpenseCents = expense,
            previousMonthExpenseCents = prevExpense,
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
