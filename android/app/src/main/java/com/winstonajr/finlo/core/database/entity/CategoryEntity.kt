package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seunome.finlo.domain.model.Category
import com.seunome.finlo.domain.model.SyncStatus

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val budgetCents: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
) {
    fun toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        budgetCents = budgetCents,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(category: Category) = CategoryEntity(
            id = category.id,
            name = category.name,
            icon = category.icon,
            colorHex = category.colorHex,
            budgetCents = category.budgetCents,
            updatedAt = category.updatedAt,
            deletedAt = category.deletedAt,
            syncStatus = category.syncStatus.name
        )
    }
}
