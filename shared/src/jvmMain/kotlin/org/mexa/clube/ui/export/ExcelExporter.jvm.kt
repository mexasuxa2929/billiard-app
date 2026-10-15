package org.mexa.clube.ui.export

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.domain.Billing
import org.mexa.clube.domain.ReportSummary
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.formatTime
import java.io.File
import java.io.FileOutputStream

actual object ExcelExporter {
    actual fun defaultDir(): String {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".billiardapp/eksport").absolutePath
    }

    actual fun exportPeriodReport(
        filePath: String,
        from: Long,
        to: Long,
        summary: ReportSummary,
        sessions: List<TableSession>,
        checks: List<CheckReceipt>
    ): String {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        XSSFWorkbook().use { wb ->
            val headerStyle = wb.createCellStyle().apply {
                val f = wb.createFont().apply { bold = true; color = IndexedColors.WHITE.index }
                setFont(f)
                fillForegroundColor = IndexedColors.DARK_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }
            val moneyFmt = wb.createDataFormat().getFormat("#,##0")
            val moneyStyle = wb.createCellStyle().apply { dataFormat = moneyFmt }

            // ---- 1. Xulosa ----
            val s0 = wb.createSheet("Xulosa")
            kv(s0, 0, "Davr (dan)", formatDate(from))
            kv(s0, 1, "Davr (gacha)", formatDate(to - 1))
            kv(s0, 2, "Sessiyalar soni", summary.sessionsCount.toDouble(), wb, moneyStyle)
            kv(s0, 3, "Stol daromadi (so'm)", summary.tableRevenue, wb, moneyStyle)
            kv(s0, 4, "Bar savdo (so'm)", summary.barRevenue, wb, moneyStyle)
            kv(s0, 5, "Bar tannarx (so'm)", summary.barCost, wb, moneyStyle)
            kv(s0, 6, "Bar foyda (so'm)", summary.barProfit, wb, moneyStyle)
            kv(s0, 7, "Chegirmalar (so'm)", summary.discount, wb, moneyStyle)
            kv(s0, 8, "JAMI daromad (so'm)", summary.totalRevenue, wb, moneyStyle)
            s0.setColumnWidth(0, 22 * 256)
            s0.setColumnWidth(1, 24 * 256)

            // ---- 2. Sessiyalar ----
            val s1 = wb.createSheet("Sessiyalar")
            header(s1, arrayOf("№", "Stol", "Boshlanish", "Tugash", "Davomiylik", "Tarif/soat", "Stol summa", "Bar summa", "Chegirma", "Jami"), headerStyle)
            sessions.forEachIndexed { i, s ->
                val r = s1.createRow(i + 1)
                r.createCell(0).setCellValue((i + 1).toDouble())
                r.createCell(1).setCellValue(s.tableName)
                r.createCell(2).setCellValue(formatTime(s.startTime))
                r.createCell(3).setCellValue(s.endTime?.let { formatTime(it) } ?: "")
                r.createCell(4).setCellValue(
                    if (s.endTime != null) Billing.durationText(s.startTime, s.endTime!!) else ""
                )
                r.createCell(5).setCellValue(s.hourlyRate)
                money(r, 6, s.tableSum, moneyStyle)
                money(r, 7, s.barSum, moneyStyle)
                money(r, 8, s.discount, moneyStyle)
                money(r, 9, s.totalSum, moneyStyle)
            }
            autosize(s1, 10)

            // ---- 3. Cheklar ----
            val s2 = wb.createSheet("Cheklar")
            header(s2, arrayOf("№", "Chek ID", "Sana", "Stol", "Stol summa", "Bar summa", "Chegirma", "Jami", "To'lov"), headerStyle)
            checks.forEachIndexed { i, c ->
                val r = s2.createRow(i + 1)
                r.createCell(0).setCellValue((i + 1).toDouble())
                r.createCell(1).setCellValue(c.id.toDouble())
                r.createCell(2).setCellValue(formatTime(c.createdAt))
                r.createCell(3).setCellValue(c.tableName)
                money(r, 4, c.tableAmount, moneyStyle)
                money(r, 5, c.barAmount, moneyStyle)
                money(r, 6, c.discount, moneyStyle)
                money(r, 7, c.totalAmount, moneyStyle)
                r.createCell(8).setCellValue(c.paymentMethod)
            }
            autosize(s2, 9)

            // ---- 4. TOP mahsulotlar ----
            val s3 = wb.createSheet("TOP mahsulotlar")
            header(s3, arrayOf("№", "Mahsulot", "Miqdor", "Savdo summasi"), headerStyle)
            summary.topProducts.forEachIndexed { i, t ->
                val r = s3.createRow(i + 1)
                r.createCell(0).setCellValue((i + 1).toDouble())
                r.createCell(1).setCellValue(t.name)
                r.createCell(2).setCellValue(t.qty)
                money(r, 3, t.revenue, moneyStyle)
            }
            autosize(s3, 4)

            FileOutputStream(file).use { out -> wb.write(out) }
        }
        return file.absolutePath
    }

    private fun kv(sheet: org.apache.poi.ss.usermodel.Sheet, row: Int, k: String, v: String) {
        val r = sheet.createRow(row)
        r.createCell(0).setCellValue(k)
        r.createCell(1).setCellValue(v)
    }

    private fun kv(sheet: org.apache.poi.ss.usermodel.Sheet, row: Int, k: String, v: Double, wb: Workbook, moneyStyle: org.apache.poi.ss.usermodel.CellStyle) {
        val r = sheet.createRow(row)
        r.createCell(0).setCellValue(k)
        money(r, 1, v, moneyStyle)
    }

    private fun money(row: org.apache.poi.ss.usermodel.Row, col: Int, v: Double, style: org.apache.poi.ss.usermodel.CellStyle) {
        row.createCell(col).apply { setCellValue(v); cellStyle = style }
    }

    private fun header(sheet: org.apache.poi.ss.usermodel.Sheet, titles: Array<String>, style: org.apache.poi.ss.usermodel.CellStyle) {
        val r = sheet.createRow(0)
        titles.forEachIndexed { i, t ->
            r.createCell(i).apply { setCellValue(t); cellStyle = style }
        }
        sheet.createFreezePane(0, 1)
    }

    private fun autosize(sheet: org.apache.poi.ss.usermodel.Sheet, cols: Int) {
        for (i in 0 until cols) runCatching { sheet.autoSizeColumn(i) }
    }
}
