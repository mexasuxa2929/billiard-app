package org.mexa.clube.data.repository

import kotlinx.coroutines.flow.Flow
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.DatabaseLocation
import org.mexa.clube.data.entity.AppSetting
import org.mexa.clube.data.entity.BarSale
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Booking
import org.mexa.clube.data.entity.Category
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.Expense
import org.mexa.clube.data.entity.MovementLog
import org.mexa.clube.data.entity.Note
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.domain.Billing
import org.mexa.clube.domain.ReportSummary
import org.mexa.clube.domain.TopProduct
import org.mexa.clube.domain.currentMillis

class BilliardRepository(
    private val db: BilliardDatabase,
    /** Dastur ishga tushganda ochilgan baza fayli (nusxa olish uchun). */
    private val openedDbPath: String = DatabaseLocation.activePath()
) {
    val tables: Flow<List<BilliardTable>> = db.tableDao().observeTables()
    val products: Flow<List<Product>> = db.productDao().observeProducts()
    val openSessions: Flow<List<TableSession>> = db.sessionDao().observeOpenSessions()
    val categories: Flow<List<Category>> = db.categoryDao().observeCategories()
    val bookings: Flow<List<Booking>> = db.bookingDao().observeActive()
    val expenses: Flow<List<Expense>> = db.expenseDao().observeAll()
    val notes: Flow<List<Note>> = db.noteDao().observeAll()

    // ---------- Categories ----------
    suspend fun addCategory(rawName: String) {
        val name = rawName.trim()
        require(name.isNotBlank()) { "Kategoriya nomi bo'sh bo'lmasligi kerak" }
        if (db.categoryDao().getByName(name) != null) throw IllegalStateException("«$name» kategoriyasi allaqachon mavjud")
        val order = (db.categoryDao().getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        db.categoryDao().insert(Category(name = name, sortOrder = order))
        log("PRODUCT", "Yangi kategoriya: $name", 0.0)
    }

    suspend fun renameCategory(oldName: String, rawNew: String) {
        val newName = rawNew.trim()
        require(newName.isNotBlank()) { "Kategoriya nomi bo'sh bo'lmasligi kerak" }
        if (oldName == newName) return
        if (db.categoryDao().getByName(newName) != null) throw IllegalStateException("«$newName» kategoriyasi allaqachon mavjud")
        val existing = db.categoryDao().getByName(oldName) ?: throw IllegalStateException("Kategoriya topilmadi")
        db.categoryDao().update(existing.copy(name = newName))
        db.productDao().renameCategory(oldName, newName)
        log("PRODUCT", "Kategoriya o'zgartirildi: $oldName → $newName", 0.0)
    }

    suspend fun deleteCategory(id: Long, name: String) {
        val used = db.productDao().countByCategory(name)
        if (used > 0) throw IllegalStateException("«$name» da $used ta mahsulot bor — avval ularni boshqa kategoriyaga o'tkazing")
        db.categoryDao().deleteById(id)
        log("PRODUCT", "Kategoriya o'chirildi: $name", 0.0)
    }

    // ---------- Tables ----------
    suspend fun addTable(name: String, hourlyPrice: Double, zone: String = "Standart") {
        val all = db.tableDao().getAllTables()
        val order = (all.maxOfOrNull { it.sortOrder } ?: all.size) + 1
        val id = db.tableDao().upsert(BilliardTable(name = name, hourlyPrice = hourlyPrice, sortOrder = order, zone = zone.trim().ifBlank { "Standart" }))
        log("TABLE", "Yangi stol qo'shildi: $name (${hourlyPrice.toLong()} so'm/soat, $zone)", 0.0)
    }

    suspend fun updateTable(table: BilliardTable) {
        db.tableDao().update(table)
        log("TABLE", "Stol yangilandi: ${table.name}", 0.0)
    }

    suspend fun deleteTable(id: Long, name: String) {
        val open = db.sessionDao().getOpenByTable(id)
        if (open != null) throw IllegalStateException("Stol band, avval sessiyani yakunlang")
        db.tableDao().deleteById(id)
        log("TABLE", "Stol o'chirildi: $name", 0.0)
    }

    // ---------- Bookings (bron) ----------
    suspend fun addBooking(tableId: Long, clientName: String, phone: String, timeMillis: Long, note: String = ""): Long {
        val table = db.tableDao().getAllTables().firstOrNull { it.id == tableId }
            ?: throw IllegalStateException("Stol topilmadi")
        if (db.sessionDao().getOpenByTable(tableId) != null) throw IllegalStateException("Stol band — bron qilib bo'lmaydi")
        if (db.bookingDao().getActiveByTable(tableId) != null) throw IllegalStateException("Bu stolda faol bron bor")
        require(clientName.trim().isNotBlank()) { "Mijoz ismi kiritilishi shart" }
        val id = db.bookingDao().upsert(
            Booking(tableId = tableId, tableName = table.name, clientName = clientName.trim(), phone = phone.trim(), timeMillis = timeMillis, note = note.trim())
        )
        log("BOOKING", "${table.name} bron qilindi: ${clientName.trim()} (${phone.trim()})", 0.0)
        return id
    }

    suspend fun updateBooking(booking: Booking) {
        db.bookingDao().update(booking)
        log("BOOKING", "Bron yangilandi: ${booking.tableName} — ${booking.clientName}", 0.0)
    }

    suspend fun cancelBooking(id: Long) {
        val b = db.bookingDao().getById(id) ?: return
        db.bookingDao().update(b.copy(status = "CANCELLED"))
        log("BOOKING", "Bron bekor qilindi: ${b.tableName} — ${b.clientName}", 0.0)
    }

    /** Bronni erta boshlash: sessiya ochiladi, bron DONE bo'ladi. */
    suspend fun startFromBooking(bookingId: Long): TableSession {
        val b = db.bookingDao().getById(bookingId) ?: throw IllegalStateException("Bron topilmadi")
        val table = db.tableDao().getAllTables().firstOrNull { it.id == b.tableId }
            ?: throw IllegalStateException("Stol topilmadi")
        val session = startSession(table)
        db.bookingDao().update(b.copy(status = "DONE"))
        log("BOOKING", "${b.tableName} bron bo'yicha erta boshladi: ${b.clientName}", 0.0)
        return session
    }

    suspend fun lastClosedByTable(tableId: Long): TableSession? = db.sessionDao().getLastClosedByTable(tableId)

    // ---------- Shift ----------
    suspend fun getOrCreateShiftStart(todayTenAm: Long): Long {
        val saved = db.settingsDao().get("shift_start")?.value?.toLongOrNull()
        if (saved != null && saved > todayTenAm - 24L * 3600 * 1000) return saved
        db.settingsDao().put(AppSetting("shift_start", todayTenAm.toString()))
        return todayTenAm
    }

    // ---------- Sessions (stol start/stop) ----------
    suspend fun startSession(table: BilliardTable): TableSession {
        val existing = db.sessionDao().getOpenByTable(table.id)
        if (existing != null) throw IllegalStateException("Bu stolda ochiq sessiya bor")
        val now = currentMillis()
        val session = TableSession(
            tableId = table.id,
            tableName = table.name,
            startTime = now,
            hourlyRate = table.hourlyPrice,
            status = "OPEN"
        )
        val id = db.sessionDao().upsert(session)
        log("TABLE_START", "${table.name} ishga tushdi (${table.hourlyPrice.toLong()} so'm/soat)", 0.0)
        return session.copy(id = id)
    }

    suspend fun previewClose(tableId: Long, atMillis: Long = currentMillis()): Triple<TableSession, Double, List<BarSaleItem>> {
        val session = db.sessionDao().getOpenByTable(tableId)
            ?: throw IllegalStateException("Ochiq sessiya topilmadi")
        val cost = Billing.calculateTableCost(session.startTime, atMillis, session.hourlyRate)
        val items = barItemsForSession(session.id)
        return Triple(session, cost, items)
    }

    suspend fun barItemsForSession(sessionId: Long): List<BarSaleItem> {
        val sales = db.barSaleDao().getBySession(sessionId)
        if (sales.isEmpty()) return emptyList()
        return db.barSaleDao().getItemsBySaleIds(sales.map { it.id })
    }

    suspend fun closeSession(
        tableId: Long,
        discount: Double = 0.0,
        paymentMethod: String = "Naqd",
        clubName: String = "BILLIARD CLUB"
    ): CheckReceipt {
        val session = db.sessionDao().getOpenByTable(tableId)
            ?: throw IllegalStateException("Ochiq sessiya topilmadi")
        val now = currentMillis()
        val tableCost = Billing.calculateTableCost(session.startTime, now, session.hourlyRate)
        val sales = db.barSaleDao().getBySession(session.id)
        var barSell = 0.0
        var barBuy = 0.0
        val allItems = if (sales.isEmpty()) emptyList() else db.barSaleDao().getItemsBySaleIds(sales.map { it.id })
        allItems.forEach {
            barSell += it.sellPrice * it.qty
            barBuy += it.buyPrice * it.qty
        }
        val total = (tableCost + barSell - discount).coerceAtLeast(0.0)
        val closed = session.copy(
            endTime = now,
            status = "CLOSED",
            tableSum = tableCost,
            barSum = barSell,
            discount = discount,
            totalSum = total
        )
        db.sessionDao().update(closed)
        // Stolga yozilgan bar savdolari shu paytda to'langan hisoblanadi:
        // hisobotda ular stol yopilgan kunga kiradi va bir marta hisoblanadi.
        db.barSaleDao().closeForSession(session.id, now)
        val details = "Stol:${tableCost.toLong()} Bar:${barSell.toLong()} Chegirma:${discount.toLong()}"
        val check = CheckReceipt(
            sessionId = session.id,
            createdAt = now,
            tableName = session.tableName,
            tableAmount = tableCost,
            barAmount = barSell,
            discount = discount,
            totalAmount = total,
            paymentMethod = paymentMethod,
            details = details
        )
        val checkId = db.checkDao().insert(check)
        log("TABLE_STOP", "${session.tableName} yakunlandi. Stol: ${tableCost.toLong()}, Bar: ${barSell.toLong()}, Jami: ${total.toLong()}", total)
        return check.copy(id = checkId)
    }

    suspend fun cancelSession(tableId: Long) {
        val session = db.sessionDao().getOpenByTable(tableId) ?: return
        // Stolga yozilgan bar itemlar bo'lsa stockni qaytarish kerak
        val sales = db.barSaleDao().getBySession(session.id)
        if (sales.isNotEmpty()) {
            val items = db.barSaleDao().getItemsBySaleIds(sales.map { it.id })
            items.forEach { db.productDao().increaseStock(it.productId, it.qty) }
        }
        val now = currentMillis()
        db.sessionDao().update(session.copy(status = "CANCELLED", endTime = now))
        // Stok qaytarilgan savdo hisobotda bar daromadi/foydasi bo'lib qolmasligi kerak.
        db.barSaleDao().cancelForSession(session.id, now)
        log("TABLE_STOP", "${session.tableName} sessiya bekor qilindi", 0.0)
    }

    // ---------- Bar / Products ----------
    suspend fun upsertProduct(p: Product): Long {
        val id = db.productDao().upsert(p)
        log("PRODUCT", "Mahsulot saqlandi: ${p.name} (kirish:${p.buyPrice.toLong()} sotish:${p.sellPrice.toLong()} qoldiq:${p.stockQty})", 0.0)
        return id
    }

    suspend fun deleteProduct(id: Long) {
        db.productDao().deleteById(id)
        log("PRODUCT", "Mahsulot o'chirildi id=$id", 0.0)
    }

    suspend fun addStock(productId: Long, qty: Double) {
        db.productDao().increaseStock(productId, qty)
        val p = db.productDao().getById(productId)
        log("PRODUCT", "Om Borga kirim: ${p?.name} +$qty ${p?.unit}", (p?.buyPrice ?: 0.0) * qty)
    }

    /** Stolga bar mahsuloti yozish (sessiya ochiq bo'lishi shart emas — bo'lmasa to'g'ridan-to'g'ri savdo bo'ladi).
     *  To'g'ridan-to'g'ri savdoda yaratilgan [CheckReceipt] ni qaytaradi, stolga yozishda null. */
    suspend fun sellToTable(sessionId: Long?, items: Map<Long, Double>, paymentMethod: String = "Naqd"): org.mexa.clube.data.entity.CheckReceipt? {
        val now = currentMillis()
        var totalBuy = 0.0
        var totalSell = 0.0
        val saleItems = mutableListOf<BarSaleItem>()
        // avval stock tekshirish
        for ((pid, qty) in items) {
            val p = db.productDao().getById(pid) ?: continue
            if (p.stockQty < qty) throw IllegalStateException("${p.name} da stok yetarli emas (${p.stockQty} ${p.unit})")
        }
        val sale = BarSale(sessionId = sessionId, createdAt = now, status = if (sessionId == null) "CLOSED" else "OPEN")
        val saleId = db.barSaleDao().insertSale(sale)
        for ((pid, qty) in items) {
            val p = db.productDao().getById(pid) ?: continue
            totalBuy += p.buyPrice * qty
            totalSell += p.sellPrice * qty
            saleItems.add(BarSaleItem(saleId = saleId, productId = pid, productName = p.name, qty = qty, buyPrice = p.buyPrice, sellPrice = p.sellPrice))
            db.productDao().decreaseStock(pid, qty)
        }
        db.barSaleDao().insertItems(saleItems)
        // sale totals yangilash
        db.barSaleDao().insertSale(sale.copy(id = saleId, totalBuy = totalBuy, totalSell = totalSell))
        if (sessionId == null) {
            // to'g'ridan-to'g'ri savdo uchun check yaratamiz
            val check = CheckReceipt(
                saleId = saleId,
                createdAt = now,
                tableName = "BAR",
                tableAmount = 0.0,
                barAmount = totalSell,
                totalAmount = totalSell,
                paymentMethod = paymentMethod,
                details = "To'g'ridan-to'g'ri bar savdosi"
            )
            val checkId = db.checkDao().insert(check)
            log("BAR_SALE", "Bar savdosi (to'g'ridan-to'g'ri): ${totalSell.toLong()} so'm", totalSell)
            return check.copy(id = checkId)
        } else {
            log("BAR_SALE", "Stolga bar yozildi (sessiya #$sessionId): ${totalSell.toLong()} so'm", totalSell)
            return null
        }
    }

    suspend fun itemsBySale(saleId: Long): List<BarSaleItem> = db.barSaleDao().getItems(saleId)

    /** Stol sessiyasiga yozilgan bitta bar pozitsiyasini o'chirish (stok qaytadi, tasdiq UI da). */
    suspend fun deleteBarItem(itemId: Long) {
        val item = db.barSaleDao().getItemById(itemId) ?: return
        db.productDao().increaseStock(item.productId, item.qty)
        db.barSaleDao().deleteItemById(itemId)
        val sale = db.barSaleDao().getSaleById(item.saleId)
        if (sale != null) {
            val rest = db.barSaleDao().getItems(item.saleId)
            if (rest.isEmpty()) {
                db.barSaleDao().deleteSaleById(item.saleId)
            } else {
                db.barSaleDao().updateSale(
                    sale.copy(
                        totalBuy = rest.sumOf { it.buyPrice * it.qty },
                        totalSell = rest.sumOf { it.sellPrice * it.qty }
                    )
                )
            }
        }
        log("BAR_SALE", "Bar pozitsiya o'chirildi: ${item.productName} x${item.qty} (stokga qaytdi)", 0.0)
    }

    // ---------- Expenses (xarajatlar) ----------
    suspend fun saveExpense(expense: Expense): Long {
        require(expense.reason.trim().isNotBlank()) { "Xarajat sababi kiritilishi shart" }
        require(expense.amount > 0.0) { "Xarajat summasi 0 dan katta bo'lishi kerak" }
        val saved = expense.copy(
            reason = expense.reason.trim(),
            category = expense.category.trim().ifBlank { "Boshqa" },
            note = expense.note.trim()
        )
        val id = db.expenseDao().upsert(saved)
        val action = if (expense.id == 0L) "qo'shildi" else "yangilandi"
        log("EXPENSE", "Xarajat $action: ${saved.reason} (${saved.category})", saved.amount)
        return if (saved.id == 0L) id else saved.id
    }

    suspend fun deleteExpense(id: Long) {
        val expense = db.expenseDao().getById(id)
        db.expenseDao().deleteById(id)
        if (expense != null) log("EXPENSE", "Xarajat o'chirildi: ${expense.reason}", expense.amount)
    }

    suspend fun expensesBetween(from: Long, to: Long): List<Expense> = db.expenseDao().getBetween(from, to)

    // ---------- Notes (zametkalar) ----------
    suspend fun saveNote(note: Note): Long {
        require(note.reason.trim().isNotBlank()) { "Zametka sababi kiritilishi shart" }
        require(note.amount >= 0.0) { "Summa manfiy bo'lishi mumkin emas" }
        val saved = note.copy(
            reason = note.reason.trim(),
            note = note.note.trim(),
            createdAt = if (note.id == 0L) currentMillis() else note.createdAt
        )
        val id = db.noteDao().upsert(saved)
        val action = if (note.id == 0L) "qo'shildi" else "yangilandi"
        log("NOTE", "Zametka $action: ${saved.reason}", saved.amount)
        return if (saved.id == 0L) id else saved.id
    }

    suspend fun deleteNote(id: Long) {
        val note = db.noteDao().getById(id)
        db.noteDao().deleteById(id)
        if (note != null) log("NOTE", "Zametka o'chirildi: ${note.reason}", note.amount)
    }

    // ---------- Reports ----------
    suspend fun report(from: Long, to: Long): ReportSummary {
        val sessions = db.sessionDao().getBetween(from, to).filter { it.status == "CLOSED" }
        // To'g'ridan-to'g'ri savdo — sotilgan paytda, stolga biriktirilgan savdo — stol yopilganda.
        // Bekor qilingan sessiyalarning savdolari hisobotga kirmaydi.
        val sales = db.barSaleDao().getForReport(from, to)
        val items = if (sales.isEmpty()) emptyList() else db.barSaleDao().getItemsBySaleIds(sales.map { it.id })
        val salesById = sales.associateBy { it.id }
        val tableRev = sessions.sumOf { it.tableSum }
        val discount = sessions.sumOf { it.discount }
        val expenseTotal = db.expenseDao().getBetween(from, to).sumOf { it.amount }

        var barTableRev = 0.0
        var barTableCost = 0.0
        var barDirectRev = 0.0
        var barDirectCost = 0.0
        items.forEach { item ->
            val sell = item.sellPrice * item.qty
            val buy = item.buyPrice * item.qty
            if (salesById[item.saleId]?.sessionId != null) {
                barTableRev += sell
                barTableCost += buy
            } else {
                barDirectRev += sell
                barDirectCost += buy
            }
        }
        val barRev = barTableRev + barDirectRev
        val barCost = barTableCost + barDirectCost
        val grouped = items.groupBy { it.productName }.map { (name, list) ->
            TopProduct(name, list.sumOf { it.qty }, list.sumOf { it.sellPrice * it.qty })
        }.sortedByDescending { it.revenue }.take(10)
        return ReportSummary(
            from = from, to = to,
            sessionsCount = sessions.size,
            tableRevenue = tableRev,
            barRevenue = barRev,
            barCost = barCost,
            barProfit = barRev - barCost,
            barTableRevenue = barTableRev,
            barTableCost = barTableCost,
            barTableProfit = barTableRev - barTableCost,
            barDirectRevenue = barDirectRev,
            barDirectCost = barDirectCost,
            barDirectProfit = barDirectRev - barDirectCost,
            discount = discount,
            totalRevenue = tableRev + barRev - discount,
            expenses = expenseTotal,
            netProfit = tableRev + barRev - discount - expenseTotal,
            topProducts = grouped
        )
    }

    /**
     * Eski bazalarni yangilash: ilgari stolga biriktirilgan savdolarda `closedAt` yozilmagan.
     * Yopilgan sessiyalar uchun savdo yopilish vaqtiga, bekor qilinganlar uchun CANCELLED ga o'tkaziladi.
     */
    suspend fun reconcileBarSales() {
        val pending = db.barSaleDao().getAttachedWithoutCloseTime()
        for (sale in pending) {
            val sessionId = sale.sessionId ?: continue
            val session = db.sessionDao().getById(sessionId) ?: continue
            val at = session.endTime ?: continue
            when (session.status) {
                "CLOSED" -> db.barSaleDao().markSaleStatus(sale.id, "CLOSED", at)
                "CANCELLED" -> db.barSaleDao().markSaleStatus(sale.id, "CANCELLED", at)
            }
        }
    }

    suspend fun checksBetween(from: Long, to: Long): List<CheckReceipt> = db.checkDao().getBetween(from, to)
    suspend fun sessionById(id: Long): TableSession? = db.sessionDao().getById(id)
    suspend fun closedSessionsBetween(from: Long, to: Long): List<TableSession> = db.sessionDao().getBetween(from, to)
    suspend fun lastChecks(limit: Int = 50): List<CheckReceipt> = db.checkDao().getLast(limit)
    suspend fun lastLogs(limit: Int = 100): List<MovementLog> = db.logDao().getLast(limit)

    private suspend fun log(type: String, desc: String, amount: Double) {
        db.logDao().insert(MovementLog(timestamp = currentMillis(), type = type, description = desc, amount = amount))
    }

    // ---------- Settings ----------
    suspend fun getSetting(key: String, default: String): String {
        return db.settingsDao().get(key)?.value ?: default
    }

    suspend fun setSetting(key: String, value: String) {
        db.settingsDao().put(AppSetting(key, value))
    }

    // ---------- Baza joylashuvi (portable .db) ----------

    /** Hozir ishlatilayotgan `.db` fayl yo'li. */
    fun databasePath(): String = openedDbPath

    /** Sozlamalarda saqlangan yo'l (standartdan farq qilsa). */
    fun configuredDatabasePath(): String? = DatabaseLocation.configuredPath()

    /** Qayta ishga tushirilgandan keyin qo'llanadigan, fayli mavjud yo'l. */
    fun pendingDatabasePath(): String? = DatabaseLocation.pendingPath()

    fun defaultDatabasePath(): String = DatabaseLocation.defaultPath()

    fun defaultBackupDir(): String = DatabaseLocation.backupDir()

    /** `null` — baza yaroqli, aks holda xato matni. */
    fun validateDatabase(path: String): String? = DatabaseLocation.validate(path)

    /** Yangi baza yo'lini eslab qoladi (dasturni qayta ishga tushirish kerak). */
    fun selectDatabase(path: String) = DatabaseLocation.usePath(path)

    /** Standart baza yo'liga qaytarish. */
    fun resetDatabasePath() = DatabaseLocation.resetToDefault()

    /** Izchil nusxa yaratadi va fayl yo'lini qaytaradi. */
    fun backupDatabase(targetDir: String): String =
        DatabaseLocation.backup(openedDbPath, targetDir)

    suspend fun ensureSeed(defaultPrice: Double = 60000.0) {
        if (db.tableDao().getAllTables().isEmpty()) {
            for (i in 1..6) {
                db.tableDao().upsert(
                    BilliardTable(name = "Stol-$i", hourlyPrice = defaultPrice, sortOrder = i)
                )
            }
            log("SYSTEM", "Boshlang'ich 6 ta stol yaratildi", 0.0)
        }
        if (db.productDao().getAll().isEmpty()) {
            val demo = listOf(
                Product(name = "Coca-Cola 0.5", category = "Ichimlik", buyPrice = 6000.0, sellPrice = 10000.0, stockQty = 50.0, unit = "dona"),
                Product(name = "Choy", category = "Ichimlik", buyPrice = 2000.0, sellPrice = 5000.0, stockQty = 100.0, unit = "dona"),
                Product(name = "Snickers", category = "Shirinlik", buyPrice = 8000.0, sellPrice = 12000.0, stockQty = 30.0, unit = "dona"),
                Product(name = "Suv 0.5", category = "Ichimlik", buyPrice = 3000.0, sellPrice = 5000.0, stockQty = 50.0, unit = "dona")
            )
            demo.forEach { db.productDao().upsert(it) }
        }
        if (db.categoryDao().getAll().isEmpty()) {
            val existingCats = db.categoryDao().getAll().map { it.name }.toSet()
            val defaults = listOf("Ichimlik", "Shirinlik", "Gazaklar", "Umumiy")
            // mahsulotlarda uchragan, lekin ro'yxatda yo'q kategoriyalarni ham qo'shamiz
            val fromProducts = db.productDao().getAll().map { it.category.trim() }.filter { it.isNotBlank() }
            (defaults + fromProducts).distinct().filter { it !in existingCats }.forEachIndexed { i, n ->
                try { db.categoryDao().insert(Category(name = n, sortOrder = i + 1)) } catch (_: Exception) {}
            }
        }
    }
}
