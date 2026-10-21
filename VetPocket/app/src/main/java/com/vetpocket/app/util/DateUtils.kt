package com.vetpocket.app.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val RuLocale: Locale = Locale.forLanguageTag("ru")
val FullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RuLocale)
val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM", RuLocale)

/** Склонение: 1 год, 2 года, 5 лет. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val m100 = n % 100
    val m10 = n % 10
    return when {
        m100 in 11..14 -> many
        m10 == 1 -> one
        m10 in 2..4 -> few
        else -> many
    }
}

/** Возраст питомца: «5 лет 2 мес.», «8 мес.», «12 дней». */
fun formatAge(birth: LocalDate, today: LocalDate = LocalDate.now()): String {
    if (birth.isAfter(today)) return "—"
    val p = Period.between(birth, today)
    val years = p.years
    val months = p.months
    return when {
        years > 0 && months > 0 -> "$years ${plural(years, "год", "года", "лет")} $months мес."
        years > 0 -> "$years ${plural(years, "год", "года", "лет")}"
        months > 0 -> "$months мес."
        else -> {
            val days = ChronoUnit.DAYS.between(birth, today).toInt()
            "$days ${plural(days, "день", "дня", "дней")}"
        }
    }
}

/** Таймер до события: «через 5 дней», «через 2 месяца», «просрочено на 3 дня». */
fun untilText(target: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days < 0 -> "просрочено на ${-days} ${plural(-days, "день", "дня", "дней")}"
        days == 0 -> "сегодня"
        days == 1 -> "завтра"
        days < 30 -> "через $days ${plural(days, "день", "дня", "дней")}"
        else -> {
            val months = maxOf(1, ChronoUnit.MONTHS.between(today, target).toInt())
            if (months < 12) {
                "через $months ${plural(months, "месяц", "месяца", "месяцев")}"
            } else {
                val years = months / 12
                "через $years ${plural(years, "год", "года", "лет")}"
            }
        }
    }
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} м" else String.format(RuLocale, "%.1f км", meters / 1000.0)

fun formatWeight(kg: Float): String = String.format(RuLocale, "%.2f", kg).trimEnd('0').trimEnd(',')
