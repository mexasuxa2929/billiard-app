package org.mexa.clube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Booking
import org.mexa.clube.data.entity.TABLE_ZONES
import org.mexa.clube.domain.Billing
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatHourMinute
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.todayAt
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ConfirmDialog
import org.mexa.clube.ui.print.CheckPrinter
import org.mexa.clube.ui.theme.CategoryPill
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProButton
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.theme.ProSearch
import org.mexa.clube.ui.theme.StatChip
import org.mexa.clube.ui.viewmodel.AppViewModel
import org.mexa.clube.ui.viewmodel.TableUi

private val BusyBorder = Color(0xFF6B2430)
private val FreeBorder = Color(0xFF1E5E3C)
private val BookBorder = Color(0xFF6B4E12)
private val ZoneVip = Color(0xFFFFB020)
private val ZoneSnooker = Color(0xFFB388FF)

@Composable
private fun zoneBadgeColor(zone: String): Color = when (zone) {
    "VIP", "VIP LOUNGE" -> ZoneVip
    "Snooker" -> ZoneSnooker
    else -> Pro.TextDim
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TablesScreen(vm: AppViewModel) {
    val tablesUi by vm.tablesUi.collectAsState()
    val products by vm.products.collectAsState()
    val checkText by vm.lastCheckText.collectAsState()
    val report by vm.report.collectAsState()
    val shiftStart by vm.shiftStart.collectAsState()

    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(0) } // 0 barchasi, 1 bo'sh, 2 band, 3 bron
    var zoneFilter by remember { mutableStateOf("Barchasi") }

    var showAdd by remember { mutableStateOf(false) }
    var editTable by remember { mutableStateOf<BilliardTable?>(null) }
    var closeUi by remember { mutableStateOf<TableUi?>(null) }
    var partsUi by remember { mutableStateOf<TableUi?>(null) }
    var barUi by remember { mutableStateOf<TableUi?>(null) }
    var showCheck by remember { mutableStateOf(false) }
    var deleteTable by remember { mutableStateOf<BilliardTable?>(null) }
    var cancelUi by remember { mutableStateOf<TableUi?>(null) }
    var viewBarUi by remember { mutableStateOf<TableUi?>(null) }
    var deleteItem by remember { mutableStateOf<org.mexa.clube.data.entity.BarSaleItem?>(null) }
    var quickBook by remember { mutableStateOf(false) }
    var quickBookTable by remember { mutableStateOf<BilliardTable?>(null) }
    var editBooking by remember { mutableStateOf<Booking?>(null) }
    var cancelBooking by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(Unit) { vm.loadReport(0) }

    val busyCount = tablesUi.count { it.session != null }
    val bookedCount = tablesUi.count { it.session == null && it.booking != null }
    val freeCount = tablesUi.size - busyCount
    val zones = remember(tablesUi) {
        listOf("Barchasi") + tablesUi.map { it.table.zone.trim().ifBlank { "Standart" } }.distinct().sorted()
    }
    if (zoneFilter != "Barchasi" && !zones.contains(zoneFilter)) zoneFilter = "Barchasi"

    val visible = tablesUi.filter { ui ->
        val statusOk = when (statusFilter) {
            1 -> ui.session == null && ui.booking == null
            2 -> ui.session != null
            3 -> ui.session == null && ui.booking != null
            else -> true
        }
        val zoneOk = zoneFilter == "Barchasi" || ui.table.zone == zoneFilter
        val searchOk = search.isBlank() ||
            ui.table.name.contains(search, ignoreCase = true) ||
            ui.table.zone.contains(search, ignoreCase = true)
        statusOk && zoneOk && searchOk
    }

    val pendingTotal = tablesUi.filter { it.session != null }.sumOf { it.currentSum + it.barSum }
    // har soniyada qayta chiziladi (tablesUi tick orqali yangilanadi)
    val nowClock = formatHourMinute(currentMillis()) + ":" +
        padSec((currentMillis() / 1000) % 60)

    ResponsiveScreen { m ->
        // ===== Header =====
        if (m.compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Pro.Surface)
                            .border(1.dp, Pro.CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("🎱", fontSize = 16.sp) }
                    Spacer(Modifier.width(8.dp))
                    Text("Stollar Boshqaruvi", color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.background(Pro.GreenBg, RoundedCornerShape(16.dp))
                            .border(1.dp, Pro.GreenBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("Smena ochiq", color = Pro.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkButton("🧾  Oxirgi\nchek", { showCheck = true })
                    DarkButton("📅  Tezkor\nbron", { quickBook = true })
                    ProButton("Stol\nqo'shish", { showAdd = true }, "+")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Pro.Surface)
                        .border(1.dp, Pro.CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("🎱", fontSize = 18.sp) }
                Spacer(Modifier.width(10.dp))
                Text("Stollar Boshqaruvi", color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.background(Pro.GreenBg, RoundedCornerShape(16.dp))
                        .border(1.dp, Pro.GreenBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("Smena ochiq", color = Pro.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkButton("🧾  Oxirgi\nchek", { showCheck = true })
                    DarkButton("📅  Tezkor\nbron", { quickBook = true })
                    ProButton("Stol\nqo'shish", { showAdd = true }, "+")
                }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip("Jami stollar:", "${tablesUi.size} ta")
            StatChip("Band stollar:", "$busyCount ta", Pro.Red)
            StatChip("Bo'sh:", "$freeCount ta", Pro.Green)
            StatChip("Bugungi tushum:", formatMoney(report?.totalRevenue ?: 0.0), Pro.Green)
        }

        // ===== Filter bar =====
        ProCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(if (m.compact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProSearch(search, { search = it }, "Qidirish... (stol raqami, zali)")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryPill("Barchasi (${tablesUi.size})", statusFilter == 0) { statusFilter = 0 }
                    CategoryPill("Bo'sh stollar ($freeCount)", statusFilter == 1) { statusFilter = 1 }
                    CategoryPill("Band / O'yinda ($busyCount)", statusFilter == 2) { statusFilter = 2 }
                    CategoryPill("Bron qilingan ($bookedCount)", statusFilter == 3) { statusFilter = 3 }
                    zones.filter { it != "Barchasi" }.forEach { z ->
                        val n = tablesUi.count { it.table.zone == z }
                        CategoryPill("$z ($n)", zoneFilter == z) {
                            zoneFilter = if (zoneFilter == z) "Barchasi" else z
                        }
                    }
                }
            }
        }

        // ===== Grid =====
        if (visible.isEmpty()) {
            ProCard(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Hech narsa topilmadi", color = Pro.TextFaint)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (m.compact) 260.dp else 270.dp),
                verticalArrangement = Arrangement.spacedBy(m.cardSpacing),
                horizontalArrangement = Arrangement.spacedBy(m.cardSpacing),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.table.id }) { ui ->
                    when {
                        ui.session != null -> BusyCard(
                            ui = ui,
                            onOrder = { barUi = ui },
                            onStop = { closeUi = ui },
                            onViewBar = { viewBarUi = ui },
                            onNewPart = { vm.startNewPart(ui.table.id) },
                            onViewParts = { partsUi = ui },
                            onEdit = { editTable = ui.table },
                            onDelete = { deleteTable = ui.table }
                        )
                        ui.booking != null -> BookedCard(
                            ui = ui,
                            booking = ui.booking!!,
                            onStartEarly = { vm.startFromBooking(ui.booking!!.id) },
                            onEditBook = { editBooking = ui.booking },
                            onCancelBook = { cancelBooking = ui.booking },
                            onEdit = { editTable = ui.table },
                            onDelete = { deleteTable = ui.table }
                        )
                        else -> FreeCard(
                            ui = ui,
                            onStart = { vm.startSession(ui.table) },
                            onBook = { quickBookTable = ui.table; quickBook = true },
                            onEdit = { editTable = ui.table },
                            onDelete = { deleteTable = ui.table }
                        )
                    }
                }
            }
        }

        // ===== Footer =====
        ProCard(Modifier.fillMaxWidth()) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = if (m.compact) 10.dp else 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(if (m.compact) 10.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DotText(Pro.Green, "Faol o'yinlar: $busyCount ta")
                Text("Kutilayotgan umumiy hisob: ${formatMoney(pendingTotal)}", color = Pro.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Smena ochilgan vaqti: ${if (shiftStart > 0) "Bugun, " + formatHourMinute(shiftStart) else "—"}",
                    color = Pro.TextDim, fontSize = 12.sp
                )
                DotText(Pro.Green, "Server: Barqaror")
                Text(nowClock, color = Pro.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }

    if (showAdd) {
        TableEditDialog(title = "Yangi stol", priceDefault = vm.defaultPrice.collectAsState().value, onDismiss = { showAdd = false }) { name, price, zone ->
            vm.addTable(name, price, zone); showAdd = false
        }
    }
    editTable?.let { t ->
        TableEditDialog(title = "Stolni tahrirlash", initial = t, priceDefault = t.hourlyPrice, onDismiss = { editTable = null }) { name, price, zone ->
            vm.updateTable(t.copy(name = name, hourlyPrice = price, zone = zone)); editTable = null
        }
    }
    partsUi?.let { snapshot ->
        val live = tablesUi.firstOrNull { it.table.id == snapshot.table.id } ?: snapshot
        PartsDialog(ui = live, onDismiss = { partsUi = null })
    }
    closeUi?.let { ui ->
        CloseSessionDialog(ui = ui, onDismiss = { closeUi = null }) { discount, payment ->
            vm.closeSession(ui.table.id, discount, payment) { showCheck = true }
            closeUi = null
        }
    }
    barUi?.let { ui ->
        val sess = ui.session
        if (sess != null) {
            AddBarDialog(products = products, onDismiss = { barUi = null }) { map ->
                vm.sellToTable(sess.id, map) { barUi = null }
            }
        }
    }
    if (showCheck) {
        CheckDialog(text = checkText.ifBlank { "Hali chek yo'q" }, onDismiss = { showCheck = false })
    }
    deleteTable?.let { t ->
        ConfirmDialog(
            title = "Stolni o'chirish?",
            message = "«${t.name}» ro'yxatdan butunlay o'chiriladi. Davom etasizmi?",
            onDismiss = { deleteTable = null },
            onConfirm = { vm.deleteTable(t); deleteTable = null }
        )
    }
    cancelUi?.let { ui ->
        ConfirmDialog(
            title = "Sessiyani bekor qilish?",
            message = "«${ui.table.name}» dagi ochiq sessiya bekor qilinadi. Stolga yozilgan bar mahsulotlari omborga qaytariladi.",
            confirmText = "Bekor qilish",
            onDismiss = { cancelUi = null },
            onConfirm = { vm.cancelSession(ui.table.id); cancelUi = null }
        )
    }
    viewBarUi?.let { snapshot ->
        val sess = snapshot.session
        if (sess != null) {
            // Karta har soniyada yangilanadi — dialogda joriy stol summasini ko'rsatamiz
            val live = tablesUi.firstOrNull { it.table.id == snapshot.table.id } ?: snapshot
            SessionBarDialog(
                ui = live,
                items = vm.sessionBars.collectAsState().value[sess.id].orEmpty(),
                onDismiss = { viewBarUi = null },
                onAddMore = { viewBarUi = null; barUi = live },
                onDelete = { deleteItem = it }
            )
        }
    }
    deleteItem?.let { item ->
        ConfirmDialog(
            title = "Bar pozitsiyani o'chirish?",
            message = "«${item.productName}» ro'yxatdan o'chiriladi va stokga qaytariladi.",
            onDismiss = { deleteItem = null },
            onConfirm = { vm.deleteBarItem(item) { deleteItem = null; vm.refreshSessionBars() } }
        )
    }
    if (quickBook) {
        val freeTables = tablesUi.filter { it.session == null && it.booking == null }.map { it.table }
        BookingDialog(
            tables = freeTables, preset = null, presetTableId = quickBookTable?.id,
            onDismiss = { quickBook = false; quickBookTable = null },
            onSave = { tableId, client, phone, time, note ->
                vm.addBooking(tableId, client, phone, time, note); quickBook = false; quickBookTable = null
            }
        )
    }
    editBooking?.let { b ->
        val tbl = tablesUi.firstOrNull { it.table.id == b.tableId }?.table
        BookingDialog(
            tables = listOfNotNull(tbl), preset = b, presetTableId = null,
            onDismiss = { editBooking = null },
            onSave = { _, client, phone, time, note ->
                vm.updateBooking(b.copy(clientName = client, phone = phone, timeMillis = time, note = note)); editBooking = null
            }
        )
    }
    cancelBooking?.let { b ->
        ConfirmDialog(
            title = "Bronni bekor qilish?",
            message = "«${b.tableName}» — ${b.clientName} broni bekor qilinadi.",
            confirmText = "Bekor qilish",
            onDismiss = { cancelBooking = null },
            onConfirm = { vm.cancelBooking(b.id); cancelBooking = null }
        )
    }
}

// kichik yordamchi: soniyani 2 xonali formatlash
private fun padSec(v: Long): String = if (v < 10) "0$v" else "$v"

@Composable
private fun DotText(dot: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(text, color = Pro.TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun DarkButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Pro.CardBorder)
    ) {
        Text(text, color = Pro.TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
    }
}

@Composable
private fun CardHeader(name: String, zone: String, price: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                name, Modifier.weight(1f, fill = false),
                color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Box(
                Modifier.background(zoneBadgeColor(zone).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, zoneBadgeColor(zone).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) { Text(zone, color = zoneBadgeColor(zone), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
        }
        Box {
            TextButton(onClick = { menu = true }) { Text("⋮", color = Pro.TextDim) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Tahrirlash") }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text("O'chirish") }, onClick = { menu = false; onDelete() })
            }
        }
    }
    Text(price, color = Pro.TextFaint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun BusyCard(
    ui: TableUi,
    onOrder: () -> Unit,
    onStop: () -> Unit,
    onViewBar: () -> Unit,
    onNewPart: () -> Unit,
    onViewParts: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val now = currentMillis()
    ProCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().border(1.dp, BusyBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardHeader(ui.table.name, ui.table.zone.ifBlank { "Standart" }, "${formatMoney(ui.table.hourlyPrice)}/soat", onEdit, onDelete)
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF2A1218), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Pro.Red))
                        Text("O'yinda (Band)", color = Pro.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    // Joriy (ochiq) partiya taymeri
                    Text(
                        "Partiya #${ui.partCount + 1}: " + Billing.durationText(ui.partStart, now),
                        color = Pro.TextFaint, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Billing.durationText(ui.session!!.startTime, now),
                        color = Pro.Red, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                    Text("jami vaqt", color = Pro.TextFaint, fontSize = 9.sp)
                }
            }
            KV("Stol hisobi:", formatMoney(ui.currentSum))
            if (ui.partCount > 0) {
                Row(
                    Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp))
                        .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Yopilgan partiyalar: ${ui.partCount} ta", color = Pro.TextFaint, fontSize = 11.sp)
                        Text(
                            formatMoney(ui.partsSum),
                            color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onViewParts,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Pro.CardBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("📋 Ko'rish", color = Pro.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
            // "Taymerni yangilash" — mijoz keyingi partiyaga o'tsa bosiladi
            OutlinedButton(
                onClick = onNewPart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Pro.Amber)
            ) {
                Text(
                    "⏱  Taymerni yangilash (partiya #${ui.partCount + 1})",
                    color = Pro.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
            Row(
                Modifier.fillMaxWidth()
                    .background(Pro.Surface, RoundedCornerShape(8.dp))
                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Bar hisobi", color = Pro.TextFaint, fontSize = 11.sp)
                    Text(
                        formatMoney(ui.barSum),
                        color = if (ui.barCount > 0) Pro.Green else Pro.TextDim,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onViewBar,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pro.GreenBorder),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (ui.barCount > 0) "🍺 Zakazlar (${ui.barCount})" else "🍺 Zakazlar",
                        color = Pro.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().background(Pro.GreenBg, RoundedCornerShape(8.dp))
                    .border(1.dp, Pro.GreenBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jami hisob:", color = Pro.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(formatMoney(ui.currentSum + ui.barSum), color = Pro.Green, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOrder, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pro.CardBorder)
                ) { Text("🛍 + Buyurtma", color = Pro.TextMain, fontSize = 12.sp) }
                Button(
                    onClick = onStop, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pro.Red, contentColor = Color.White)
                ) { Text("✕ To'xtatish", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun FreeCard(ui: TableUi, onStart: () -> Unit, onBook: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ProCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().border(1.dp, FreeBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardHeader(ui.table.name, ui.table.zone.ifBlank { "Standart" }, "${formatMoney(ui.table.hourlyPrice)}/soat", onEdit, onDelete)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Pro.Green))
                Text("Bo'sh", color = Pro.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            val last = ui.lastSession
            if (last?.endTime != null) {
                val dur = Billing.durationText(last.startTime, last.endTime!!)
                KV("Oxirgi seans:", dur)
                KV("Oxirgi tushum:", formatMoney(last.totalSum))
            } else {
                KV("Holati:", "O'yinga tayyor")
                KV("Bron:", "Yo'q")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A))
                ) { Text("▶  Boshlash", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onBook,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pro.CardBorder)
                ) { Text("📅", color = Pro.TextDim) }
            }
        }
    }
}

@Composable
private fun BookedCard(
    ui: TableUi, booking: Booking,
    onStartEarly: () -> Unit, onEditBook: () -> Unit, onCancelBook: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit
) {
    ProCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().border(1.dp, BookBorder, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardHeader(ui.table.name, ui.table.zone.ifBlank { "Standart" }, "${formatMoney(ui.table.hourlyPrice)}/soat", onEdit, onDelete)
            Row(
                Modifier.fillMaxWidth().background(Pro.AmberBg, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Pro.Amber))
                    Text("Bron qilingan", color = Pro.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${formatHourMinute(booking.timeMillis)} ga", color = Pro.Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            KV("Mijoz:", booking.clientName)
            KV("Tel:", booking.phone.ifBlank { "—" })
            if (booking.note.isNotBlank()) KV("Izoh:", booking.note)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEditBook, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pro.CardBorder)
                ) { Text("O'zgartirish", color = Pro.TextDim, fontSize = 12.sp) }
                Button(
                    onClick = onStartEarly, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pro.Amber, contentColor = Color(0xFF2A1A00))
                ) { Text("Erta boshlash", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
            TextButton(onClick = onCancelBook, modifier = Modifier.fillMaxWidth()) {
                Text("Bronni bekor qilish", color = Pro.Red, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = Pro.TextFaint, fontSize = 12.sp)
        Text(
            v, Modifier.weight(1f).padding(start = 8.dp),
            color = Pro.TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TableEditDialog(
    title: String, initial: BilliardTable? = null, priceDefault: Double,
    onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf((initial?.hourlyPrice ?: priceDefault).toLong().toString()) }
    var zone by remember { mutableStateOf(initial?.zone?.trim().orEmpty().ifBlank { "Standart" }) }
    var zoneEx by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Stol nomi (masalan: Stol-1, VIP-1)") })
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() } }, label = { Text("Soatlik narx (so'm)") })
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = zone, onValueChange = {}, readOnly = true,
                        label = { Text("Zona *") },
                        trailingIcon = { Text(if (zoneEx) "▲" else "▼", color = Pro.TextDim) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                            .clickable { zoneEx = true }
                    )
                    DropdownMenu(expanded = zoneEx, onDismissRequest = { zoneEx = false }) {
                        TABLE_ZONES.forEach { z ->
                            DropdownMenuItem(
                                text = { Text(z, color = if (z == zone) Pro.Green else Pro.TextMain) },
                                onClick = { zone = z; zoneEx = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                onSave(name.trim(), price.toDoubleOrNull() ?: priceDefault, zone.trim().ifBlank { "Standart" })
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } }
    )
}

@Composable
private fun BookingDialog(
    tables: List<BilliardTable>,
    preset: Booking?,
    presetTableId: Long?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Long, String) -> Unit
) {
    var tableId by remember { mutableStateOf(preset?.tableId ?: presetTableId ?: tables.firstOrNull()?.id ?: 0L) }
    var tableEx by remember { mutableStateOf(false) }
    var client by remember { mutableStateOf(preset?.clientName ?: "") }
    var phone by remember { mutableStateOf(preset?.phone ?: "") }
    var time by remember {
        mutableStateOf(
            if (preset != null && preset.timeMillis > 0) formatHourMinute(preset.timeMillis) else ""
        )
    }
    var note by remember { mutableStateOf(preset?.note ?: "") }
    var err by remember { mutableStateOf<String?>(null) }
    val tableName = tables.firstOrNull { it.id == tableId }?.name ?: preset?.tableName ?: ""
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset == null) "📅 Tezkor bron" else "Bronni o'zgartirish") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preset == null) {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tableName.ifBlank { "Stol tanlang" }, onValueChange = {}, readOnly = true,
                            label = { Text("Stol *") },
                            trailingIcon = { Text(if (tableEx) "▲" else "▼", color = Pro.TextDim) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                                .clickable { tableEx = true }
                        )
                        DropdownMenu(expanded = tableEx, onDismissRequest = { tableEx = false }) {
                            tables.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.name} • ${t.zone}") },
                                    onClick = { tableId = t.id; tableEx = false }
                                )
                            }
                        }
                    }
                } else {
                    Text("Stol: $tableName", color = Pro.TextDim)
                }
                OutlinedTextField(client, { client = it }, label = { Text("Mijoz ismi *") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") })
                OutlinedTextField(time, { time = it.filter { c -> c.isDigit() || c == ':' } }, label = { Text("Vaqt (masalan 10:30) *") })
                OutlinedTextField(note, { note = it }, label = { Text("Izoh") })
                if (err != null) Text(err!!, color = Pro.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (preset == null && tableId == 0L) { err = "Bo'sh stol tanlang"; return@Button }
                if (client.isBlank()) { err = "Mijoz ismini kiriting"; return@Button }
                val t = todayAt(time, currentMillis())
                if (t == null) { err = "Vaqt formati noto'g'ri (masalan 10:30)"; return@Button }
                onSave(tableId, client.trim(), phone.trim(), t, note.trim())
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloseSessionDialog(ui: TableUi, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var discount by remember { mutableStateOf("0") }
    var payment by remember { mutableStateOf("Naqd") }
    val now = currentMillis()
    val sess = ui.session
    // Stol hisobi: yopilgan partiyalar + hozir davom etayotgan partiya
    val runningPart = if (sess != null) Billing.calculateTableCost(ui.partStart, now, sess.hourlyRate) else 0.0
    val cost = ui.partsSum + runningPart
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${ui.table.name} — yakunlash") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Davomiylik: ${sess?.let { Billing.durationText(it.startTime, now) } ?: "-"}")
                if (ui.partCount > 0) {
                    Text(
                        "Partiyalar: ${ui.partCount} ta — ${formatMoney(ui.partsSum)}",
                        color = Pro.TextDim, fontSize = 12.sp
                    )
                    Text(
                        "Joriy partiya #${ui.partCount + 1}: ${Billing.durationText(ui.partStart, now)} — ${formatMoney(runningPart)}",
                        color = Pro.TextDim, fontSize = 12.sp
                    )
                }
                Text("Stol summasi: ${formatMoney(cost)}")
                if (ui.barCount > 0) Text("Bar: ${formatMoney(ui.barSum)} (${ui.barCount} pozitsiya)", color = Pro.Green)
                OutlinedTextField(discount, { discount = it.filter { c -> c.isDigit() } }, label = { Text("Chegirma (so'm)") })
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("To'lov:")
                    listOf("Naqd", "Karta", "Click").forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = payment == m, onCheckedChange = { if (it) payment = m },
                                colors = CheckboxDefaults.colors(checkedColor = Pro.Green)
                            )
                            Text(m)
                        }
                    }
                }
                val d = discount.toDoubleOrNull() ?: 0.0
                Text("Jami: ${formatMoney((cost + ui.barSum - d).coerceAtLeast(0.0))}", color = Pro.Green, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(discount.toDoubleOrNull() ?: 0.0, payment) }) { Text("Yopish + Chek") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } }
    )
}

/**
 * Sessiya partiyalari: har bir partiyaning vaqti, davomiyligi va summasi
 * hamda barcha partiyalarning umumiy summasi.
 */
@Composable
private fun PartsDialog(ui: TableUi, onDismiss: () -> Unit) {
    val now = currentMillis()
    val rate = ui.session?.hourlyRate ?: 0.0
    val runningAmount = Billing.calculateTableCost(ui.partStart, now, rate)
    val runningMinutes = Billing.durationMinutes(ui.partStart, now)
    val total = ui.partsSum + runningAmount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⏱ Partiyalar — ${ui.table.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.session == null) {
                    Text("Sessiya topilmadi", color = Pro.TextFaint)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ui.parts.size) { idx ->
                            val p = ui.parts[idx]
                            Row(
                                Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(Pro.GreenBg)
                                        .border(1.dp, Pro.GreenBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("${p.partNo}", color = Pro.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${formatHourMinute(p.startTime)} – ${formatHourMinute(p.endTime)}",
                                        color = Pro.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${p.minutes} daq · ${formatMoney(p.hourlyRate)}/soat",
                                        color = Pro.TextFaint, fontSize = 11.sp
                                    )
                                }
                                Text(formatMoney(p.amount), color = Pro.Green, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        // Hozir davom etayotgan partiya
                        item {
                            Row(
                                Modifier.fillMaxWidth().background(Pro.AmberBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, Pro.Amber, RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(Color(0xFF3A2A0A))
                                        .border(1.dp, Pro.Amber, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("${ui.partCount + 1}", color = Pro.Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${formatHourMinute(ui.partStart)} – hozir",
                                        color = Pro.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text("$runningMinutes daq · davom etmoqda", color = Pro.TextFaint, fontSize = 11.sp)
                                }
                                Text(formatMoney(runningAmount), color = Pro.Amber, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    HorizontalDivider(color = Pro.CardBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Partiyalar: ${ui.partCount + 1} ta", color = Pro.TextDim, fontSize = 12.sp)
                        Text("Jami: ${formatMoney(total)}", color = Pro.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Text(
                        "Stol hisobi shu partiyalar yig'indisidan chiqadi.",
                        color = Pro.TextFaint, fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Yopish") } },
        dismissButton = {}
    )
}

@Composable
private fun AddBarDialog(products: List<org.mexa.clube.data.entity.Product>, onDismiss: () -> Unit, onSave: (Map<Long, Double>) -> Unit) {
    val qtyMap = remember { mutableStateMapOf<Long, Double>() }
    var search by remember { mutableStateOf("") }
    var confirmStep by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") refresh
    val selected = qtyMap.filter { it.value > 0 }
    val total = selected.entries.sumOf { (pid, qty) -> (products.firstOrNull { it.id == pid }?.sellPrice ?: 0.0) * qty }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmStep) "Buyurtmani tasdiqlash" else "Stolga bar qo'shish") },
        text = {
            Column(Modifier.fillMaxWidth().height(400.dp)) {
                if (!confirmStep) {
                    OutlinedTextField(search, { search = it }, label = { Text("Qidirish...") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    val filtered = products.filter { it.name.contains(search, ignoreCase = true) }
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered.size) { idx ->
                            val p = filtered[idx]
                            val q = qtyMap[p.id] ?: 0.0
                            Row(Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = Pro.TextMain)
                                    Text("${formatMoney(p.sellPrice)} • qoldiq: ${p.stockQty} ${p.unit}", color = Pro.TextDim)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { if (q > 0) qtyMap[p.id] = q - 1; refresh++ }) { Text("−") }
                                    Text(if (q % 1.0 == 0.0) q.toLong().toString() else q.toString(), color = Pro.TextMain)
                                    TextButton(onClick = { qtyMap[p.id] = q + 1; refresh++ }) { Text("+") }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Jami: ${formatMoney(total)}", color = Pro.Green, fontWeight = FontWeight.Bold)
                } else {
                    // 2-bosqich: tanlanganlar ro'yxati + tasdiqlash
                    Text("Stolga quyidagilar yoziladi:", color = Pro.TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selected.entries.toList().size) { idx ->
                            val (pid, qty) = selected.entries.toList()[idx]
                            val p = products.firstOrNull { it.id == pid }
                            if (p != null) {
                                val qtyTxt = if (qty % 1.0 == 0.0) qty.toLong().toString() else qty.toString()
                                Row(
                                    Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "${formatMoney(p.sellPrice)} x $qtyTxt = ${formatMoney(p.sellPrice * qty)}",
                                            color = Pro.TextDim, fontSize = 12.sp
                                        )
                                    }
                                    TextButton(onClick = {
                                        qtyMap.remove(pid); refresh++
                                        if (qtyMap.none { it.value > 0 }) confirmStep = false
                                    }) { Text("🗑", color = Pro.Red) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider(color = Pro.CardBorder)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pozitsiyalar: ${selected.size} ta", color = Pro.TextDim, fontSize = 12.sp)
                        Text("JAMI: ${formatMoney(total)}", color = Pro.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!confirmStep) {
                Button(onClick = { if (selected.isNotEmpty()) confirmStep = true }) { Text("Davom etish →") }
            } else {
                Button(onClick = {
                    val m = qtyMap.filter { it.value > 0 }
                    if (m.isNotEmpty()) onSave(m)
                }) { Text("✓ Tasdiqlash") }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (confirmStep) TextButton(onClick = { confirmStep = false }) { Text("← Orqaga") }
                TextButton(onClick = onDismiss) { Text("Bekor") }
            }
        }
    )
}

@Composable
fun SessionBarDialog(
    ui: TableUi,
    items: List<org.mexa.clube.data.entity.BarSaleItem>,
    onDismiss: () -> Unit,
    onAddMore: () -> Unit,
    onDelete: (org.mexa.clube.data.entity.BarSaleItem) -> Unit
) {
    val barTotal = items.sumOf { it.sellPrice * it.qty }
    val allTotal = ui.currentSum + barTotal
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🍺 ${ui.table.name} — bar zakazlari (${items.size})") },
        text = {
            Column(Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Bu stolga hali bar mahsuloti yozilmagan.", color = Pro.TextFaint)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items.size) { idx ->
                            val it = items[idx]
                            val qtyTxt = if (it.qty % 1.0 == 0.0) it.qty.toLong().toString() else it.qty.toString()
                            Row(
                                Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(it.productName, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${formatMoney(it.sellPrice)} x $qtyTxt = ${formatMoney(it.sellPrice * it.qty)}",
                                        color = Pro.TextDim, fontSize = 12.sp
                                    )
                                }
                                TextButton(onClick = { onDelete(it) }) { Text("🗑", color = Pro.Red) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Pro.CardBorder)
                Text("Stol: ${formatMoney(ui.currentSum)}", color = Pro.TextDim, fontSize = 12.sp)
                Text("Bar: ${formatMoney(barTotal)} (${items.size} pozitsiya)", color = Pro.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Jami (hozircha): ${formatMoney(allTotal)}", color = Pro.TextMain, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddMore) { Text("+ Bar") }
                OutlinedButton(onClick = onDismiss) { Text("Yopish") }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun CheckDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🧾 Chek") },
        text = { Text(text, fontFamily = FontFamily.Monospace) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { try { CheckPrinter.print(text) } catch (_: Exception) {} }) { Text("Chop etish") }
                OutlinedButton(onClick = {
                    try { CheckPrinter.saveToFile(text, "chek_${currentMillis()}.txt") } catch (_: Exception) {}
                }) { Text("Saqlash") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Yopish") } }
    )
}
