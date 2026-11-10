package com.seunome.finlo.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.seunome.finlo.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_BILLS = "finlo_channel_bills"
        const val CHANNEL_DAILY = "finlo_channel_daily"
        const val NOTIFICATION_BILLS_ID = 1001
        const val NOTIFICATION_DAILY_ID = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val billsChannel = NotificationChannel(
                CHANNEL_BILLS,
                "Contas a Pagar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes de vencimento de faturas e pagamentos"
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Lembrete Diário de Gastos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembrete para registrar suas despesas do dia"
            }

            manager.createNotificationChannel(billsChannel)
            manager.createNotificationChannel(dailyChannel)
        }
    }

    fun showBillReminder(billDescription: String, amountFormatted: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Conta a vencer: $billDescription")
            .setContentText("Valor: $amountFormatted. Não esqueça de efetuar o pagamento!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_BILLS_ID, notification)
    }

    fun showDailyReminder() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Como foram suas finanças hoje?")
            .setContentText("Mantenha seu orçamento em dia registrando seus gastos de hoje.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_DAILY_ID, notification)
    }
}
