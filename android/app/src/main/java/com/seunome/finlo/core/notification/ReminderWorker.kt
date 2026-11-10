package com.seunome.finlo.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.seunome.finlo.core.database.AppDatabase
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val database: AppDatabase? = null
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
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
