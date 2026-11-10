package com.seunome.finlo

import android.app.Application
import com.seunome.finlo.core.notification.ReminderWorker
import com.seunome.finlo.core.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedulePeriodicSync(this)
        ReminderWorker.scheduleDailyReminders(this)
    }
}
