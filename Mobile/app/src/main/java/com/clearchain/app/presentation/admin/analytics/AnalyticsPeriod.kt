package com.clearchain.app.presentation.admin.analytics

import android.content.res.Resources
import androidx.annotation.StringRes
import com.clearchain.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** In the order the filter chips show them. */
enum class StatsPeriodType(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

/**
 * A calendar period the stats are shown for: the current day, week (Monday to Sunday)
 * or month when [offset] is 0, and the one [offset] periods before it otherwise.
 * The current period runs only up to today. Days are UTC, which is how the server counts.
 */
data class AnalyticsPeriod(
    val type: StatsPeriodType = StatsPeriodType.ALL,
    val offset: Int = 0
) {
    /** First and last day, or null for all time. */
    fun range(today: LocalDate = utcToday()): Pair<LocalDate, LocalDate>? {
        val start = when (type) {
            StatsPeriodType.DAY -> today.minusDays(offset.toLong())
            StatsPeriodType.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(offset.toLong())
            StatsPeriodType.MONTH -> today.withDayOfMonth(1).minusMonths(offset.toLong())
            StatsPeriodType.ALL -> return null
        }
        val end = when (type) {
            StatsPeriodType.DAY -> start
            StatsPeriodType.WEEK -> start.plusDays(6)
            StatsPeriodType.MONTH -> start.plusMonths(1).minusDays(1)
            StatsPeriodType.ALL -> return null
        }
        return start to minOf(end, today)
    }

    /** Whether the period before this one still holds data, given the first day that has any. */
    fun canGoBack(firstDataDate: LocalDate?, today: LocalDate = utcToday()): Boolean {
        val start = range(today)?.first ?: return false
        return firstDataDate == null || start.isAfter(firstDataDate)
    }

    val canGoForward: Boolean get() = type != StatsPeriodType.ALL && offset > 0

    /** "Today", "Last week", "September 2026"… */
    fun title(res: Resources, today: LocalDate = utcToday()): String {
        val (start, _) = range(today) ?: return res.getString(R.string.preset_all_time)
        return when (type) {
            StatsPeriodType.DAY -> when (offset) {
                0 -> res.getString(R.string.preset_today)
                1 -> res.getString(R.string.period_yesterday)
                else -> start.format(dayFormat())
            }
            StatsPeriodType.WEEK -> when (offset) {
                0 -> res.getString(R.string.preset_this_week)
                1 -> res.getString(R.string.period_last_week)
                else -> res.getString(R.string.period_week_of, start.format(dayFormat()))
            }
            StatsPeriodType.MONTH -> start.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            StatsPeriodType.ALL -> res.getString(R.string.preset_all_time)
        }
    }

    /** The days covered, e.g. "1 Sep 2026 – 30 Sep 2026"; null for a single day or all time. */
    fun rangeText(res: Resources, today: LocalDate = utcToday()): String? {
        val (start, end) = range(today) ?: return null
        if (start == end) return null
        return res.getString(R.string.stats_period_range, start.format(dayFormat()), end.format(dayFormat()))
    }

    /** Title and days together, for the screen subtitle and the PDF. */
    fun description(res: Resources, today: LocalDate = utcToday()): String {
        if (type == StatsPeriodType.ALL) return res.getString(R.string.stats_all_time_subtitle)
        val days = rangeText(res, today)
        val title = title(res, today)
        return if (days == null) title else "$title · $days"
    }

    companion object {
        fun utcToday(): LocalDate = LocalDate.now(ZoneOffset.UTC)
        private fun dayFormat() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    }
}
