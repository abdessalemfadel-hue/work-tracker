package com.abdessalem.worktracker.domain.model

import com.abdessalem.worktracker.data.local.ShiftEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max

object TimeUtils {
    fun grossMillis(shift: ShiftEntity, now: Long = System.currentTimeMillis()): Long =
        max(0L, (shift.clockOut ?: now) - shift.clockIn)

    fun breakMillis(shift: ShiftEntity, now: Long = System.currentTimeMillis()): Long =
        max(0L, shift.totalBreakMillis + if (shift.breakStart != null) now - shift.breakStart else 0L)

    fun netMillis(shift: ShiftEntity, now: Long = System.currentTimeMillis()): Long =
        max(0L, grossMillis(shift, now) - breakMillis(shift, now))

    fun overtimeMillis(shift: ShiftEntity, targetHours: Double, now: Long = System.currentTimeMillis()): Long =
        max(0L, netMillis(shift, now) - (targetHours * 3_600_000.0).toLong())

    fun expectedFinishMillis(
        shift: ShiftEntity,
        targetHours: Double,
        expectedBreakMinutes: Int,
        now: Long = System.currentTimeMillis()
    ): Long {
        val target = (targetHours * 3_600_000.0).toLong()
        val breakTarget = max(expectedBreakMinutes * 60_000L, breakMillis(shift, now))
        return shift.clockIn + target + breakTarget
    }

    fun formatDuration(millis: Long, withSeconds: Boolean = false): String {
        val totalSeconds = max(0L, millis / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (withSeconds) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", hours, minutes)
        }
    }

    fun startOfDayMillis(timeMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun startOfWeekMillis(timeMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalDate()
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun startOfMonthMillis(timeMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalDate().withDayOfMonth(1)
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun endOfMonthMillis(timeMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalDate().withDayOfMonth(1).plusMonths(1)
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun epochDay(timeMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalDate().toEpochDay()

    fun dateFromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun minutesOfDay(timeMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val time = Instant.ofEpochMilli(timeMillis).atZone(zoneId).toLocalTime()
        return time.hour * 60 + time.minute
    }
}
