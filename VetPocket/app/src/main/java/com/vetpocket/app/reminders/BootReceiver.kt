package com.vetpocket.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vetpocket.app.VetPocketApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** После перезагрузки AlarmManager забывает все будильники — восстанавливаем их из Room. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as VetPocketApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.repository.rescheduleAllReminders()
            } finally {
                pending.finish()
            }
        }
    }
}
