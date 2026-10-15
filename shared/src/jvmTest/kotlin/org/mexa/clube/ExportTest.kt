package org.mexa.clube

import kotlinx.coroutines.runBlocking
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.domain.ReportSummary
import org.mexa.clube.domain.TopProduct
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.daysInMonth
import org.mexa.clube.domain.firstWeekdayMondayFirst
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.parseUzbekDate
import org.mexa.clube.domain.startOfDay
import org.mexa.clube.ui.export.ExcelExporter
import java.io.File
import java.io.FileInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportTest {

    @Test
    fun parseUzbekDateValid() {
        val base = startOfDay(currentMillis())
        assertEquals(base, parseUzbekDate(formatDate(base)))
        // Turli ajratkichlar
        assertEquals(base, parseUzbekDate(formatDate(base).replace(".", "/")))
        assertEquals(base, parseUzbekDate("  " + formatDate(base) + "  "))
    }

    @Test
    fun parseUzbekDateInvalid() {
        assertNull(parseUzbekDate(""))
        assertNull(parseUzbekDate("14.10"))
        assertNull(parseUzbekDate("32.01.2026"))
        assertNull(parseUzbekDate("30.02.2026"))
        assertNull(parseUzbekDate("14.13.2026"))
        assertNull(parseUzbekDate("14.10.1999"))
        assertNull(parseUzbekDate("salom"))
    }

    @Test
    fun calendarMathIsCorrect() {
        // 01.10.2026 — payshanba (dushanba=0 -> 3)
        assertEquals(3, firstWeekdayMondayFirst(2026, 10))
        assertEquals(31, daysInMonth(2026, 10))
        assertEquals(28, daysInMonth(2026, 2))
        assertEquals(29, daysInMonth(2024, 2)) // kabisa
        assertEquals(30, daysInMonth(2026, 4))
        assertEquals(0, firstWeekdayMondayFirst(2026, 6)) // 01.06.2026 — dushanba
        assertEquals(6, firstWeekdayMondayFirst(2026, 11)) // 01.11.2026 — yakshanba
        // Har bir oy uchun: 1-kun + kunlar soni keyingi oyning 1-kuniga to'g'ri keladi
        for (m in 1..12) {
            val dim = daysInMonth(2026, m)
            assertTrue(dim in 28..31, "oy $m")
            val first = firstWeekdayMondayFirst(2026, m)
            assertTrue(first in 0..6, "hafta kuni $m")
        }
    }

    @Test
    fun exportCreatesValidXlsx() {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-export-${System.nanoTime()}")
        try {
            assertTrue(ExcelExporter.defaultDir().isNotBlank())
            val from = startOfDay(currentMillis())
            val to = from + 24L * 3600 * 1000
            val summary = ReportSummary(
                from = from, to = to, sessionsCount = 1,
                tableRevenue = 60000.0, barRevenue = 15000.0, barCost = 9000.0,
                barProfit = 6000.0, discount = 0.0, totalRevenue = 75000.0,
                topProducts = listOf(TopProduct("Choy", 3.0, 15000.0))
            )
            val sessions = listOf(
                TableSession(
                    id = 1, tableId = 1, tableName = "Stol-1",
                    startTime = from + 3600_000L, endTime = from + 2 * 3600_000L,
                    hourlyRate = 60000.0, status = "CLOSED",
                    tableSum = 60000.0, barSum = 15000.0, discount = 0.0, totalSum = 75000.0
                )
            )
            val checks = listOf(
                CheckReceipt(
                    id = 1, sessionId = 1, createdAt = from + 2 * 3600_000L,
                    tableName = "Stol-1", tableAmount = 60000.0, barAmount = 15000.0,
                    discount = 0.0, totalAmount = 75000.0, paymentMethod = "Naqd"
                )
            )
            val path = ExcelExporter.exportPeriodReport(
                File(dir, "hisobot_test.xlsx").absolutePath, from, to, summary, sessions, checks
            )
            val file = File(path)
            assertTrue(file.exists() && file.length() > 0, "xlsx fayl yaratilishi kerak")

            FileInputStream(file).use { ins ->
                XSSFWorkbook(ins).use { wb ->
                    assertEquals(4, wb.numberOfSheets)
                    assertEquals("Xulosa", wb.getSheetName(0))
                    assertEquals("Sessiyalar", wb.getSheetName(1))
                    assertEquals("Cheklar", wb.getSheetName(2))
                    assertEquals("TOP mahsulotlar", wb.getSheetName(3))
                    // Xulosa: 9-qator (index 8) JAMI
                    val totalRow = wb.getSheetAt(0).getRow(8)
                    assertEquals("JAMI daromad (so'm)", totalRow.getCell(0).stringCellValue)
                    assertEquals(75000.0, totalRow.getCell(1).numericCellValue)
                    // Xulosa: oraliq sanalari faylda aks etgan
                    assertEquals(formatDate(from), wb.getSheetAt(0).getRow(0).getCell(1).stringCellValue)
                    assertEquals(formatDate(to - 1), wb.getSheetAt(0).getRow(1).getCell(1).stringCellValue)
                    // Sessiyalar: 1-qator
                    val sRow = wb.getSheetAt(1).getRow(1)
                    assertEquals("Stol-1", sRow.getCell(1).stringCellValue)
                    assertEquals(75000.0, sRow.getCell(9).numericCellValue)
                    // TOP: Choy
                    val tRow = wb.getSheetAt(3).getRow(1)
                    assertEquals("Choy", tRow.getCell(1).stringCellValue)
                    assertNotNull(tRow.getCell(3))
                }
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun exportEmptyReportStillCreatesFile() = runBlocking {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-export-${System.nanoTime()}")
        try {
            val from = startOfDay(currentMillis())
            val path = ExcelExporter.exportPeriodReport(
                File(dir, "empty.xlsx").absolutePath, from, from + 24L * 3600 * 1000,
                ReportSummary(from = from, to = from), emptyList(), emptyList()
            )
            assertTrue(File(path).exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
