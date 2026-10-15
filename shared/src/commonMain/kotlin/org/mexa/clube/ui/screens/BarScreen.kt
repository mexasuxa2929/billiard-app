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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.data.entity.Product
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ConfirmDialog
import org.mexa.clube.ui.theme.CategoryPill
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProButton
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.theme.ProSearch
import org.mexa.clube.ui.theme.StatChip
import org.mexa.clube.ui.theme.TableHeaderCell
import org.mexa.clube.ui.viewmodel.AppViewModel

private const val LOW_STOCK = 40.0

private fun stockLevel(qty: Double): Triple<String, Color, Color> =
    when {
        qty > LOW_STOCK -> Triple("Yetarli", Pro.Green, Pro.GreenBg)
        qty > 10 -> Triple("Kam", Pro.Amber, Pro.AmberBg)
        else -> Triple("Kritik", Pro.Red, Pro.RedBg)
    }

private fun categoryIcon(cat: String): Pair<String, Color> = when (cat.lowercase()) {
    "ichimlik" -> Pair("🥤", Color(0xFF4CAF50))
    "shirinlik" -> Pair("🍫", Color(0xFFFF7043))
    "gazaklar", "gazak" -> Pair("🍿", Color(0xFFFFB020))
    else -> Pair("💧", Color(0xFF29B6F6))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarScreen(vm: AppViewModel) {
    val products by vm.products.collectAsState()
    val checkText by vm.lastCheckText.collectAsState()
    val dbCategories by vm.categories.collectAsState()
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Barchasi") }
    var sortBy by remember { mutableStateOf(0) } // 0 qoldiq, 1 nom, 2 narx
    var showAdd by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Product?>(null) }
    var stockPlus by remember { mutableStateOf<Product?>(null) }
    var directSale by remember { mutableStateOf(false) }
    var deleteProduct by remember { mutableStateOf<Product?>(null) }
    var showCheck by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }

    val dbNames = dbCategories.map { it.name }
    // DB bo'sh bo'lsa (migratsiya oldidan) mahsulotlardagidan fallback
    val categories = remember(dbNames, products) {
        val base = dbNames.ifEmpty {
            products.map { it.category.trim().ifBlank { "Umumiy" } }.distinct().sorted()
        }
        listOf("Barchasi") + base
    }
    if (category != "Barchasi" && !categories.contains(category)) category = "Barchasi"

    val totalStock = products.sumOf { it.stockQty }
    val lowCount = products.count { it.stockQty <= LOW_STOCK }
    val okCount = products.size - lowCount
    val kassa = products.sumOf { it.sellPrice * it.stockQty }

    var filtered = products.filter {
        (category == "Barchasi" || it.category == category) &&
            (search.isBlank() || it.name.contains(search, ignoreCase = true) || it.category.contains(search, ignoreCase = true))
    }
    filtered = when (sortBy) {
        1 -> filtered.sortedBy { it.name.lowercase() }
        2 -> filtered.sortedByDescending { it.sellPrice }
        else -> filtered.sortedBy { it.stockQty }
    }

    ResponsiveScreen { m ->
        // Header
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🍺", fontSize = 22.sp)
                Text(
                    "Bar & Mahsulotlar", color = Pro.TextMain, fontWeight = FontWeight.Black,
                    fontSize = if (m.compact) 18.sp else 20.sp
                )
            }
            StatChip("Jami mahsulot:", "${products.size} xil")
            StatChip("Ombor qoldig'i:", "${totalStock.toLong()} dona")
            Row(
                Modifier.background(Pro.AmberBg, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF6B4E12), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Kam qolganlar:", color = Pro.Amber, fontSize = 12.sp)
                Text("$lowCount ta", color = Pro.Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProButton("To'g'ridan savdo", { directSale = true }, "💰")
            ProButton("Mahsulot", { showAdd = true }, "+")
            ProButton("Kategoriyalar", { showCategories = true }, "🏷")
        }

        // Filter bar
        ProCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(if (m.compact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f)) { ProSearch(search, { search = it }) }
                    Box(
                        Modifier.background(Pro.Input, RoundedCornerShape(8.dp))
                            .border(1.dp, Pro.CardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val sortName = when (sortBy) { 1 -> "Nom"; 2 -> "Narx"; else -> "Qoldiq" }
                        TextButton(onClick = { sortBy = (sortBy + 1) % 3 }) {
                            Text("⇅  $sortName", color = Pro.TextDim, fontSize = 12.sp)
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(8).forEach { c ->
                        CategoryPill(c, category == c) { category = c }
                    }
                }
            }
        }

        // Table / kartalar
        val dense = m.width < 940.dp
        val showBuy = !dense
        val showProfit = m.expanded
        ProCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                if (m.useCards) {
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Mahsulot topilmadi", color = Pro.TextFaint, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                            items(filtered, key = { it.id }) { p ->
                                ProductCard(
                                    p,
                                    onStock = { stockPlus = p },
                                    onEdit = { edit = p },
                                    onDelete = { deleteProduct = p }
                                )
                                HorizontalDivider(color = Pro.CardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = if (dense) 12.dp else 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TableHeaderCell("NOMI VA KATEGORIYA", Modifier.weight(2.2f))
                        if (showBuy) TableHeaderCell("KIRISH NARXI", Modifier.weight(1f))
                        TableHeaderCell("SOTISH NARXI", Modifier.weight(1f))
                        if (showProfit) TableHeaderCell("KUTILGAN FOYDA", Modifier.weight(1.2f))
                        TableHeaderCell("QOLDIQ", Modifier.weight(0.9f))
                        TableHeaderCell("AMAL", Modifier.weight(1.1f))
                    }
                    HorizontalDivider(color = Pro.CardBorder, thickness = 1.dp)
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Mahsulot topilmadi", color = Pro.TextFaint, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                            items(filtered, key = { it.id }) { p ->
                                ProductRow(
                                    p,
                                    showBuy = showBuy,
                                    showProfit = showProfit,
                                    onStock = { stockPlus = p },
                                    onEdit = { edit = p },
                                    onDelete = { deleteProduct = p }
                                )
                                HorizontalDivider(color = Pro.CardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            }
                        }
                    }
                }
                HorizontalDivider(color = Pro.CardBorder, thickness = 1.dp)
                BarFooter(filtered.size, okCount, lowCount, kassa)
            }
        }
    }

    if (showAdd) {
        ProductDialog(null, dbNames.ifEmpty { listOf("Umumiy") }, onDismiss = { showAdd = false }) { vm.saveProduct(it); showAdd = false }
    }
    edit?.let { p ->
        ProductDialog(p, dbNames.ifEmpty { listOf(p.category.ifBlank { "Umumiy" }) }, onDismiss = { edit = null }) { vm.saveProduct(it.copy(id = p.id)); edit = null }
    }
    stockPlus?.let { p ->
        var qty by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { stockPlus = null },
            title = { Text("Ombor kirimi: ${p.name}") },
            text = { OutlinedTextField(qty, { qty = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Miqdor (${p.unit})") }) },
            confirmButton = {
                Button(onClick = { vm.addStock(p.id, qty.toDoubleOrNull() ?: 0.0); stockPlus = null }) { Text("Kirim") }
            },
            dismissButton = { TextButton(onClick = { stockPlus = null }) { Text("Bekor") } }
        )
    }
    if (directSale) {
        DirectSaleDialog(products = products, onDismiss = { directSale = false }) { map, payment ->
            vm.sellToTable(null, map, payment) { directSale = false; showCheck = true }
        }
    }
    deleteProduct?.let { p ->
        ConfirmDialog(
            title = "Mahsulotni o'chirish?",
            message = "«${p.name}» butunlay o'chiriladi. Davom etasizmi?",
            onDismiss = { deleteProduct = null },
            onConfirm = { vm.deleteProduct(p.id); deleteProduct = null }
        )
    }
    if (showCheck) {
        CheckDialog(text = checkText.ifBlank { "Hali chek yo'q" }, onDismiss = { showCheck = false })
    }
    if (showCategories) {
        CategoryDialog(
            categories = dbCategories,
            counts = remember(products) { products.groupingBy { it.category }.eachCount() },
            onDismiss = { showCategories = false },
            onAdd = { vm.addCategory(it) },
            onRename = { old, new -> vm.renameCategory(old, new) },
            onDelete = { vm.deleteCategory(it.id, it.name) }
        )
    }
}

@Composable
private fun ProductRow(
    p: Product,
    showBuy: Boolean = true,
    showProfit: Boolean = true,
    onStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val profit = p.sellPrice - p.buyPrice
    val pct = if (p.buyPrice > 0) profit / p.buyPrice * 100 else 0.0
    val pctText = "+${pct.toLong()}% (${formatMoney(profit)})"
    val (icon, tint) = categoryIcon(p.category)
    val (_, dotColor, _) = stockLevel(p.stockQty)
    val stockText = if (p.stockQty % 1.0 == 0.0) p.stockQty.toLong().toString() else p.stockQty.toString()

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nomi
        Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f))
                    .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 17.sp) }
            Column(Modifier.weight(1f)) {
                Text(
                    p.name, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.category.ifBlank { "Umumiy" }, color = Pro.Green, fontSize = 11.sp, maxLines = 1)
                    Text("•", color = Pro.TextFaint, fontSize = 11.sp)
                    Text("foyda: ${formatMoney(profit)}", color = Pro.Green, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (showBuy) Text(formatMoney(p.buyPrice), Modifier.weight(1f), color = Pro.TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatMoney(p.sellPrice), Modifier.weight(1f), color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Foyda pill
        if (showProfit) Box(Modifier.weight(1.2f)) {
            Box(
                Modifier.background(Color.Transparent, RoundedCornerShape(16.dp))
                    .border(1.dp, Pro.GreenBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(pctText, color = Pro.Green, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // Qoldiq
        Row(Modifier.weight(0.9f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stockText, color = if (dotColor == Pro.Green) Pro.TextMain else Pro.Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text(p.unit, color = Pro.TextDim, fontSize = 11.sp, maxLines = 1)
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        }
        // Amal
        Row(Modifier.weight(1.1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionIcon("+", Pro.Green, Pro.GreenBg) { onStock() }
            ActionIcon("✎", Pro.TextDim, Pro.Surface) { onEdit() }
            ActionIcon("🗑", Pro.Red, Pro.RedBg) { onDelete() }
        }
    }
}

/** Tor ekran uchun mahsulot kartasi (jadval o'rniga). */
@Composable
private fun ProductCard(p: Product, onStock: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val profit = p.sellPrice - p.buyPrice
    val (icon, tint) = categoryIcon(p.category)
    val (_, dotColor, _) = stockLevel(p.stockQty)
    val stockText = if (p.stockQty % 1.0 == 0.0) p.stockQty.toLong().toString() else p.stockQty.toString()

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f))
                    .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 15.sp) }
            Column(Modifier.weight(1f)) {
                Text(
                    p.name, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${p.category.ifBlank { "Umumiy" }} • foyda: ${formatMoney(profit)}",
                    color = Pro.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            ActionIcon("+", Pro.Green, Pro.GreenBg) { onStock() }
            ActionIcon("✎", Pro.TextDim, Pro.Surface) { onEdit() }
            ActionIcon("🗑", Pro.Red, Pro.RedBg) { onDelete() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PriceCell("Kirish", formatMoney(p.buyPrice), Pro.TextDim, Modifier.weight(1f))
            PriceCell("Sotish", formatMoney(p.sellPrice), Pro.TextMain, Modifier.weight(1f))
            PriceCell("Qoldiq", "$stockText ${p.unit}", dotColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PriceCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Pro.TextFaint, fontSize = 10.sp)
        Text(
            value, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarFooter(shown: Int, okCount: Int, lowCount: Int, kassa: Double) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Ko'rsatilmoqda: $shown ta", color = Pro.TextFaint, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Pro.Green))
            Text("Yetarli ($okCount)", color = Pro.TextDim, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Pro.Amber))
            Text("Kam ($lowCount)", color = Pro.TextDim, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Kassa:", color = Pro.TextFaint, fontSize = 12.sp)
            Text(formatMoney(kassa), color = Pro.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ActionIcon(text: String, color: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
            .background(bg).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ProductDialog(
    initial: Product?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var cat by remember {
        mutableStateOf(
            initial?.category?.trim().orEmpty().ifBlank {
                categories.firstOrNull().orEmpty().ifBlank { "Umumiy" }
            }
        )
    }
    // agar kategoriyalar kech yuklansa va cat bo'sh bo'lsa — birinchisini olamiz
    if (cat.isBlank() && categories.isNotEmpty()) cat = categories.first()
    var catExpanded by remember { mutableStateOf(false) }
    var buy by remember { mutableStateOf(initial?.buyPrice?.toLong()?.toString() ?: "") }
    var sell by remember { mutableStateOf(initial?.sellPrice?.toLong()?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stockQty?.toString() ?: "0") }
    var unit by remember { mutableStateOf(initial?.unit ?: "dona") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yangi mahsulot" else "Tahrirlash: ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nomi *") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Kategoriya dropdown
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = cat,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategoriya *") },
                            trailingIcon = { Text(if (catExpanded) "▲" else "▼", color = Pro.TextDim) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // faqat o'qish rejimidagi maydon ustiga bosiladigan shaffof qatlam
                        Box(
                            Modifier.matchParentSize()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { catExpanded = true }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { c ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(c, color = if (c == cat) Pro.Green else Pro.TextMain) },
                                    onClick = { cat = c; catExpanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(unit, { unit = it }, label = { Text("Birlik") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(buy, { buy = it.filter { c -> c.isDigit() } }, label = { Text("Kirish narxi *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(sell, { sell = it.filter { c -> c.isDigit() } }, label = { Text("Sotish narxi *") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(stock, { stock = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Boshlang'ich qoldiq") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                onSave(
                    Product(
                        name = name.trim(), category = cat.trim().ifBlank { "Umumiy" },
                        buyPrice = buy.toDoubleOrNull() ?: 0.0,
                        sellPrice = sell.toDoubleOrNull() ?: 0.0,
                        stockQty = stock.toDoubleOrNull() ?: 0.0,
                        unit = unit.trim().ifBlank { "dona" }
                    )
                )
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } }
    )
}

@Composable
private fun CategoryDialog(
    categories: List<org.mexa.clube.data.entity.Category>,
    counts: Map<String, Int>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (org.mexa.clube.data.entity.Category) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<org.mexa.clube.data.entity.Category?>(null) }
    var editText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<org.mexa.clube.data.entity.Category?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏷 Kategoriyalar (${categories.size})") },
        text = {
            Column(Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        newName, { newName = it },
                        label = { Text("Yangi kategoriya nomi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { if (newName.isNotBlank()) { onAdd(newName); newName = "" } }) { Text("Qo'shish") }
                }
                HorizontalDivider(color = Pro.CardBorder)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    items(categories, key = { it.id }) { c ->
                        val used = counts[c.name] ?: 0
                        if (editing?.id == c.id) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    editText, { editText = it }, singleLine = true,
                                    modifier = Modifier.weight(1f), label = { Text("Yangi nom") }
                                )
                                TextButton(onClick = {
                                    if (editText.isNotBlank()) onRename(c.name, editText)
                                    editing = null
                                }) { Text("✓", color = Pro.Green) }
                                TextButton(onClick = { editing = null }) { Text("✕", color = Pro.TextDim) }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("$used ta mahsulot", color = Pro.TextDim, fontSize = 11.sp)
                                }
                                TextButton(onClick = { editing = c; editText = c.name }) { Text("✎", color = Pro.TextDim) }
                                TextButton(onClick = { confirmDelete = c }) { Text("🗑", color = Pro.Red) }
                            }
                        }
                    }
                }
                Text("Band kategoriyani o'chirib bo'lmaydi — avval mahsulotlarni ko'chiring.", color = Pro.TextFaint, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Yopish") } },
        dismissButton = {}
    )
    confirmDelete?.let { c ->
        ConfirmDialog(
            title = "Kategoriyani o'chirish?",
            message = "«${c.name}» o'chiriladi. Davom etasizmi?",
            onDismiss = { confirmDelete = null },
            onConfirm = { onDelete(c); confirmDelete = null }
        )
    }
}

@Composable
private fun DirectSaleDialog(products: List<Product>, onDismiss: () -> Unit, onSave: (Map<Long, Double>, String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Naqd") }
    var confirmStep by remember { mutableStateOf(false) }
    val map = remember { mutableMapOf<Long, Double>() }
    var refresh by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") refresh
    val selected = map.filter { it.value > 0 }
    val total = selected.entries.sumOf { (pid, qty) -> (products.firstOrNull { it.id == pid }?.sellPrice ?: 0.0) * qty }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmStep) "Savdoni tasdiqlash" else "To'g'ridan-to'g'ri bar savdosi") },
        text = {
            Column {
                if (!confirmStep) {
                    OutlinedTextField(search, { search = it }, label = { Text("Qidirish...") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    val f = products.filter { it.name.contains(search, ignoreCase = true) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(280.dp)) {
                        items(f, key = { it.id }) { p ->
                            val q = map[p.id] ?: 0.0
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${p.name} (${formatMoney(p.sellPrice)})", modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { if (q > 0) map[p.id] = q - 1; refresh++ }) { Text("−") }
                                    Text(q.toLong().toString())
                                    TextButton(onClick = { map[p.id] = q + 1; refresh++ }) { Text("+") }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("To'lov:", color = Pro.TextDim)
                        listOf("Naqd", "Karta", "Click").forEach { m ->
                            val sel = payment == m
                            TextButton(onClick = { payment = m }) {
                                Text(
                                    (if (sel) "● " else "○ ") + m,
                                    color = if (sel) Pro.Green else Pro.TextDim,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text("Jami: ${formatMoney(total)}", color = Pro.Green, fontWeight = FontWeight.Bold)
                } else {
                    // 2-bosqich: umumiy ma'lumotlar ro'yxati + tasdiqlash
                    Text("Quyidagi savdoni tasdiqlaysizmi?", color = Pro.TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(260.dp)) {
                        items(selected.entries.toList()) { (pid, qty) ->
                            val p = products.firstOrNull { it.id == pid }
                            if (p != null) {
                                Row(
                                    Modifier.fillMaxWidth().background(Pro.Surface, RoundedCornerShape(8.dp)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "${formatMoney(p.sellPrice)} x ${qty.toLong()} = ${formatMoney(p.sellPrice * qty)}",
                                            color = Pro.TextDim, fontSize = 12.sp
                                        )
                                    }
                                    TextButton(onClick = {
                                        map.remove(pid); refresh++
                                        if (map.none { it.value > 0 }) confirmStep = false
                                    }) { Text("🗑", color = Pro.Red) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Pro.CardBorder)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pozitsiyalar: ${selected.size} ta", color = Pro.TextDim, fontSize = 12.sp)
                        Text("To'lov: $payment", color = Pro.TextDim, fontSize = 12.sp)
                    }
                    Text("JAMI: ${formatMoney(total)}", color = Pro.Green, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            if (!confirmStep) {
                Button(onClick = { if (selected.isNotEmpty()) confirmStep = true }) { Text("Davom etish →") }
            } else {
                Button(onClick = {
                    val m = map.filter { it.value > 0 }
                    if (m.isNotEmpty()) onSave(m, payment)
                }) { Text("✓ Tasdiqlash + Chek") }
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
