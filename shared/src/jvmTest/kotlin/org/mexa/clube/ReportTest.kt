package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.entity.BarSale
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.startOfDay
import org.mexa.clube.domain.startOfMonth
import org.mexa.clube.domain.startOfWeek
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val H = 3600_000L
private const val DAY = 24 * H

@OptIn(ExperimentalTime::class)
class ReportTest {
    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun newDb(): BilliardDatabase {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-test-${System.nanoTime()}")
        dir.mkdirs()
        tmpDirs += dir
        // App dagi bilan bir xil builder (Room + BundledSQLiteDriver)
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

    /** Kunlik hisobot: sessiya yakunlangan kuniga (endTime) hisoblanadi. */
    @Test
    fun dailyReportAggregatesByCloseDay() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        // Bugun yopilgan sessiya
        insertSession(db, start = base + 10 * H, end = base + 11 * H, tableSum = 60000.0, discount = 5000.0)
        // Kecha boshlanib bugun yopilgan (tun oralagan) sessiya -> bugunga kirishi kerak
        insertSession(db, start = base - 1 * H, end = base + 1 * H, tableSum = 40000.0, discount = 0.0)
        // Kecha yopilgan sessiya -> bugungi hisobotga kirmasligi kerak
        insertSession(db, start = base - 3 * H, end = base - 2 * H, tableSum = 30000.0, discount = 0.0)

        // Bugungi bar savdosi: Cola x2 (6k/10k) + Choy x1 (2k/5k)
        insertSale(
            db, createdAt = base + 10 * H, items = listOf(
                Triple("Coca-Cola 0.5", 2.0, 6000.0 to 10000.0),
                Triple("Choy", 1.0, 2000.0 to 5000.0)
            )
        )
        // Kechagi bar savdosi
        insertSale(
            db, createdAt = base - 5 * H, items = listOf(
                Triple("Suv 0.5", 1.0, 3000.0 to 5000.0)
            )
        )

        val r = repo.report(base, base + DAY)
        assertEquals(2, r.sessionsCount, "bugun yopilgan sessiyalar soni")
        assertEquals(100000.0, r.tableRevenue, "stol daromadi")
        assertEquals(5000.0, r.discount, "chegirma")
        assertEquals(25000.0, r.barRevenue, "bar savdo")
        assertEquals(14000.0, r.barCost, "bar tannarx")
        assertEquals(11000.0, r.barProfit, "bar foyda")
        assertEquals(120000.0, r.totalRevenue, "JAMI")
        assertEquals(2, r.topProducts.size)
        assertEquals("Coca-Cola 0.5", r.topProducts[0].name)
        assertEquals(2.0, r.topProducts[0].qty)
        assertEquals(20000.0, r.topProducts[0].revenue)
        assertEquals("Choy", r.topProducts[1].name)
    }

    /** Kechagi oyna faqat kecha yopilganlarni oladi. */
    @Test
    fun yesterdayWindowExcludesToday() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())

        insertSession(db, start = base + 10 * H, end = base + 11 * H, tableSum = 60000.0, discount = 0.0)
        insertSession(db, start = base - 3 * H, end = base - 2 * H, tableSum = 30000.0, discount = 0.0)
        insertSale(
            db, createdAt = base - 5 * H, items = listOf(
                Triple("Suv 0.5", 1.0, 3000.0 to 5000.0)
            )
        )

        val r = repo.report(base - DAY, base)
        assertEquals(1, r.sessionsCount)
        assertEquals(30000.0, r.tableRevenue)
        assertEquals(5000.0, r.barRevenue)
        assertEquals(3000.0, r.barCost)
        assertEquals(2000.0, r.barProfit)
        assertEquals(35000.0, r.totalRevenue)
    }

    /** Bo'sh oyna — nollar. */
    @Test
    fun emptyWindowGivesZeros() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())
        insertSession(db, start = base + 10 * H, end = base + 11 * H, tableSum = 60000.0, discount = 0.0)

        val r = repo.report(base + 2 * DAY, base + 3 * DAY)
        assertEquals(0, r.sessionsCount)
        assertEquals(0.0, r.tableRevenue)
        assertEquals(0.0, r.barRevenue)
        assertEquals(0.0, r.barCost)
        assertEquals(0.0, r.barProfit)
        assertEquals(0.0, r.totalRevenue)
        assertTrue(r.topProducts.isEmpty())
    }

    /** Haftalik oyna joriy haftadagi sessiyani oladi, o'tgan haftadagisini olmaydi. */
    @Test
    fun weeklyWindowContainsThisWeekOnly() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val now = currentMillis()
        val weekStart = startOfWeek(now)

        insertSession(db, start = now - H, end = now - H / 2, tableSum = 20000.0, discount = 0.0)
        // Haftadan oldin yopilgan
        insertSession(db, start = weekStart - 2 * H, end = weekStart - H, tableSum = 90000.0, discount = 0.0)

        val r = repo.report(weekStart, weekStart + 7 * DAY)
        assertEquals(1, r.sessionsCount)
        assertEquals(20000.0, r.tableRevenue)
    }

    /** Oylik oyna joriy oydagi sessiyani oladi, o'tgan oydagisini olmaydi. */
    @Test
    fun monthlyWindowContainsThisMonthOnly() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val now = currentMillis()
        val monthStart = startOfMonth(now)
        val nextMonth = startOfMonth(monthStart + 32 * DAY)

        insertSession(db, start = now - H, end = now - H / 2, tableSum = 25000.0, discount = 0.0)
        insertSession(db, start = monthStart - 2 * H, end = monthStart - H, tableSum = 80000.0, discount = 0.0)

        val r = repo.report(monthStart, nextMonth)
        assertEquals(1, r.sessionsCount)
        assertEquals(25000.0, r.tableRevenue)
    }

    /**
     * Stolga biriktirilgan bar savdo: ochiq stolda hisobotga kirmaydi,
     * stol yopilganda bar daromadi/foydasiga qo'shiladi va JAMI da bir marta hisoblanadi.
     */
    @Test
    fun tableBarSaleCountsOnlyAfterSessionClose() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val tableId = db.tableDao().upsert(BilliardTable(name = "Stol-1", hourlyPrice = 60000.0))
        val productId = db.productDao().upsert(
            Product(name = "Cola", buyPrice = 6000.0, sellPrice = 10000.0, stockQty = 10.0)
        )

        val session = repo.startSession(BilliardTable(id = tableId, name = "Stol-1", hourlyPrice = 60000.0))
        repo.sellToTable(session.id, mapOf(productId to 2.0))

        // Stol hali ochiq — bar savdo "kutilayotgan" hisoblanadi, hisobotga kirmaydi.
        val before = repo.report(0, currentMillis() + DAY)
        assertEquals(0.0, before.barRevenue)
        assertEquals(0.0, before.barProfit)
        assertEquals(0, before.sessionsCount)

        val check = repo.closeSession(tableId)
        assertEquals(20000.0, check.barAmount)

        val after = repo.report(0, currentMillis() + DAY)
        assertEquals(20000.0, after.barRevenue)
        assertEquals(8000.0, after.barProfit)
        assertEquals(20000.0, after.barTableRevenue)
        assertEquals(8000.0, after.barTableProfit)
        assertEquals(0.0, after.barDirectRevenue)
        assertEquals(1, after.sessionsCount)
        // JAMI = vaqt + bar − chegirma; bar summasi alohida qo'shilmaydi.
        assertEquals(after.tableRevenue + 20000.0 - after.discount, after.totalRevenue)
    }

    /** Bekor qilingan sessiyaning bar savdosi hisobotga kirmaydi va stok qaytadi. */
    @Test
    fun cancelledSessionBarSaleIsExcluded() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val tableId = db.tableDao().upsert(BilliardTable(name = "Stol-1", hourlyPrice = 60000.0))
        val productId = db.productDao().upsert(
            Product(name = "Cola", buyPrice = 6000.0, sellPrice = 10000.0, stockQty = 10.0)
        )

        val session = repo.startSession(BilliardTable(id = tableId, name = "Stol-1", hourlyPrice = 60000.0))
        repo.sellToTable(session.id, mapOf(productId to 2.0))
        repo.cancelSession(tableId)

        val r = repo.report(0, currentMillis() + DAY)
        assertEquals(0.0, r.barRevenue)
        assertEquals(0.0, r.barProfit)
        assertEquals(0.0, r.totalRevenue)
        assertEquals(0, r.sessionsCount)
        assertEquals(10.0, db.productDao().getById(productId)?.stockQty ?: -1.0)
    }

    /** Stolga yozilgan savdo stol yopilgan kunga kiradi (buyurtma kuniga emas). */
    @Test
    fun tableBarSaleIsReportedOnCloseDay() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())
        val sessionId = insertSession(
            db, start = base - 1 * H, end = base + 1 * H, tableSum = 40000.0, discount = 0.0
        )
        insertSale(
            db, createdAt = base - 1 * H,
            sessionId = sessionId, closedAt = base + 1 * H, status = "CLOSED",
            items = listOf(Triple("Cola", 1.0, 6000.0 to 10000.0))
        )

        val yesterday = repo.report(base - DAY, base)
        assertEquals(0.0, yesterday.barRevenue)

        val today = repo.report(base, base + DAY)
        assertEquals(10000.0, today.barRevenue)
        assertEquals(4000.0, today.barProfit)
        assertEquals(10000.0, today.barTableRevenue)
    }

    /** Eski bazadagi (closedAt yozilmagan) stol savdolari ishga tushganda to'g'rilanadi. */
    @Test
    fun legacyTableBarSalesAreReconciled() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val base = startOfDay(currentMillis())
        val sessionId = insertSession(
            db, start = base + 10 * H, end = base + 11 * H, tableSum = 60000.0, discount = 0.0
        )
        insertSale(
            db, createdAt = base + 10 * H,
            sessionId = sessionId, closedAt = null, status = "OPEN",
            items = listOf(Triple("Cola", 1.0, 6000.0 to 10000.0))
        )

        // Yangilashdan oldin eski savdo hisobotga kirmaydi.
        assertEquals(0.0, repo.report(base, base + DAY).barRevenue)

        repo.reconcileBarSales()

        val r = repo.report(base, base + DAY)
        assertEquals(10000.0, r.barRevenue)
        assertEquals(4000.0, r.barProfit)
        assertEquals(10000.0, r.barTableRevenue)
    }

    /** Davr chegaralari invariantlari (kun/hafta/oy). */
    @Test
    fun periodBoundariesAreCorrect() {
        val now = currentMillis()
        val tz = TimeZone.currentSystemDefault()

        val day = startOfDay(now)
        val dayDt = Instant.fromEpochMilliseconds(day).toLocalDateTime(tz)
        assertEquals(0, dayDt.hour)
        assertEquals(0, dayDt.minute)
        assertTrue(day <= now && now < day + DAY)

        val week = startOfWeek(now)
        val weekDt = Instant.fromEpochMilliseconds(week).toLocalDateTime(tz)
        assertEquals(DayOfWeek.MONDAY, weekDt.dayOfWeek)
        assertEquals(0, weekDt.hour)
        assertTrue(week <= now && now < week + 7 * DAY)

        val month = startOfMonth(now)
        val monthDt = Instant.fromEpochMilliseconds(month).toLocalDateTime(tz)
        assertEquals(1, monthDt.day)
        assertEquals(0, monthDt.hour)
        assertTrue(month <= now)
    }

    // ---------- helpers ----------

    private suspend fun insertSession(
        db: BilliardDatabase,
        start: Long,
        end: Long,
        tableSum: Double,
        discount: Double
    ): Long = db.sessionDao().upsert(
        TableSession(
            tableId = 1,
            tableName = "Stol-1",
            startTime = start,
            endTime = end,
            hourlyRate = 60000.0,
            status = "CLOSED",
            tableSum = tableSum,
            barSum = 0.0,
            discount = discount,
            totalSum = tableSum - discount
        )
    )

    private suspend fun insertSale(
        db: BilliardDatabase,
        createdAt: Long,
        items: List<Triple<String, Double, Pair<Double, Double>>>,
        sessionId: Long? = null,
        closedAt: Long? = null,
        status: String = if (sessionId == null) "CLOSED" else "OPEN"
    ): Long {
        val totalBuy = items.sumOf { (_, qty, prices) -> prices.first * qty }
        val totalSell = items.sumOf { (_, qty, prices) -> prices.second * qty }
        val saleId = db.barSaleDao().insertSale(
            BarSale(
                sessionId = sessionId, createdAt = createdAt, closedAt = closedAt,
                status = status, totalBuy = totalBuy, totalSell = totalSell
            )
        )
        db.barSaleDao().insertItems(
            items.map { (name, qty, prices) ->
                BarSaleItem(
                    saleId = saleId, productId = 0, productName = name,
                    qty = qty, buyPrice = prices.first, sellPrice = prices.second
                )
            }
        )
        return saleId
    }
}
