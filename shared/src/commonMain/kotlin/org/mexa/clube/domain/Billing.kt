package org.mexa.clube.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.TableSession
import kotlin.math.ceil
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object Billing {
    /** Daqiqabay hisob: soniyalarni yuqoriga yaxlitlaydi, min 1 daqiqa */
    fun calculateTableCost(startMillis: Long, endMillis: Long, hourlyPrice: Double): Double {
        val diff = (endMillis - startMillis).coerceAtLeast(0)
        val minutes = ceil(diff / 60_000.0).coerceAtLeast(1.0)
        return hourlyPrice * minutes / 60.0
    }

    fun durationText(startMillis: Long, endMillis: Long): String {
        val diff = (endMillis - startMillis).coerceAtLeast(0) / 1000
        val h = diff / 3600
        val m = (diff % 3600) / 60
        val s = diff % 60
        return "${pad(h)}:${pad(m)}:${pad(s)}"
    }

    private fun pad(v: Long): String = if (v < 10) "0$v" else "$v"

    fun roundToHundred(value: Double): Double = ceil(value / 100.0) * 100.0
}

fun formatMoney(value: Double): String {
    val v = value.toLong()
    val negative = v < 0
    val s = kotlin.math.abs(v).toString().reversed().chunked(3).joinToString(" ").reversed()
    return (if (negative) "-" else "") + "$s so'm"
}

@OptIn(ExperimentalTime::class)
fun formatTime(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad2(dt.day)}.${pad2(dt.month.number)}.${dt.year} ${pad2(dt.hour)}:${pad2(dt.minute)}"
}

@OptIn(ExperimentalTime::class)
fun formatHourMinute(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad2(dt.hour)}:${pad2(dt.minute)}"
}

/** "HH:mm" matnni bugungi kun millisiga aylantirish (bron vaqti uchun). */
fun todayAt(hhmm: String, now: Long): Long? {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return startOfDay(now) + h * 3600_000L + m * 60_000L
}

private fun pad2(v: Int): String = if (v < 10) "0$v" else "$v"

/** "14.10.2026" ko'rinishida sana (hisobot oralig'i uchun). */
@OptIn(ExperimentalTime::class)
fun formatDate(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad2(dt.day)}.${pad2(dt.month.number)}.${dt.year}"
}

/** "DD.MM.YYYY" matnni kun boshiga (millis) aylantiradi. Noto'g'ri bo'lsa null. */
@OptIn(ExperimentalTime::class)
fun parseUzbekDate(s: String): Long? {
    val p = s.trim().split(".", " ", "/", "-").filter { it.isNotBlank() }
    if (p.size != 3) return null
    val d = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    val y = p[2].toIntOrNull() ?: return null
    if (y < 2000 || y > 2100 || m !in 1..12 || d !in 1..31) return null
    return try {
        // Noto'g'ri kun (masalan 30-fevral) da LocalDate xato beradi
        val date = kotlinx.datetime.LocalDate(y, m, d)
        LocalDateTime(date.year, date.month, date.day, 0, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

/** Fayl nomi uchun "20261014_184215" stamp. */
@OptIn(ExperimentalTime::class)
fun formatFileStamp(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}${pad2(dt.month.number)}${pad2(dt.day)}_${pad2(dt.hour)}${pad2(dt.minute)}${pad2(dt.second)}"
}

/** Oydagi kunlar soni (1..12 oy uchun). */
fun daysInMonth(year: Int, month: Int): Int {
    require(month in 1..12)
    val first = kotlinx.datetime.LocalDate(year, month, 1)
    val next = if (month == 12) kotlinx.datetime.LocalDate(year + 1, 1, 1)
    else kotlinx.datetime.LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}

/** Oy 1-kunining hafta kuni, dushanba=0 .. yakshanba=6. */
fun firstWeekdayMondayFirst(year: Int, month: Int): Int {
    require(month in 1..12)
    return kotlinx.datetime.LocalDate(year, month, 1).dayOfWeek.ordinal
}

val UZ_MONTHS = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
    "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"
)

@OptIn(ExperimentalTime::class)
fun startOfDay(millis: Long): Long {
    val tz = TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
    val start = LocalDateTime(dt.year, dt.month, dt.day, 0, 0, 0, 0)
    return start.toInstant(tz).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun startOfWeek(millis: Long): Long {
    // Dushanbadan boshlab 7 kun
    val day = startOfDay(millis)
    val tz = TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
    // ISO: Monday=1 ... Sunday=7
    val dow = dt.dayOfWeek.ordinal // Monday=0
    return day - dow * 24L * 60 * 60 * 1000
}

@OptIn(ExperimentalTime::class)
fun startOfMonth(millis: Long): Long {
    val tz = TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz)
    val start = LocalDateTime(dt.year, dt.month, 1, 0, 0, 0, 0)
    return start.toInstant(tz).toEpochMilliseconds()
}

data class ReportSummary(
    val from: Long,
    val to: Long,
    val sessionsCount: Int = 0,
    val tableRevenue: Double = 0.0,
    val barRevenue: Double = 0.0,
    val barCost: Double = 0.0,
    val barProfit: Double = 0.0,
    /** Bar savdodan stolga biriktirilgan qismi (sessiya yopilganda hisobga olinadi). */
    val barTableRevenue: Double = 0.0,
    val barTableCost: Double = 0.0,
    val barTableProfit: Double = 0.0,
    /** Bar savdodan to'g'ridan-to'g'ri (stolsiz) savdo qismi. */
    val barDirectRevenue: Double = 0.0,
    val barDirectCost: Double = 0.0,
    val barDirectProfit: Double = 0.0,
    val discount: Double = 0.0,
    val totalRevenue: Double = 0.0,
    /** Davr ichidagi xarajatlar yig'indisi. */
    val expenses: Double = 0.0,
    /** Sof foyda = JAMI daromad − xarajatlar. */
    val netProfit: Double = 0.0,
    val topProducts: List<TopProduct> = emptyList()
)

data class TopProduct(
    val name: String,
    val qty: Double,
    val revenue: Double
)

fun buildCheckText(
    clubName: String,
    session: TableSession?,
    barItems: List<BarSaleItem>,
    check: CheckReceipt,
    address: String = "",
    footer: String = "Xizmatimizdan mamnun bo'ling!"
): String {
    val sb = StringBuilder()
    sb.appendLine("=================================")
    sb.appendLine("        $clubName")
    if (address.isNotBlank()) sb.appendLine("        $address")
    sb.appendLine("=================================")
    sb.appendLine("Chek #${check.id}  ${formatTime(check.createdAt)}")
    sb.appendLine("To'lov: ${check.paymentMethod}")
    sb.appendLine("---------------------------------")
    if (session != null) {
        val dur = if (session.endTime != null) Billing.durationText(session.startTime, session.endTime!!)
        else Billing.durationText(session.startTime, currentMillis())
        sb.appendLine("Stol: ${session.tableName}")
        sb.appendLine("Boshlanish: ${formatTime(session.startTime)}")
        if (session.endTime != null) sb.appendLine("Tugash: ${formatTime(session.endTime!!)}")
        sb.appendLine("Davomiylik: $dur")
        sb.appendLine("Tarif: ${formatMoney(session.hourlyRate)}/soat")
        sb.appendLine("Stol summasi: ${formatMoney(check.tableAmount)}")
        sb.appendLine("---------------------------------")
    }
    if (barItems.isNotEmpty()) {
        sb.appendLine("BAR:")
        barItems.forEachIndexed { i, it ->
            sb.appendLine("${i + 1}. ${it.productName} x${trimQty(it.qty)}")
            sb.appendLine("   ${formatMoney(it.sellPrice)} = ${formatMoney(it.sellPrice * it.qty)}")
        }
        sb.appendLine("Bar summasi: ${formatMoney(check.barAmount)}")
        sb.appendLine("---------------------------------")
    }
    if (check.discount > 0) sb.appendLine("Chegirma: ${formatMoney(check.discount)}")
    sb.appendLine("JAMI: ${formatMoney(check.totalAmount)}")
    sb.appendLine("=================================")
    sb.appendLine(footer)
    sb.appendLine("=================================")
    return sb.toString()
}

private fun trimQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

expect fun currentMillis(): Long
