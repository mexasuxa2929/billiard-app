package org.mexa.clube.ui.files

/** Tizim fayl tanlash dialogi orqali `.db` faylni tanlash. Bekor qilinsa null. */
expect object FilePicker {
    fun pickDatabase(initialDir: String): String?
}
