package org.mexa.clube.ui.files

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual object FilePicker {
    actual fun pickDatabase(initialDir: String): String? {
        return try {
            val start = initialDir.ifBlank { System.getProperty("user.home") ?: "." }
            val chooser = JFileChooser(start).apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                dialogTitle = "Ma'lumotlar bazasini tanlang (.db)"
                isAcceptAllFileFilterUsed = true
                fileFilter = FileNameExtensionFilter("SQLite baza (*.db, *.sqlite)", "db", "sqlite", "sqlite3")
            }
            val res = chooser.showOpenDialog(null)
            if (res == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }
}
