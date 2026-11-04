package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.currentMillis
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hisobotlar tarixidagi xato chekni o'chirish. */
class CheckDeleteTest {
    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun newDb(): BilliardDatabase {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-check-${System.nanoTime()}")
        dir.mkdirs()
        tmpDirs += dir
        val db = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = File(dir, "test.db").absolutePath)
        )
        openDbs += db
        return db
    }

    @AfterTest
    fun cleanup() {
        openDbs.forEach { runCatching { it.close() } }
        openDbs.clear()
        tmpDirs.forEach { it.deleteRecursively() }
        tmpDirs.clear()
    }

    @Test
    fun deletingCheckRemovesItFromHistory() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        repo.addTable("Stol-1", 60_000.0)
        val table = repo.tables.first().first()

        repo.startSession(table)
        val check = repo.closeSession(table.id)
        val to = currentMillis() + 24L * 3600 * 1000

        assertEquals(1, repo.checksBetween(0, to).size, "chek yozilgan bo'lishi kerak")
        val revenueBefore = repo.report(0, to).totalRevenue

        repo.deleteCheck(check.id)

        assertTrue(repo.checksBetween(0, to).isEmpty(), "chek tarixdan o'chiriladi")
        assertEquals(
            revenueBefore, repo.report(0, to).totalRevenue,
            "faqat chek yozuvi o'chadi, sessiya daromadi hisobotda qoladi"
        )
    }
}
