package com.vetpocket.app

import android.app.Application
import com.vetpocket.app.reminders.ReminderScheduler

class VetPocketApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
    }
}
