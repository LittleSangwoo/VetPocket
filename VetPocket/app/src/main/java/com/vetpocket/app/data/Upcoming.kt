package com.vetpocket.app.data

import com.vetpocket.app.data.db.MedicalEventEntity
import com.vetpocket.app.data.db.ProcedureType
import java.time.LocalDate

data class UpcomingEvent(
    val procedure: ProcedureType,
    val date: LocalDate,
    val drugName: String,
)

/**
 * Для каждой процедуры берём самую свежую запись; если по ней включено напоминание —
 * показываем дату следующей процедуры. Результат отсортирован по ближайшей дате.
 */
fun computeUpcoming(events: List<MedicalEventEntity>): List<UpcomingEvent> =
    events.groupBy { it.procedure }
        .mapNotNull { (_, list) ->
            val latest = list.maxByOrNull { it.dateEpochDay } ?: return@mapNotNull null
            val next = latest.nextDateEpochDay
            if (latest.isScheduledReminder && next != null) {
                UpcomingEvent(latest.procedure, LocalDate.ofEpochDay(next), latest.drugName)
            } else {
                null
            }
        }
        .sortedBy { it.date }
