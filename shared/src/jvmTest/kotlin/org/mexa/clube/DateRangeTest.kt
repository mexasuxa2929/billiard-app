package org.mexa.clube

import org.mexa.clube.domain.startOfDay
import org.mexa.clube.domain.startOfMonth
import org.mexa.clube.ui.components.DateRange
import org.mexa.clube.ui.components.DateRangePreset
import org.mexa.clube.ui.components.allRange
import org.mexa.clube.ui.components.label
import org.mexa.clube.ui.components.monthRange
import org.mexa.clube.ui.components.rangeForPreset
import org.mexa.clube.ui.components.todayRange
import org.mexa.clube.ui.components.weekRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sana oralig'i yordamchilari: barcha hisob sahifalari shu qoidalarga tayanadi. */
class DateRangeTest {

    private val now = 1_800_000_000_000L // tayyor (fixed) vaqt

    @Test
    fun todayRangeCoversExactlyOneDay() {
        val dayStart = startOfDay(now)
        val range = todayRange(now)
        assertEquals(dayStart, range.from)
        assertEquals(24L * 3600 * 1000, range.toExclusive - range.from)
        assertTrue(range.contains(dayStart))
        assertTrue(range.contains(dayStart + 23 * 3600 * 1000))
        assertFalse(range.contains(dayStart - 1))
        assertFalse(range.contains(range.toExclusive), "tugash chegarasi kirmaydi")
    }

    @Test
    fun weekAndMonthRangesContainNow() {
        assertTrue(weekRange(now).contains(now))
        assertTrue(monthRange(now).contains(now))
        assertEquals(startOfMonth(now), monthRange(now).from)
    }

    @Test
    fun allRangeIncludesFutureWhenAsked() {
        // "Jami" odatda bugungi kun oxirigacha...
        assertFalse(allRange(now).contains(now + 30L * 24 * 3600 * 1000))
        // ...zametkalarda esa kelajakdagi yozuvlar ham ko'rinadi.
        assertTrue(allRange(now, includeFuture = true).contains(now + 30L * 24 * 3600 * 1000))
    }

    @Test
    fun presetMappingAndLabels() {
        assertEquals(todayRange(now), rangeForPreset(DateRangePreset.TODAY, now))
        assertEquals(weekRange(now), rangeForPreset(DateRangePreset.WEEK, now))
        assertEquals(monthRange(now), rangeForPreset(DateRangePreset.MONTH, now))
        assertNull(rangeForPreset(DateRangePreset.CUSTOM, now))
        assertEquals("Barcha vaqt", DateRange(0L, now).label(DateRangePreset.ALL))
    }
}
