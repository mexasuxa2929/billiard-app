package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.entity.Expense
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

private const val DAY_MS = 24L * 3600 * 1000

class ExpenseTest {
    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun newDb(): BilliardDatabase {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-expense-${System.nanoTime()}")
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
    fun expenseIsSavedWithReasonAndListed() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        repo.saveExpense(
            Expense(
                spentAt = base + 12 * 3600_000L,
                reason = "Elektr energiyasi",
                category = "Kommunal",
                amount = 250_000.0,
                paymentMethod = "Naqd",
                note = "Yanvar oyi uchun"
            )
        )
        repo.saveExpense(
            Expense(
                spentAt = base + 13 * 3600_000L,
                reason = "Tozalash vositalari",
                category = "Boshqa",
                amount = 50_000.0
            )
        )

        val all = repo.expensesBetween(base, base + DAY_MS)
        assertEquals(2, all.size)
        assertEquals("Tozalash vositalari", all[0].reason, "eng yangi xarajat birinchi turadi")
        assertEquals(300_000.0, all.sumOf { it.amount })
    }

    @Test
    fun reportIncludesExpensesAndNetProfit() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        db.sessionDao().upsert(
            TableSession(
                tableId = 1, tableName = "Stol-1",
                startTime = base + 10 * 3600_000L, endTime = base + 11 * 3600_000L,
                hourlyRate = 60_000.0, status = "CLOSED",
                tableSum = 60_000.0, barSum = 0.0, discount = 0.0, totalSum = 60_000.0
            )
        )
        repo.saveExpense(
            Expense(
                spentAt = base + 12 * 3600_000L,
                reason = "Kreslo ta'miri",
                category = "Ta'mirlash",
                amount = 15_000.0
            )
        )

        val r = repo.report(base, base + DAY_MS)
        assertEquals(60_000.0, r.tableRevenue)
        assertEquals(0.0, r.barRevenue)
        assertEquals(15_000.0, r.expenses)
        assertEquals(60_000.0, r.totalRevenue)
        assertEquals(45_000.0, r.netProfit)
    }

    @Test
    fun expenseRequiresReasonAndPositiveAmount() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        val blankReason = runCatching {
            repo.saveExpense(Expense(spentAt = base, reason = "   ", amount = 10_000.0))
        }.exceptionOrNull()
        assertTrue(blankReason is IllegalArgumentException, "sababsiz xarajat saqlanmasligi kerak")

        val zeroAmount = runCatching {
            repo.saveExpense(Expense(spentAt = base, reason = "Elektr", amount = 0.0))
        }.exceptionOrNull()
        assertTrue(zeroAmount is IllegalArgumentException, "0 summali xarajat saqlanmasligi kerak")

        assertTrue(repo.expensesBetween(base, base + DAY_MS).isEmpty())
    }

    @Test
    fun expenseCanBeEditedAndDeleted() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        val id = repo.saveExpense(
            Expense(spentAt = base, reason = "Eski sabab", amount = 10_000.0)
        )
        val saved = repo.expensesBetween(base, base + DAY_MS).first()
        repo.saveExpense(saved.copy(id = id, reason = "Yangi sabab", amount = 20_000.0))

        val updated = repo.expensesBetween(base, base + DAY_MS).first()
        assertEquals("Yangi sabab", updated.reason)
        assertEquals(20_000.0, updated.amount)

        repo.deleteExpense(id)
        assertTrue(repo.expensesBetween(base, base + DAY_MS).isEmpty())
    }
}
