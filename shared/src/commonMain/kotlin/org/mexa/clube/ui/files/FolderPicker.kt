package org.mexa.clube.ui.files

/** Tizim papka tanlash dialogini ochadi. Tanlansa yo'l, bekor qilinsa null. Faqat JVM/desktop. */
expect object FolderPicker {
    fun pickFolder(initialDir: String): String?
}
