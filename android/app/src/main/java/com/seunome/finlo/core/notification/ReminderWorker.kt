package com.seunome.finlo.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.domain.repository.RecurringRuleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun appDatabase(): AppDatabase
    fun recurringRuleRepository(): RecurringRuleRepository
    fun secureStorage(): SecureStorage
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val injectedDatabase: AppDatabase? = null,
    private val injectedRecurringRepo: RecurringRuleRepository? = null,
    private val injectedSecureStorage: SecureStorage? = null
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = if (injectedDatabase == null || injectedRecurringRepo == null || injectedSecureStorage == null) {
                try {
                    EntryPointAccessors.fromApplication(
                        applicationContext,
                        ReminderWorkerEntryPoint::class.java
                    )
                } catch (e: Exception) {
                    null
                }
            } else null

            val database = injectedDatabase ?: entryPoint?.appDatabase()
            val recurringRepo = injectedRecurringRepo ?: entryPoint?.recurringRuleRepository()

            recurringRepo?.processDueRecurringRules()

            val notificationHelper = NotificationHelper(applicationContext)

            if (database != null) {
                val twoDaysAhead = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
                val dueRules = database.recurringRuleDao().getDueRules(twoDaysAhead)

                for (rule in dueRules) {
                    val amountFormatted = "R$ " + "%.2f".format(rule.amountCents / 100.0)
                    notificationHelper.showBillReminder("Pagamento agendado", amountFormatted)
                }
            }

            notificationHelper.showDailyReminder()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "finlo_daily_reminders"

        fun scheduleDailyReminders(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
