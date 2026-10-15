package org.mexa.clube.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Booking
import org.mexa.clube.data.entity.Category
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.MovementLog
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.TableSession
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.Billing
import org.mexa.clube.domain.ReportSummary
import org.mexa.clube.domain.buildCheckText
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatFileStamp
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.startOfDay
import org.mexa.clube.domain.startOfMonth
import org.mexa.clube.domain.startOfWeek

data class TableUi(
    val table: BilliardTable,
    val session: TableSession?,
    val elapsedSec: Long = 0,
    val currentSum: Double = 0.0,
    val barSum: Double = 0.0,
    val barCount: Int = 0,
    val booking: Booking? = null,
    val lastSession: TableSession? = null
)

class AppViewModel(private val repo: BilliardRepository) : ViewModel() {
    val tables: StateFlow<List<BilliardTable>> = repo.tables
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val products: StateFlow<List<Product>> = repo.products
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val openSessions: StateFlow<List<TableSession>> = repo.openSessions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<Category>> = repo.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _tick = MutableStateFlow(0L)

    private val _tablesUi = MutableStateFlow<List<TableUi>>(emptyList())
    val tablesUi: StateFlow<List<TableUi>> = _tablesUi.asStateFlow()

    // SessiyaId -> shu sessiyaga yozilgan bar itemlar (stol kartasi + ko'rish dialogi uchun)
    private val _sessionBars = MutableStateFlow<Map<Long, List<BarSaleItem>>>(emptyMap())
    val sessionBars: StateFlow<Map<Long, List<BarSaleItem>>> = _sessionBars.asStateFlow()

    val bookings: StateFlow<List<Booking>> = repo.bookings
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // StolId -> oxirgi yopilgan sessiya (bo'sh karta uchun)
    private val _lastClosed = MutableStateFlow<Map<Long, TableSession>>(emptyMap())

    val shiftStart = MutableStateFlow(0L)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _lastCheck = MutableStateFlow<CheckReceipt?>(null)
    val lastCheck = _lastCheck.asStateFlow()

    private val _lastCheckText = MutableStateFlow<String>("")
    val lastCheckText = _lastCheckText.asStateFlow()

    private val _lastCheckItems = MutableStateFlow<List<BarSaleItem>>(emptyList())
    val lastCheckItems = _lastCheckItems.asStateFlow()

    private val _lastCheckSession = MutableStateFlow<TableSession?>(null)
    val lastCheckSession = _lastCheckSession.asStateFlow()

    private val _report = MutableStateFlow<ReportSummary?>(null)
    val report = _report.asStateFlow()

    private val _checks = MutableStateFlow<List<CheckReceipt>>(emptyList())
    val checks = _checks.asStateFlow()

    private val _logs = MutableStateFlow<List<MovementLog>>(emptyList())
    val logs = _logs.asStateFlow()

    val clubName = MutableStateFlow("BILLIARD CLUB")
    val defaultPrice = MutableStateFlow(60000.0)

    // ---------- Portable baza (.db) holati ----------
    /** Dastur ishga tushganda ochilgan (hozir ishlatilayotgan) baza fayli. */
    val dbPath = MutableStateFlow("")
    /** Sozlamalarda tanlangan yo'l — qayta ishga tushirilgandan keyin qo'llanadi. */
    val dbConfiguredPath = MutableStateFlow<String?>(null)
    /** Qayta ishga tushirishda haqiqatan qo'llanadigan yo'l (fayl mavjud bo'lsa). */
    val dbPendingPath = MutableStateFlow<String?>(null)
    private val _dbMessage = MutableStateFlow<String?>(null)
    val dbMessage = _dbMessage.asStateFlow()

    fun clearDbMessage() { _dbMessage.value = null }

    fun refreshDatabaseInfo() {
        dbConfiguredPath.value = repo.configuredDatabasePath()
        dbPendingPath.value = repo.pendingDatabasePath()
    }

    /** Nusxa saqlash uchun standart papka. */
    fun defaultBackupDir(): String = repo.defaultBackupDir()

    /** Yangi `.db` tanlash — tekshiriladi va eslab qolinadi (qayta ishga tushirish kerak). */
    fun selectDatabase(path: String) = viewModelScope.launch {
        try {
            if (path.trim() == dbPath.value) {
                _dbMessage.value = "Bu baza allaqachon ishlatilmoqda."
                return@launch
            }
            withContext(Dispatchers.Default) { repo.selectDatabase(path) }
            refreshDatabaseInfo()
            _dbMessage.value = "✓ Saqlandi. Yangi bazani ishlatish uchun dasturni qayta ishga tushiring."
        } catch (e: Exception) {
            _error.value = e.message ?: "Bazani tanlab bo'lmadi"
        }
    }

    /** Standart baza yo'liga qaytish. */
    fun resetDatabasePath() = viewModelScope.launch {
        try {
            withContext(Dispatchers.Default) { repo.resetDatabasePath() }
            refreshDatabaseInfo()
            _dbMessage.value = "Standart joyga qaytarildi. Qayta ishga tushiring."
        } catch (e: Exception) {
            _error.value = e.message ?: "Standart joyga qaytarib bo'lmadi"
        }
    }

    /** Izchil nusxa (backup) yaratish. */
    fun backupDatabase(targetDir: String) = viewModelScope.launch {
        try {
            val p = withContext(Dispatchers.Default) { repo.backupDatabase(targetDir) }
            _dbMessage.value = "✓ Nusxa saqlandi: $p"
        } catch (e: Exception) {
            _error.value = e.message ?: "Nusxa yaratib bo'lmadi"
        }
    }

    private var ticker: Job? = null

    init {
        // Hozir ochilgan baza yo'lini bir marta eslab qolamiz.
        dbPath.value = repo.databasePath()
        refreshDatabaseInfo()
        viewModelScope.launch {
            try {
                clubName.value = repo.getSetting("club_name", "BILLIARD CLUB")
                defaultPrice.value = repo.getSetting("default_price", "60000").toDoubleOrNull() ?: 60000.0
                repo.ensureSeed(defaultPrice.value)
                val now = currentMillis()
                shiftStart.value = repo.getOrCreateShiftStart(startOfDay(now) + 10L * 3600 * 1000)
                exportDir.value = repo.getSetting("export_dir", "")
            } catch (e: Exception) {
                _error.value = "DB xato (seed): ${e.message}"
            }
        }
        // Har soniyada timer yangilash + tables/openSessions o'zgarishini kuzatish
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(tables, openSessions, _tick, _sessionBars, bookings, _lastClosed) { args ->
                @Suppress("UNCHECKED_CAST")
                val ts = args[0] as List<BilliardTable>
                val ss = args[1] as List<TableSession>
                val bars = args[3] as Map<Long, List<BarSaleItem>>
                val bs = args[4] as List<Booking>
                val last = args[5] as Map<Long, TableSession>
                val now = currentMillis()
                ts.map { t ->
                    val s = ss.firstOrNull { it.tableId == t.id }
                    if (s == null) TableUi(t, null, booking = bs.firstOrNull { it.tableId == t.id }, lastSession = last[t.id])
                    else {
                        val sum = Billing.calculateTableCost(s.startTime, now, s.hourlyRate)
                        val items = bars[s.id].orEmpty()
                        TableUi(
                            t, s, (now - s.startTime) / 1000, sum,
                            barSum = items.sumOf { it.sellPrice * it.qty },
                            barCount = items.size,
                            booking = null,
                            lastSession = last[t.id]
                        )
                    }
                }
            }.collect { _tablesUi.value = it }
        }
        ticker = viewModelScope.launch {
            var n = 0
            while (true) {
                delay(1000)
                _tick.value = currentMillis()
                // Bar yozuvlarini har 3 soniyada yangilab turamiz (stok/savdo o'zgarishi uchun)
                if (++n % 3 == 0) refreshSessionBars()
            }
        }
        viewModelScope.launch {
            // seed tugashini kutib, keyin barlarni yuklaymiz
            delay(1500)
            refreshSessionBars()
            refreshLastClosed()
        }
        viewModelScope.launch { loadReport(0) }
    }

    fun refreshSessionBars() = viewModelScope.launch {
        try {
            val ss = openSessions.value
            if (ss.isEmpty()) {
                if (_sessionBars.value.isNotEmpty()) _sessionBars.value = emptyMap()
                return@launch
            }
            _sessionBars.value = ss.associate { it.id to repo.barItemsForSession(it.id) }
        } catch (_: Exception) {}
    }

    fun refreshLastClosed() = viewModelScope.launch {
        try {
            val ts = tables.value
            if (ts.isEmpty()) return@launch
            _lastClosed.value = ts.mapNotNull { t ->
                repo.lastClosedByTable(t.id)?.let { t.id to it }
            }.toMap()
        } catch (_: Exception) {}
    }

    private fun refreshTablesUi() {
        // timer tick orqali avtomatik yangilanadi — qo'lda majburlash
        _tick.value = currentMillis()
    }

    fun clearError() { _error.value = null }

    fun addTable(name: String, price: Double, zone: String = "Standart") = viewModelScope.launch {
        try { repo.addTable(name, price, zone); refreshLastClosed() } catch (e: Exception) { _error.value = e.message }
    }

    fun updateTable(t: BilliardTable) = viewModelScope.launch {
        try { repo.updateTable(t) } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteTable(t: BilliardTable) = viewModelScope.launch {
        try { repo.deleteTable(t.id, t.name) } catch (e: Exception) { _error.value = e.message }
    }

    fun startSession(t: BilliardTable) = viewModelScope.launch {
        try { repo.startSession(t); refreshTablesUi(); refreshSessionBars() } catch (e: Exception) { _error.value = e.message }
    }

    fun addBooking(tableId: Long, client: String, phone: String, timeMillis: Long, note: String = "") =
        viewModelScope.launch {
            try { repo.addBooking(tableId, client, phone, timeMillis, note) } catch (e: Exception) { _error.value = e.message }
        }

    fun updateBooking(b: Booking) = viewModelScope.launch {
        try { repo.updateBooking(b) } catch (e: Exception) { _error.value = e.message }
    }

    fun cancelBooking(id: Long) = viewModelScope.launch {
        try { repo.cancelBooking(id) } catch (e: Exception) { _error.value = e.message }
    }

    fun startFromBooking(bookingId: Long) = viewModelScope.launch {
        try { repo.startFromBooking(bookingId); refreshTablesUi(); refreshSessionBars() } catch (e: Exception) { _error.value = e.message }
    }

    fun cancelSession(tableId: Long) = viewModelScope.launch {
        try { repo.cancelSession(tableId); refreshTablesUi(); refreshSessionBars() } catch (e: Exception) { _error.value = e.message }
    }

    fun closeSession(tableId: Long, discount: Double, payment: String, onDone: (CheckReceipt) -> Unit = {}) =
        viewModelScope.launch {
            try {
                val check = repo.closeSession(tableId, discount, payment, clubName.value)
                val session = repo.report(0, 1) // dummy to keep flow
                // check matnini tayyorlash
                val items = if (check.sessionId != null) repo.barItemsForSession(check.sessionId!!) else emptyList()
                // sessiya ma'lumotini topish uchun oxirgi yopilganlar orasidan
                _lastCheck.value = check
                _lastCheckItems.value = items
                _lastCheckSession.value = null
                _lastCheckText.value = buildCheckText(clubName.value, null, items, check).let {
                    // stol nomini qo'lda qo'shamiz
                    it.replace("BAR:", "Stol: ${check.tableName}\nStol summasi: ${formatMoney(check.tableAmount)}\n---------------------------------\nBAR:")
                }
                // To'liq matn: sessiya bilan
                val fullText = buildFullCheck(check, items)
                _lastCheckText.value = fullText
                refreshTablesUi()
                refreshSessionBars()
                refreshLastClosed()
                loadReport(0)
                onDone(check)
            } catch (e: Exception) { _error.value = e.message }
        }

    private suspend fun buildFullCheck(check: CheckReceipt, items: List<BarSaleItem>): String {
        // sessiya topilmasa ham check ichidagi summalar bilan chiroyli matn
        val sb = StringBuilder()
        sb.appendLine("=================================")
        sb.appendLine("        ${clubName.value}")
        sb.appendLine("=================================")
        sb.appendLine("Chek #${check.id}  ${org.mexa.clube.domain.formatTime(check.createdAt)}")
        sb.appendLine("Stol: ${check.tableName}")
        sb.appendLine("To'lov: ${check.paymentMethod}")
        sb.appendLine("---------------------------------")
        sb.appendLine("Stol summasi: ${formatMoney(check.tableAmount)}")
        if (items.isNotEmpty()) {
            sb.appendLine("---------------------------------")
            sb.appendLine("BAR:")
            items.forEachIndexed { i, it ->
                sb.appendLine("${i + 1}. ${it.productName} x${if (it.qty % 1.0 == 0.0) it.qty.toLong().toString() else it.qty.toString()}")
                sb.appendLine("   ${formatMoney(it.sellPrice)} = ${formatMoney(it.sellPrice * it.qty)}")
            }
            sb.appendLine("Bar summasi: ${formatMoney(check.barAmount)}")
        }
        sb.appendLine("---------------------------------")
        if (check.discount > 0) sb.appendLine("Chegirma: ${formatMoney(check.discount)}")
        sb.appendLine("JAMI: ${formatMoney(check.totalAmount)}")
        sb.appendLine("=================================")
        sb.appendLine("Xizmatimizdan mamnun bo'ling!")
        sb.appendLine("=================================")
        return sb.toString()
    }

    fun saveProduct(p: Product) = viewModelScope.launch {
        try { repo.upsertProduct(p) } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteProduct(id: Long) = viewModelScope.launch {
        try { repo.deleteProduct(id) } catch (e: Exception) { _error.value = e.message }
    }

    fun addCategory(name: String) = viewModelScope.launch {
        try { repo.addCategory(name) } catch (e: Exception) { _error.value = e.message }
    }

    fun renameCategory(oldName: String, newName: String) = viewModelScope.launch {
        try { repo.renameCategory(oldName, newName) } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteCategory(id: Long, name: String) = viewModelScope.launch {
        try { repo.deleteCategory(id, name) } catch (e: Exception) { _error.value = e.message }
    }

    fun addStock(id: Long, qty: Double) = viewModelScope.launch {
        try { repo.addStock(id, qty) } catch (e: Exception) { _error.value = e.message }
    }

    fun sellToTable(
        sessionId: Long?,
        items: Map<Long, Double>,
        paymentMethod: String = "Naqd",
        onDone: (CheckReceipt?) -> Unit = {}
    ) =
        viewModelScope.launch {
            try {
                val check = repo.sellToTable(sessionId, items, paymentMethod)
                if (check?.saleId != null) {
                    val saleItems = repo.itemsBySale(check.saleId!!)
                    _lastCheck.value = check
                    _lastCheckItems.value = saleItems
                    _lastCheckSession.value = null
                    _lastCheckText.value = buildCheckText(clubName.value, null, saleItems, check)
                }
                refreshTablesUi()
                refreshSessionBars()
                loadReport(0)
                onDone(check)
            } catch (e: Exception) { _error.value = e.message }
        }

    fun deleteBarItem(item: BarSaleItem, onDone: () -> Unit = {}) = viewModelScope.launch {
        try {
            repo.deleteBarItem(item.id)
            refreshSessionBars()
            loadReport(0)
            onDone()
        } catch (e: Exception) { _error.value = e.message }
    }

    /** Eski chekni qayta chop etish uchun matnini tiklaydi (Chek dialogi ochiladi). */
    fun reprintCheck(check: CheckReceipt, onDone: () -> Unit = {}) = viewModelScope.launch {
        try {
            val session = check.sessionId?.let { repo.sessionById(it) }
            val items = when {
                check.sessionId != null -> repo.barItemsForSession(check.sessionId!!)
                check.saleId != null -> repo.itemsBySale(check.saleId!!)
                else -> emptyList()
            }
            _lastCheck.value = check
            _lastCheckItems.value = items
            _lastCheckSession.value = session
            _lastCheckText.value = if (session != null) buildFullCheck(check, items)
            else buildCheckText(clubName.value, null, items, check)
            onDone()
        } catch (e: Exception) { _error.value = e.message }
    }

    suspend fun barItems(sessionId: Long): List<BarSaleItem> = repo.barItemsForSession(sessionId)

    /** period: 0-kunlik, 1-haftalik, 2-oylik, 3-barchasi */
    fun loadReport(period: Int) = viewModelScope.launch {
        try {
            val now = currentMillis()
            val (from, to) = periodRange(period, now)
            _report.value = repo.report(from, to)
            _checks.value = repo.checksBetween(from, to)
        } catch (e: Exception) { _error.value = e.message }
    }

    /** Ixtiyoriy oraliq bo'yicha hisobot (DD.MM.YYYY oralig'i). */
    fun loadCustomReport(from: Long, toExclusive: Long) = viewModelScope.launch {
        try {
            _report.value = repo.report(from, toExclusive)
            _checks.value = repo.checksBetween(from, toExclusive)
        } catch (e: Exception) { _error.value = e.message }
    }

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult = _exportResult.asStateFlow()
    fun clearExportResult() { _exportResult.value = null }

    val exportDir = MutableStateFlow("")
    fun saveExportDir(path: String) = viewModelScope.launch {
        try {
            // Bo'sh bo'lsa standart papka (~/.billiardapp/eksport) ishlatiladi
            val p = path.trim()
            repo.setSetting("export_dir", p)
            exportDir.value = p
        } catch (e: Exception) { _error.value = e.message }
    }

    /** Aniq oraliq bo'yicha eksport (Oraliq panelidan). Ekranni ham shu oraliqqa yangilaydi. */
    fun exportRange(from: Long, toExclusive: Long, onDone: (String) -> Unit = {}) =
        viewModelScope.launch {
            try {
                exportRangeInternal(from, toExclusive, onDone)
            } catch (e: Exception) { _error.value = "Eksport xatosi: ${e.message}" }
        }

    private suspend fun exportRangeInternal(from: Long, to: Long, onDone: (String) -> Unit) {
        val summary = repo.report(from, to)
        val sessions = repo.closedSessionsBetween(from, to)
        val checks = repo.checksBetween(from, to)
        var dir = repo.getSetting("export_dir", "")
        if (dir.isBlank()) {
            dir = org.mexa.clube.ui.export.ExcelExporter.defaultDir()
            repo.setSetting("export_dir", dir)
            exportDir.value = dir
        }
        val stamp = formatFileStamp(currentMillis())
        val path = org.mexa.clube.ui.export.ExcelExporter.exportPeriodReport(
            "$dir/hisobot_$stamp.xlsx", from, to, summary, sessions, checks
        )
        // Eksport qilingan oraliq ekranda ham ko'rinsin
        _report.value = summary
        _checks.value = checks
        _exportResult.value = path
        onDone(path)
    }

    fun loadLogs() = viewModelScope.launch {
        try { _logs.value = repo.lastLogs(200) } catch (e: Exception) { _error.value = e.message }
    }

    companion object {
        /** period: 0-kunlik, 1-haftalik, 2-oylik, else-barchasi. Returns (from, toExclusive). */
        fun periodRange(period: Int, now: Long): Pair<Long, Long> = when (period) {
            0 -> startOfDay(now) to (startOfDay(now) + 24L * 3600 * 1000)
            1 -> startOfWeek(now) to (startOfWeek(now) + 7L * 24 * 3600 * 1000)
            2 -> {
                val s = startOfMonth(now)
                val next = startOfMonth(s + 32L * 24 * 3600 * 1000)
                s to next
            }
            else -> 0L to (now + 24L * 3600 * 1000)
        }
    }

    fun saveSettings(name: String, price: Double) = viewModelScope.launch {
        try {
            repo.setSetting("club_name", name)
            repo.setSetting("default_price", price.toString())
            clubName.value = name
            defaultPrice.value = price
        } catch (e: Exception) { _error.value = e.message }
    }
}
