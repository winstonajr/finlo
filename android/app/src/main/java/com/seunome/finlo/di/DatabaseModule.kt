package com.seunome.finlo.di

import android.content.Context
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.dao.AccountDao
import com.seunome.finlo.core.database.dao.CategoryDao
import com.seunome.finlo.core.database.dao.GoalDao
import com.seunome.finlo.core.database.dao.RecurringRuleDao
import com.seunome.finlo.core.database.dao.SyncChangeDao
import com.seunome.finlo.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.buildDatabase(context)
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideRecurringRuleDao(db: AppDatabase): RecurringRuleDao = db.recurringRuleDao()

    @Provides
    fun provideSyncChangeDao(db: AppDatabase): SyncChangeDao = db.syncChangeDao()
}
