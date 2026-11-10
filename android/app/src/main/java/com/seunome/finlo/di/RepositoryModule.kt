package com.seunome.finlo.di

import android.content.Context
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.export.BackupManager
import com.seunome.finlo.core.network.ApiService
import com.seunome.finlo.core.security.PinManager
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.sync.SyncManager
import com.seunome.finlo.data.repository.AccountRepositoryImpl
import com.seunome.finlo.data.repository.CategoryRepositoryImpl
import com.seunome.finlo.data.repository.GoalRepositoryImpl
import com.seunome.finlo.data.repository.TransactionRepositoryImpl
import com.seunome.finlo.domain.repository.AccountRepository
import com.seunome.finlo.domain.repository.CategoryRepository
import com.seunome.finlo.domain.repository.GoalRepository
import com.seunome.finlo.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun providePinManager(secureStorage: SecureStorage): PinManager {
        return PinManager(secureStorage)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        apiService: ApiService,
        database: AppDatabase,
        secureStorage: SecureStorage
    ): SyncManager {
        return SyncManager(apiService, database, secureStorage)
    }

    @Provides
    @Singleton
    fun provideBackupManager(database: AppDatabase): BackupManager {
        return BackupManager(database)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        database: AppDatabase,
        secureStorage: SecureStorage
    ): AccountRepository {
        return AccountRepositoryImpl(database, secureStorage)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        database: AppDatabase,
        secureStorage: SecureStorage
    ): CategoryRepository {
        return CategoryRepositoryImpl(database, secureStorage)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        database: AppDatabase,
        secureStorage: SecureStorage
    ): TransactionRepository {
        return TransactionRepositoryImpl(database, secureStorage)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(
        database: AppDatabase,
        secureStorage: SecureStorage
    ): GoalRepository {
        return GoalRepositoryImpl(database, secureStorage)
    }
}
