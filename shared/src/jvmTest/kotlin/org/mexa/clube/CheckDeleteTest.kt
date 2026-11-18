package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.startOfDay
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hisobotlar tarixidagi xato chekni o'chirish: summalar ham hisobotdan chiqadi. */
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
    fun deletingSessionCheckCancelsSessionAndRevenue() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())
        val sessionId = db.sessionDao().upsert(
            TableSession(
                tableId = 1, tableName = "Stol-1",
                startTime = base, endTime = base + 60 * 60 * 1000,
                hourlyRate = 60_000.0, status = "CLOSED",
                tableSum = 60_000.0, totalSum = 60_000.0
            )
        )
        val checkId = db.checkDao().insert(
            CheckReceipt(
                sessionId = sessionId, createdAt = base + 60 * 60 * 1000,
                tableName = "Stol-1", tableAmount = 60_000.0, totalAmount = 60_000.0
            )
        )
        val to = base + 24L * 3600 * 1000

        assertEquals(1, repo.checksBetween(0, to).size, "chek yozilgan bo'lishi kerak")
        assertEquals(60_000.0, repo.report(base, to).totalRevenue, "chek summasi hisobotda")

        repo.deleteCheck(checkId)

        assertTrue(repo.checksBetween(0, to).isEmpty(), "chek tarixdan o'chiriladi")
        assertEquals("CANCELLED", db.sessionDao().getById(sessionId)?.status, "sessiya bekor qilinadi")
        assertEquals(0.0, repo.report(base, to).totalRevenue, "umumiy summa kamayadi")
    }

    @Test
    fun deletingDirectBarCheckCancelsSaleAndRestoresStock() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())
        val productId = repo.upsertProduct(
            Product(name = "Cola", category = "Ichimlik", buyPrice = 1_000.0, sellPrice = 2_000.0, stockQty = 10.0)
        )

        val check = repo.sellToTable(null, mapOf(productId to 2.0))!!
        val to = base + 24L * 3600 * 1000

        assertEquals(4_000.0, repo.report(base, to).barRevenue, "to'g'ridan-to'g'ri savdo hisobotda")
        assertEquals(8.0, repo.products.first().first { it.id == productId }.stockQty)

        repo.deleteCheck(check.id)

        assertTrue(repo.checksBetween(0, to).isEmpty(), "chek tarixdan o'chiriladi")
        assertEquals(0.0, repo.report(base, to).barRevenue, "umumiy summa kamayadi")
        assertEquals(10.0, repo.products.first().first { it.id == productId }.stockQty, "stok omborga qaytadi")
    }
}
