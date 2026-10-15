package org.mexa.clube.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_DIR = ".billiardapp"
private const val CONFIG_FILE = "db_path.txt"
private const val ENV_DB = "BILLIARD_DB"
private const val PROP_DB = "billiard.db"
private const val SQLITE_HEADER = "SQLite format 3\u0000"

actual object DatabaseLocation {

    // ---------- Yo'llar ----------

    actual fun defaultDir(): String {
        val home = System.getProperty("user.home")?.takeIf { it.isNotBlank() } ?: "."
        val dir = File(home, APP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    actual fun defaultPath(): String = File(defaultDir(), BilliardDatabase.DB_NAME).absolutePath

    actual fun backupDir(): String {
        val dir = File(defaultDir(), "backup")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    private fun configFile(): File = File(defaultDir(), CONFIG_FILE)

    /** Ataylab berilgan yo'l: `-Dbilliard.db=...` yoki `BILLIARD_DB=...` */
    private fun explicitOverride(): String? =
        System.getProperty(PROP_DB)?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv(ENV_DB)?.trim()?.takeIf { it.isNotBlank() }

    private fun readConfig(): String? {
        val cfg = configFile()
        if (!cfg.exists()) return null
        return try {
            cfg.readText().trim().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    actual fun configuredPath(): String? =
        (explicitOverride() ?: readConfig())?.let { File(it).absoluteFile.path }

    actual fun pendingPath(): String? {
        val configured = configuredPath() ?: return null
        if (configured == activePath()) return null
        val f = File(configured)
        return if (f.isFile && f.canRead() && f.canWrite()) configured else null
    }

    actual fun activePath(): String {
        // 1) Aniq berilgan yo'l — fayl hali yo'q bo'lsa ham ishlatiladi (yangi baza yaratiladi).
        explicitOverride()?.let { raw ->
            val f = File(raw).absoluteFile
            val parent = f.parentFile
            if (f.exists() && (!f.isFile || !f.canRead() || !f.canWrite())) return defaultPath()
            if (parent != null && (parent.exists() || parent.mkdirs()) && parent.canWrite()) return f.path
            return defaultPath()
        }
        // 2) Sozlamalardan tanlangan yo'l — fayl mavjud va o'qiladigan bo'lishi shart.
        readConfig()?.let { raw ->
            val f = File(raw).absoluteFile
            if (f.isFile && f.canRead() && f.canWrite()) return f.path
        }
        // 3) Standart joy.
        return defaultPath()
    }

    // ---------- Tanlash / tiklash ----------

    actual fun usePath(path: String) {
        val f = File(path.trim()).absoluteFile
        validate(f.path)?.let { throw IllegalStateException(it) }
        val parent = f.parentFile ?: throw IllegalStateException("Fayl yo'li noto'g'ri")
        if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Papka yaratilmadi: ${parent.path}")
        if (!parent.canWrite()) throw IllegalStateException("Papkaga yozish huquqi yo'q: ${parent.path}")

        if (f.path == defaultPath()) {
            resetToDefault()
            return
        }
        val cfg = configFile()
        val tmp = File(cfg.parentFile, "$CONFIG_FILE.tmp")
        tmp.writeText(f.path)
        if (!tmp.renameTo(cfg)) {
            cfg.writeText(f.path)
            tmp.delete()
        }
    }

    actual fun resetToDefault() {
        try {
            configFile().delete()
        } catch (_: Exception) {
        }
    }

    // ---------- Tekshirish ----------

    actual fun validate(path: String): String? {
        val f = File(path)
        if (!f.exists()) return "Fayl topilmadi: ${f.path}"
        if (!f.isFile) return "Bu fayl emas: ${f.path}"
        if (f.length() < 100) return "Fayl SQLite bazasiga o'xshamaydi (juda kichik)"
        val header = try {
            f.inputStream().use { it.readNBytes(16) }
        } catch (e: Exception) {
            return "Faylni o'qishda xato: ${e.message}"
        }
        if (!header.contentEquals(SQLITE_HEADER.toByteArray(Charsets.US_ASCII))) {
            return "Bu SQLite .db fayli emas"
        }

        val walBefore = File("${f.path}-wal").exists()
        val shmBefore = File("${f.path}-shm").exists()
        var conn: SQLiteConnection? = null
        return try {
            conn = BundledSQLiteDriver().open(f.absolutePath)
            // Baza hozir dastur tomonidan ochiq bo'lishi mumkin — qisqa kutish qo'yamiz.
            conn.prepare("PRAGMA busy_timeout = 5000").use { it.step() }
            val version = conn.prepare("PRAGMA user_version").use { st ->
                if (st.step()) st.getLong(0) else 0L
            }
            when {
                version <= 0L ->
                    "Bu baza Billiard dasturiga tegishli emas (versiya topilmadi)"
                version > BilliardDatabase.DB_VERSION ->
                    "Baza yangiroq versiyada (v$version). Dasturni yangilang"
                else -> {
                    val hasTables = conn.prepare(
                        "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'tables'"
                    ).use { st -> st.step() && st.getLong(0) > 0L }
                    if (!hasTables) "Bu bazada 'tables' jadvali yo'q — boshqa .db tanlang" else null
                }
            }
        } catch (e: Exception) {
            "Bazani o'qishda xato: ${e.message}"
        } finally {
            try {
                conn?.close()
            } catch (_: Exception) {
            }
            // Tekshirish paytida yaratilgan ortiqcha yon fayllarni tozalaymiz
            // (foydalanuvchi bazasi yagona .db fayl bo'lib qolishi uchun).
            if (!walBefore) File("${f.path}-wal").delete()
            if (!shmBefore) File("${f.path}-shm").delete()
        }
    }

    // ---------- Nusxa ----------

    actual fun backup(sourcePath: String, targetDir: String): String {
        val src = File(sourcePath)
        if (!src.isFile) throw IllegalStateException("Baza fayli topilmadi: $sourcePath")
        val dir = File(targetDir)
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("Papka yaratilmadi: $targetDir")

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        var target = File(dir, "billiard_club_$stamp.db")
        var n = 1
        while (target.exists()) {
            target = File(dir, "billiard_club_${stamp}_${n++}.db")
        }

        var conn: SQLiteConnection? = null
        try {
            conn = BundledSQLiteDriver().open(src.absolutePath)
            conn.prepare("PRAGMA busy_timeout = 5000").use { it.step() }
            conn.prepare("VACUUM INTO ?").use { st ->
                st.bindText(1, target.absolutePath)
                st.step()
            }
            if (!target.isFile) throw IllegalStateException("Nusxa yaratilmadi")
            return target.absolutePath
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Nusxa yaratishda xato: ${e.message}", e)
        } finally {
            try {
                conn?.close()
            } catch (_: Exception) {
            }
        }
    }
}
