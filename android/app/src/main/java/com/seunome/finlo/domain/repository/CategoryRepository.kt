package com.seunome.finlo.domain.repository

import com.seunome.finlo.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesFlow(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun createCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String, fallbackCategoryId: String)
}
