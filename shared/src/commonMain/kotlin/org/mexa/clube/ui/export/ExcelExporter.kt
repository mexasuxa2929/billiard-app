package org.mexa.clube.ui.export

import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.domain.ReportSummary

/** Hisobotni .xlsx faylga yozadi, to'liq yo'lni qaytaradi. Faqat JVM/desktop da ishlaydi. */
expect object ExcelExporter {
    fun defaultDir(): String
    fun exportPeriodReport(
        filePath: String,
        from: Long,
        to: Long,
        summary: ReportSummary,
        sessions: List<TableSession>,
        checks: List<CheckReceipt>
    ): String
}
