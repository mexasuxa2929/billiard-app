package org.mexa.clube.ui.files

import javax.swing.JFileChooser

actual object FolderPicker {
    actual fun pickFolder(initialDir: String): String? {
        return try {
            val start = initialDir.ifBlank { System.getProperty("user.home") ?: "." }
            val chooser = JFileChooser(start).apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Eksport papkasini tanlang"
                isAcceptAllFileFilterUsed = false
            }
            val res = chooser.showOpenDialog(null)
            if (res == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }
}
