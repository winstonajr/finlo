package com.seunome.finlo.presentation.reports

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.export.CsvExporter
import com.seunome.finlo.core.export.PdfExporter
import com.seunome.finlo.domain.repository.AccountRepository
import com.seunome.finlo.domain.repository.CategoryRepository
import com.seunome.finlo.domain.repository.TransactionRepository
import com.seunome.finlo.presentation.components.charts.DonutSlice
import com.seunome.finlo.presentation.components.charts.MonthEvolutionData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReportsUiState(
    val currentMonthIncome: Long = 0L,
    val currentMonthExpense: Long = 0L,
    val previousMonthIncome: Long = 0L,
    val previousMonthExpense: Long = 0L,
    val slices: List<DonutSlice> = emptyList(),
    val evolution: List<MonthEvolutionData> = emptyList(),
    val exportSuccessMessage: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val currentMonthStart: Long
    private val currentMonthEnd: Long
    private val prevMonthStart: Long
    private val prevMonthEnd: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        currentMonthStart = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        currentMonthEnd = cal.timeInMillis - 1

        cal.add(Calendar.MONTH, -2)
        prevMonthStart = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        prevMonthEnd = cal.timeInMillis - 1
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        transactionRepository.getMonthlyIncomeFlow(currentMonthStart, currentMonthEnd),
        transactionRepository.getMonthlyExpenseFlow(currentMonthStart, currentMonthEnd),
        transactionRepository.getMonthlyIncomeFlow(prevMonthStart, prevMonthEnd),
        transactionRepository.getMonthlyExpenseFlow(prevMonthStart, prevMonthEnd),
        transactionRepository.getExpensesByCategoryFlow(currentMonthStart, currentMonthEnd),
        categoryRepository.getCategoriesFlow()
    ) { curIncome, curExpense, prevIncome, prevExpense, catTotals, categories ->

        val catMap = categories.associateBy { it.id }
        val slices = catTotals.map { total ->
            val cat = catMap[total.categoryId]
            val color = try {
                Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#4CAF50"))
            } catch (e: Exception) {
                Color.Gray
            }
            DonutSlice(
                categoryName = cat?.name ?: "Outros",
                amountCents = total.totalCents,
                color = color
            )
        }

        val evolution = listOf(
            MonthEvolutionData("Mês Anterior", prevIncome, prevExpense),
            MonthEvolutionData("Mês Atual", curIncome, curExpense)
        )

        ReportsUiState(
            currentMonthIncome = curIncome,
            currentMonthExpense = curExpense,
            previousMonthIncome = prevIncome,
            previousMonthExpense = prevExpense,
            slices = slices,
            evolution = evolution
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun exportCsv(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val transactions = transactionRepository.getFilteredTransactionsFlow().first()
            val accounts = accountRepository.getAccountsFlow().first().associate { it.id to it.name }
            val categories = categoryRepository.getCategoriesFlow().first().associate { it.id to it.name }

            val csvContent = CsvExporter.exportToCsv(transactions, accounts, categories)

            val file = File(context.cacheDir, "extrato_finlo_${System.currentTimeMillis()}.csv")
            file.writeText(csvContent)
            onResult("CSV exportado com sucesso: ${file.name}")
        }
    }

    fun exportPdf(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val transactions = transactionRepository.getFilteredTransactionsFlow().first()
            val state = uiState.value

            val file = File(context.cacheDir, "relatorio_finlo_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                PdfExporter.exportToPdf(
                    outputStream = out,
                    transactions = transactions,
                    totalIncomeCents = state.currentMonthIncome,
                    totalExpenseCents = state.currentMonthExpense,
                    periodName = monthName
                )
            }
            onResult("PDF gerado com sucesso: ${file.name}")
        }
    }
}
