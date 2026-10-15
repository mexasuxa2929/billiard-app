package org.mexa.clube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mexa.clube.domain.UZ_MONTHS
import org.mexa.clube.domain.daysInMonth
import org.mexa.clube.domain.firstWeekdayMondayFirst
import org.mexa.clube.ui.theme.Pro
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val WEEKDAYS = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")

/**
 * O'zimizning qorong'u (Pro) kalendar dialogi.
 * Material3 DatePicker desktop da eski kotlinx-datetime bilan crash beradi,
 * shuning uchun sof Compose da yozilgan.
 *
 * @param initialMillis boshlang'ich sana (mahalliy yarim tun millis) yoki null=bugun
 * @param onPick tanlangan sana (mahalliy yarim tun millis)
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ProDatePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val initDt = (initialMillis ?: 0L).let {
        if (it > 0) Instant.fromEpochMilliseconds(it).toLocalDateTime(tz)
        else Instant.fromEpochMilliseconds(org.mexa.clube.domain.currentMillis()).toLocalDateTime(tz)
    }
    var year by remember { mutableStateOf(initDt.year) }
    var month by remember { mutableStateOf(initDt.month.number) }
    var day by remember { mutableStateOf(initDt.day) }

    fun shiftMonth(delta: Int) {
        var m = month + delta
        var y = year
        while (m < 1) { m += 12; y-- }
        while (m > 12) { m -= 12; y++ }
        if (y in 2000..2100) {
            year = y; month = m
            day = day.coerceAtMost(daysInMonth(y, m))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📅 Sana tanlash", color = Pro.TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Oy navigatsiyasi
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { shiftMonth(-1) }) { Text("◀", color = Pro.Green) }
                    Text(
                        "${UZ_MONTHS[month - 1]} $year",
                        color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 15.sp
                    )
                    TextButton(onClick = { shiftMonth(1) }) { Text("▶", color = Pro.Green) }
                }
                // Hafta kunlari
                Row(Modifier.fillMaxWidth()) {
                    WEEKDAYS.forEach {
                        Text(
                            it, Modifier.weight(1f), textAlign = TextAlign.Center,
                            color = Pro.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Kunlar to'ri (dushanbadan boshlanadi)
                val blanks = firstWeekdayMondayFirst(year, month)
                val total = daysInMonth(year, month)
                val cells = (List(blanks) { null } + (1..total).toList())
                val today = Instant.fromEpochMilliseconds(org.mexa.clube.domain.currentMillis()).toLocalDateTime(tz)
                val isTodayMonth = today.year == year && today.month.number == month
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d == null) {
                                    Spacer(Modifier.size(34.dp))
                                } else {
                                    val selected = d == day
                                    val isToday = isTodayMonth && d == today.day
                                    Box(
                                        Modifier.size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Pro.Green else Color.Transparent)
                                            .then(
                                                if (!selected && isToday) Modifier.border(1.dp, Pro.Green, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { day = d },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            d.toString(),
                                            color = if (selected) Color(0xFF06281A)
                                            else if (isToday) Pro.Green else Pro.TextMain,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                        // oxirgi qator to'liq bo'lmasa bo'sh joy
                        if (week.size < 7) {
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(Pro.Surface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Tanlandi: ${pad(day)}.${pad(month)}.$year",
                        color = Pro.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val picked = LocalDateTime(year, month, day, 0, 0, 0, 0)
                    .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                onPick(picked)
            }) { Text("Tanlash", color = Pro.Green, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor", color = Pro.TextDim) } }
    )
}

private fun pad(v: Int): String = if (v < 10) "0$v" else "$v"
