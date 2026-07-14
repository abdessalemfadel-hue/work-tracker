package com.abdessalem.worktracker.domain.model

import com.abdessalem.worktracker.data.local.ShiftEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    @Test fun netTimeSubtractsCompletedBreaks() { val shift = ShiftEntity(clockIn = 0L, clockOut = 9 * 3_600_000L, totalBreakMillis = 60 * 60_000L); assertEquals(8 * 3_600_000L, TimeUtils.netMillis(shift, 9 * 3_600_000L)) }
    @Test fun overtimeStartsAfterScheduledHours() { val shift = ShiftEntity(clockIn = 0L, clockOut = 10 * 3_600_000L); assertEquals(2 * 3_600_000L, TimeUtils.overtimeMillis(shift, 8.0, 10 * 3_600_000L)) }
    @Test fun expectedFinishIncludesTargetBreak() { val start = 1_000L; val shift = ShiftEntity(clockIn = start); assertEquals(start + 8 * 3_600_000L + 60 * 60_000L, TimeUtils.expectedFinishMillis(shift, 8.0, 60, start)) }
    @Test fun durationFormattingIsStable() { assertEquals("08:05", TimeUtils.formatDuration(8 * 3_600_000L + 5 * 60_000L)); assertEquals("08:05:09", TimeUtils.formatDuration(8 * 3_600_000L + 5 * 60_000L + 9_000L, true)) }
}
