package org.mexa.clube

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.repository.BilliardRepository
import java.io.File
import kotlin.test.Test

/**
 * VAQTINCHALIK diagnostika: Windows'da qaysi journal rejimi "avval o'qish,
 * keyin yozish" da ishlashini aniqlaydi.
 */
class WindowsSmokeDiag {

    private fun build(path: String, mode: RoomDatabase.JournalMode): BilliardDatabase =
        Room.databaseBuilder<BilliardDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(mode)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private fun attempt(label: String, mode: RoomDatabase.JournalMode, reads: Int = 1) = runBlocking {
        val dir = File(System.getProperty("user.home"), ".billiardapp-diag-${System.nanoTime()}")
        dir.mkdirs()
        val path = File(dir, "diag.db").absolutePath
        var db: BilliardDatabase? = null
        val t0 = System.currentTimeMillis()
        var ok = true
        try {
            db = build(path, mode)
            repeat(reads) { db.tableDao().getAllTables() }
            BilliardRepository(db).addTable("Stol", 1000.0)
            repeat(reads) { db.tableDao().getAllTables() }
            BilliardRepository(db).addTable("Stol 2", 2000.0)
            println("DIAG $label -> OK (${System.currentTimeMillis() - t0} ms)")
        } catch (e: Throwable) {
            ok = false
            println("DIAG $label -> XATO ${e::class.simpleName}: ${e.message} (${System.currentTimeMillis() - t0} ms)")
        } finally {
            runCatching { db?.close() }
            val left = dir.list()?.joinToString(",") ?: "-"
            println("DIAG $label ok=$ok yopilgandan keyingi fayllar=[$left]")
            runCatching { dir.deleteRecursively() }
        }
    }

    @Test
    fun journalModes() {
        println("DIAG os=${System.getProperty("os.name")}")
        attempt("TRUNCATE/read+write", RoomDatabase.JournalMode.TRUNCATE)
        attempt("WAL/read+write", RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        attempt("TRUNCATE/write-only", RoomDatabase.JournalMode.TRUNCATE, reads = 0)
        attempt("TRUNCATE/read3+write", RoomDatabase.JournalMode.TRUNCATE, reads = 3)
        attempt("WAL/read3+write", RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING, reads = 3)
    }
}
