package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seunome.finlo.domain.model.Account
import com.seunome.finlo.domain.model.AccountType
import com.seunome.finlo.domain.model.SyncStatus

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val colorHex: String,
    val isArchived: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
) {
    fun toDomain(balanceCents: Long = 0L) = Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        currency = currency,
        colorHex = colorHex,
        isArchived = isArchived,
        balanceCents = balanceCents,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(account: Account) = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type.name,
            currency = account.currency,
            colorHex = account.colorHex,
            isArchived = account.isArchived,
            updatedAt = account.updatedAt,
            deletedAt = account.deletedAt,
            syncStatus = account.syncStatus.name
        )
    }
}
