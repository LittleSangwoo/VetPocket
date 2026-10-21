package com.vetpocket.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vetpocket.app.data.db.ProcedureType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Планирует локальные уведомления о предстоящих прививках и обработках через AlarmManager. */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(eventId: Long, petName: String, procedure: ProcedureType, date: LocalDate) {
        val triggerAt = date.atTime(LocalTime.of(10, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        val title = "Пора: ${procedure.title}"
        val text = if (petName.isBlank()) {
            "Сегодня запланирована процедура «${procedure.title}»."
        } else {
            "$petName: сегодня запланирована процедура «${procedure.title}»."
        }
        // setAndAllowWhileIdle не требует разрешения SCHEDULE_EXACT_ALARM: для напоминаний «на день» точность до минут не нужна.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(eventId, title, text),
        )
    }

    fun cancel(eventId: Long) {
        alarmManager.cancel(pendingIntent(eventId, "", ""))
    }

    private fun pendingIntent(eventId: Long, title: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "vaccination_reminders"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о прививках",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Плановые прививки и обработки питомца"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
