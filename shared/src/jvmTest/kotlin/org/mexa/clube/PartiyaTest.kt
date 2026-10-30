package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.Billing
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Partiyalar ("taymerni yangilash"): har bir partiya alohida hisoblanadi,
 * sessiya yakunida esa ularning umumiy summasi chiqadi.
 */
class PartiyaTest {

    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun newDb(): BilliardDatabase {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-part-${System.nanoTime()}")
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
    fun partAmountsAreSummedIntoTableTotal() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val price = 120_000.0
        repo.addTable("Stol-1", price)
        val table = repo.tables.first().first()

        val session = repo.startSession(table)
        // 10 daqiqalik birinchi partiya
        val firstPart = repo.startNewPart(table.id, atMillis = session.startTime + 10 * 60_000L)
        assertEquals(1, firstPart.partNo, "birinchi partiya raqami 1 bo'lishi kerak")
        assertEquals(10L, firstPart.minutes, "10 daqiqa")
        assertEquals(price * 10 / 60.0, firstPart.amount, "partiya summasi daqiqabay")

        // Yana bitta partiya
        val secondPart = repo.startNewPart(table.id, atMillis = firstPart.endTime + 5 * 60_000L)
        assertEquals(2, secondPart.partNo)
        assertEquals(price * 5 / 60.0, secondPart.amount)

        val parts = repo.partsForSession(session.id)
        assertEquals(2, parts.size, "ikkita yopilgan partiya bo'lishi kerak")
        // Joriy partiya hali davom etmoqda: kamida 1 daqiqa hisoblanadi.
        assertEquals(
            parts.sumOf { it.amount } + price / 60.0,
            repo.previewClose(table.id, atMillis = secondPart.endTime).second,
            "stol hisobi yopilgan partiyalar + joriy partiya bo'lishi kerak"
        )

        val check = repo.closeSession(table.id, 0.0, "Naqd", "TEST")
        val allParts = repo.partsForSession(session.id)
        assertEquals(3, allParts.size, "yakunlashda oxirgi partiya ham yoziladi")
        assertEquals(allParts.sumOf { it.amount }, check.tableAmount, "stol summasi = barcha partiyalar yig'indisi")
        assertTrue("Partiya:3" in check.details, "chekda partiyalar soni ko'rsatilishi kerak")
    }

    @Test
    fun sessionWithoutPartsIsBilledAsOne() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        repo.addTable("Stol-2", 60_000.0)
        val table = repo.tables.first().first()

        val session = repo.startSession(table)
        val check = repo.closeSession(table.id, 0.0, "Naqd", "TEST")

        val parts = repo.partsForSession(session.id)
        assertEquals(1, parts.size, "partiyaga bo'linmagan sessiya ham bitta partiya bo'lib yoziladi")
        assertEquals(Billing.durationMinutes(parts[0].startTime, parts[0].endTime), parts[0].minutes)
        assertEquals(parts[0].amount, check.tableAmount)
    }

    @Test
    fun veryShortPartIsRejected() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        repo.addTable("Stol-3", 60_000.0)
        val table = repo.tables.first().first()

        repo.startSession(table)
        // Darhol bosilsa (10 soniyadan kam) — tasodifiy ikki marta bosishdan himoya
        assertFailsWith<IllegalStateException> { repo.startNewPart(table.id) }
        assertEquals(0, repo.partsForSession(repo.openSessions.first().first().id).size)
    }

    @Test
    fun cancelledSessionDropsParts() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        repo.addTable("Stol-4", 60_000.0)
        val table = repo.tables.first().first()

        val session = repo.startSession(table)
        repo.startNewPart(table.id, atMillis = session.startTime + 60_000L)
        assertEquals(1, repo.partsForSession(session.id).size)

        repo.cancelSession(table.id)
        assertEquals(0, repo.partsForSession(session.id).size, "bekor qilingan sessiya partiyalari o'chiriladi")
    }
}
