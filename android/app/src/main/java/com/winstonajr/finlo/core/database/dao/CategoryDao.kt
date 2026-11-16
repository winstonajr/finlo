package com.seunome.finlo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.seunome.finlo.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteCategory(id: String, deletedAt: Long, updatedAt: Long, syncStatus: String = "PENDING")

    @Query("SELECT id FROM transactions WHERE categoryId = :categoryId AND deletedAt IS NULL")
    suspend fun getTransactionIdsByCategoryId(categoryId: String): List<String>

    @Query("UPDATE transactions SET categoryId = :newCategoryId, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE categoryId = :oldCategoryId AND deletedAt IS NULL")
    suspend fun reassignTransactions(oldCategoryId: String, newCategoryId: String, updatedAt: Long)

    @Transaction
    suspend fun reassignAndSoftDelete(oldCategoryId: String, newCategoryId: String, timestamp: Long) {
        reassignTransactions(oldCategoryId, newCategoryId, timestamp)
        softDeleteCategory(oldCategoryId, timestamp, timestamp, "PENDING")
    }

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
