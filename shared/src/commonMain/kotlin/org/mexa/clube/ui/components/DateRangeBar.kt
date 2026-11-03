package org.mexa.clube.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.parseUzbekDate
import org.mexa.clube.domain.startOfDay
import org.mexa.clube.domain.startOfMonth
import org.mexa.clube.domain.startOfWeek
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProCard
import kotlin.time.ExperimentalTime

private const val DAY_MS = 24L * 3600 * 1000

/** Hisob-kitob sahifalarida ishlatiladigan sana oralig'i. */
enum class DateRangePreset(val label: String) {
    TODAY("Bugun"),
    WEEK("Hafta"),
    MONTH("Oy"),
    ALL("Jami"),
    CUSTOM("Oraliq")
}

/** [from] — kiritilgan, [toExclusive] — chiqarilgan (shu kundan keyingi yarim tun). */
data class DateRange(val from: Long, val toExclusive: Long) {
    fun contains(millis: Long): Boolean = millis >= from && millis < toExclusive
}

fun todayRange(now: Long = currentMillis()): DateRange {
    val s = startOfDay(now)
    return DateRange(s, s + DAY_MS)
}

fun weekRange(now: Long = currentMillis()): DateRange {
    val s = startOfWeek(now)
    return DateRange(s, s + 7 * DAY_MS)
}

fun monthRange(now: Long = currentMillis()): DateRange {
    val s = startOfMonth(now)
    return DateRange(s, startOfMonth(s + 32 * DAY_MS))
}

/** Barcha vaqt — [includeFuture] bo'lsa, kelajakdagi zametkalar ham ko'rinadi. */
fun allRange(now: Long = currentMillis(), includeFuture: Boolean = false): DateRange {
    val end = if (includeFuture) startOfDay(now) + 100L * 365 * DAY_MS else startOfDay(now) + DAY_MS
    return DateRange(0L, end)
}

fun rangeForPreset(preset: DateRangePreset, now: Long = currentMillis()): DateRange? =
    when (preset) {
        DateRangePreset.TODAY -> todayRange(now)
        DateRangePreset.WEEK -> weekRange(now)
        DateRangePreset.MONTH -> monthRange(now)
        DateRangePreset.ALL -> allRange(now)
        DateRangePreset.CUSTOM -> null
    }

/** Ekranda ko'rinadigan qisqa oraliq matni. */
fun DateRange.label(preset: DateRangePreset): String =
    if (preset == DateRangePreset.ALL) {
        "Barcha vaqt"
    } else {
        "${formatDate(from)} – ${formatDate(toExclusive - 1)}"
    }

/**
 * Barcha hisob-kitob sahifalari uchun yagona sana oralig'i paneli.
 *
 * Tez tanlovlar (Bugun / Hafta / Oy / Jami) va ixtiyoriy **Oraliq**
 * (Dan–Gacha kalendarlar) bilan ishlaydi. Tanlangan oraliq har doim
 * pastda ko'rsatiladi, shuning uchun foydalanuvchi qaysi davr
 * hisoblanayotganini doim ko'rib turadi.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalTime::class)
@Composable
fun DateRangeBar(
    range: DateRange,
    preset: DateRangePreset,
    onChange: (DateRange, DateRangePreset) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** "Jami" tanlanganda kelajakdagi yozuvlar ham ko'rilsinmi (masalan zametkalar). */
    includeFutureInAll: Boolean = false
) {
    val now = currentMillis()
    var fromStr by remember { mutableStateOf(formatDate(range.from)) }
    var toStr by remember { mutableStateOf(formatDate(range.toExclusive - 1)) }
    var error by remember { mutableStateOf<String?>(null) }

    // Tashqaridan oraliq o'zgarsa (masalan "Oy" tanlansa), maydonlarni yangilaymiz.
    LaunchedEffect(range.from, range.toExclusive) {
        if (preset == DateRangePreset.CUSTOM) {
            fromStr = formatDate(range.from)
            toStr = formatDate(range.toExclusive - 1)
        }
    }

    fun beginCustom() {
        val fallback = monthRange(now)
        val start = if (preset == DateRangePreset.ALL || range.from <= 0L) fallback.from else range.from
        val end = if (preset == DateRangePreset.ALL || range.toExclusive <= 0L) {
            fallback.toExclusive - 1
        } else {
            range.toExclusive - 1
        }
        fromStr = formatDate(start)
        toStr = formatDate(end)
        error = null
        onChange(range, DateRangePreset.CUSTOM)
    }

    fun applyCustom() {
        val from = parseUzbekDate(fromStr)
        val toDay = parseUzbekDate(toStr)
        if (from == null || toDay == null) {
            error = "Sana formati noto'g'ri"
            return
        }
        if (from > toDay) {
            error = "Boshlanish sanasi tugashdan keyin bo'lmasligi kerak"
            return
        }
        error = null
        onChange(DateRange(from, toDay + DAY_MS), DateRangePreset.CUSTOM)
    }

    ProCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateRangePreset.entries.forEach { p ->
                    if (p == preset) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Pro.Green,
                                contentColor = Color(0xFF06281A)
                            )
                        ) { Text(p.label, fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(onClick = {
                            if (p == DateRangePreset.CUSTOM) beginCustom()
                            else if (p == DateRangePreset.ALL) {
                                onChange(allRange(now, includeFutureInAll), p)
                            } else rangeForPreset(p, now)?.let { onChange(it, p) }
                        }) { Text(p.label, color = Pro.TextDim) }
                    }
                }
            }

            if (preset == DateRangePreset.CUSTOM) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateField(
                        label = "Dan",
                        value = fromStr,
                        modifier = Modifier.weight(1f),
                        onPick = { fromStr = formatDate(it); error = null }
                    )
                    DateField(
                        label = "Gacha",
                        value = toStr,
                        modifier = Modifier.weight(1f),
                        onPick = { toStr = formatDate(it); error = null }
                    )
                }
                Button(onClick = { applyCustom() }) { Text("Ko'rsatish") }
            }

            Text(
                "📅 Davr: " + if (preset == DateRangePreset.CUSTOM) "$fromStr – $toStr" else range.label(preset),
                color = if (error != null) Pro.Red else Pro.TextDim,
                fontSize = 12.sp
            )
            if (error != null) Text(error!!, color = Pro.Red, fontSize = 11.sp)
            else if (preset == DateRangePreset.CUSTOM) {
                Text("Kalendardan sanani tanlang — «Ko'rsatish» bosilganda hisob yangilanadi.", color = Pro.TextFaint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DateField(label: String, value: String, modifier: Modifier = Modifier, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("📅", color = Pro.TextDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                .clickable { open = true }
        )
    }
    if (open) {
        ProDatePickerDialog(
            initialMillis = parseUzbekDate(value),
            onDismiss = { open = false },
            onPick = {
                open = false
                onPick(it)
            }
        )
    }
}
