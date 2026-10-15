package org.mexa.clube.ui.print

import java.awt.Font
import java.awt.print.PrinterJob
import java.io.File

actual object CheckPrinter {
    actual fun print(text: String) {
        val job = PrinterJob.getPrinterJob()
        job.setPrintable { graphics, pageFormat, _ ->
            graphics.font = Font("Monospaced", Font.PLAIN, 10)
            var y = 20
            for (line in text.lines()) {
                graphics.drawString(line, 20, y)
                y += 14
            }
            java.awt.print.Printable.PAGE_EXISTS
        }
        if (job.printDialog()) {
            job.print()
        }
    }

    actual fun saveToFile(text: String, fileName: String): String {
        val dir = File(System.getProperty("user.home") ?: ".", ".billiardapp/cheklar")
        dir.mkdirs()
        val f = File(dir, fileName)
        f.writeText(text)
        return f.absolutePath
    }
}
