package com.seunome.finlo.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.domain.model.Category
import com.seunome.finlo.domain.repository.CategoryRepository
import com.seunome.finlo.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class CategoryWithBudget(
    val category: Category,
    val spentCents: Long = 0L,
    val budgetCents: Long = 0L,
    val isOver80Percent: Boolean = false,
    val isOver100Percent: Boolean = false
)

data class CategoryUiState(
    val categories: List<CategoryWithBudget> = emptyList()
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val startOfMonth: Long
    private val endOfMonth: Long

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        endOfMonth = calendar.timeInMillis - 1
    }

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getCategoriesFlow(),
        transactionRepository.getExpensesByCategoryFlow(startOfMonth, endOfMonth)
    ) { categories, expenseTotals ->
        val expenseMap = expenseTotals.associate { it.categoryId to it.totalCents }

        val withBudgets = categories.map { cat ->
            val spent = expenseMap[cat.id] ?: 0L
            val budget = cat.budgetCents
            val over80 = budget > 0 && spent >= (budget * 80 / 100)
            val over100 = budget > 0 && spent >= budget

            CategoryWithBudget(
                category = cat,
                spentCents = spent,
                budgetCents = budget,
                isOver80Percent = over80,
                isOver100Percent = over100
            )
        }

        CategoryUiState(categories = withBudgets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    fun saveCategory(id: String?, name: String, colorHex: String, icon: String, budgetCents: Long) {
        viewModelScope.launch {
            val cat = Category(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                colorHex = colorHex,
                icon = icon,
                budgetCents = budgetCents
            )
            if (id == null) {
                categoryRepository.createCategory(cat)
            } else {
                categoryRepository.updateCategory(cat)
            }
        }
    }

    fun deleteCategory(id: String, fallbackCategoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id, fallbackCategoryId)
        }
    }
}
