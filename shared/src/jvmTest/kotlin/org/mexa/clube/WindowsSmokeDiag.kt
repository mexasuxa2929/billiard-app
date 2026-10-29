package org.mexa.clube

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import java.io.File
import kotlin.test.Test

/**
 * VAQTINCHALIK diagnostika: Windows'da haqiqiy baza yo'lida
 * "avval o'qish, keyin yozish" ishlashini tekshiradi.
 */
class WindowsSmokeDiag {

    private fun journalMode(path: String): String = runCatching {
        BundledSQLiteDriver().open(path).use { conn ->
            conn.prepare("PRAGMA journal_mode").use { st ->
                if (st.step()) st.getText(0) else "?"
            }
        }
    }.getOrElse { "xato: ${it.message}" }

    @Test
    fun readThenWriteOnRealPath() = runBlocking {
        val dir = File(System.getProperty("user.home"), ".billiardapp-diag")
        dir.deleteRecursively()
        dir.mkdirs()
        val path = File(dir, "diag.db").absolutePath

        println("DIAG os=${System.getProperty("os.name")} path=$path")
        println("DIAG java.io.tmpdir=${System.getProperty("java.io.tmpdir")}")

        var db: BilliardDatabase? = null
        try {
            db = getRoomDatabase(Room.databaseBuilder<BilliardDatabase>(name = path))
            println("DIAG journal(initial)=${journalMode(path)}")
            val read = db.tableDao().getAllTables()
            println("DIAG read=${read.size}")
            BilliardRepository(db).addTable("Diag stol", 1000.0)
            println("DIAG write=ok")
            println("DIAG journal(after_write)=${journalMode(path)}")
        } catch (e: Throwable) {
            println("DIAG xato: ${e::class.simpleName}: ${e.message}")
            e.stackTrace.take(6).forEach { println("DIAG   at $it") }
        } finally {
            runCatching { db?.close() }
            println("DIAG files=${dir.list()?.joinToString(",") ?: "-"}")
            runCatching { dir.deleteRecursively() }
        }
    }
}
