package com.vetpocket.app

import android.content.Context
import com.vetpocket.app.data.PetRepository
import com.vetpocket.app.data.db.AppDatabase
import com.vetpocket.app.data.remote.LlmRepository
import com.vetpocket.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow

/** Простейший ручной DI-контейнер: хватает для MVP и не тянет Hilt/Koin. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val scheduler = ReminderScheduler(context)
    val repository = PetRepository(context, database, scheduler)
    val llm = LlmRepository()

    /** Кнопка SOS на главной выставляет флаг; экран карты включает «Открыто прямо сейчас» и сбрасывает его. */
    val sosRequested = MutableStateFlow(false)
}
