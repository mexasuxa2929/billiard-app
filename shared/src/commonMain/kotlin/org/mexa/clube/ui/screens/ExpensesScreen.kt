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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import org.mexa.clube.data.entity.EXPENSE_CATEGORIES
import org.mexa.clube.data.entity.Expense
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.startOfDay
import org.mexa.clube.domain.startOfMonth
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ConfirmDialog
import org.mexa.clube.ui.components.ProDatePickerDialog
import org.mexa.clube.ui.theme.CategoryPill
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProButton
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.theme.ProSearch
import org.mexa.clube.ui.theme.StatChip
import org.mexa.clube.ui.theme.TableHeaderCell
import org.mexa.clube.ui.viewmodel.AppViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(vm: AppViewModel) {
    val expenses by vm.expenses.collectAsState()
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Barchasi") }
    var showAdd by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Expense?>(null) }
    var delete by remember { mutableStateOf<Expense?>(null) }

    val categories = remember { listOf("Barchasi") + EXPENSE_CATEGORIES }
    if (!categories.contains(category)) category = "Barchasi"

    val filtered = remember(expenses, search, category) {
        expenses.filter { e ->
            (category == "Barchasi" || e.category == category) &&
                (search.isBlank() ||
                    e.reason.contains(search, ignoreCase = true) ||
                    e.category.contains(search, ignoreCase = true) ||
                    e.note.contains(search, ignoreCase = true))
        }
    }

    val now = currentMillis()
    val todayStart = startOfDay(now)
    val monthStart = startOfMonth(now)
    val todayTotal = expenses.filter { it.spentAt >= todayStart }.sumOf { it.amount }
    val monthTotal = expenses.filter { it.spentAt >= monthStart }.sumOf { it.amount }
    val filteredTotal = filtered.sumOf { it.amount }

    ResponsiveScreen { m ->
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("💸", fontSize = 22.sp)
                Text(
                    "Xarajatlar", color = Pro.TextMain, fontWeight = FontWeight.Black,
                    fontSize = if (m.compact) 18.sp else 20.sp
                )
            }
            StatChip("Jami:", "${expenses.size} ta")
            StatChip("Bugun:", formatMoney(todayTotal), Pro.Red)
            StatChip("Bu oy:", formatMoney(monthTotal), Pro.Red)
            ProButton("Xarajat qo'shish", { showAdd = true }, "+")
        }

        ProCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(if (m.compact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProSearch(search, { search = it }, placeholder = "Qidirish... (sabab, kategoriya, izoh)")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { c ->
                        CategoryPill(c, category == c) { category = c }
                    }
                }
            }
        }

        ProCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Hali xarajat kiritilmagan", color = Pro.TextFaint, fontSize = 13.sp)
                            ProButton("Birinchi xarajatni qo'shish", { showAdd = true }, "+")
                        }
                    }
                } else if (m.useCards) {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(filtered, key = { it.id }) { e ->
                            ExpenseCard(
                                e,
                                onEdit = { edit = e },
                                onDelete = { delete = e }
                            )
                            HorizontalDivider(color = Pro.CardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TableHeaderCell("SANA", Modifier.weight(1.0f))
                        TableHeaderCell("SABAB", Modifier.weight(2.6f))
                        TableHeaderCell("KATEGORIYA", Modifier.weight(1.2f))
                        TableHeaderCell("TO'LOV", Modifier.weight(1.0f))
                        TableHeaderCell("SUMMA", Modifier.weight(1.3f))
                        TableHeaderCell("AMAL", Modifier.weight(1.0f))
                    }
                    HorizontalDivider(color = Pro.CardBorder, thickness = 1.dp)
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(filtered, key = { it.id }) { e ->
                            ExpenseRow(
                                e,
                                onEdit = { edit = e },
                                onDelete = { delete = e }
                            )
                            HorizontalDivider(color = Pro.CardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                }
                HorizontalDivider(color = Pro.CardBorder, thickness = 1.dp)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ko'rsatilmoqda: ${filtered.size} ta", color = Pro.TextFaint, fontSize = 12.sp)
                    Text("Jami: ${formatMoney(filteredTotal)}", color = Pro.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showAdd) {
        ExpenseDialog(initial = null, onDismiss = { showAdd = false }) {
            vm.saveExpense(it) { showAdd = false }
        }
    }
    edit?.let { e ->
        ExpenseDialog(initial = e, onDismiss = { edit = null }) {
            vm.saveExpense(it) { edit = null }
        }
    }
    delete?.let { e ->
        ConfirmDialog(
            title = "Xarajatni o'chirish?",
            message = "«${e.reason}» (${formatMoney(e.amount)}) o'chiriladi. Davom etasizmi?",
            onDismiss = { delete = null },
            onConfirm = { vm.deleteExpense(e.id) { delete = null } }
        )
    }
}

@Composable
private fun ExpenseRow(e: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(formatDate(e.spentAt), Modifier.weight(1.0f), color = Pro.TextDim, fontSize = 12.sp, maxLines = 1)
        Column(Modifier.weight(2.6f)) {
            Text(
                e.reason, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (e.note.isNotBlank()) Text(
                e.note, color = Pro.TextFaint, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.weight(1.2f)) { ExpenseCategoryPill(e.category) }
        Text(e.paymentMethod, Modifier.weight(1.0f), color = Pro.TextDim, fontSize = 12.sp, maxLines = 1)
        Text(formatMoney(e.amount), Modifier.weight(1.3f), color = Pro.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        Row(Modifier.weight(1.0f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ActionIcon("✎", Pro.TextDim, Pro.Surface, onEdit)
            ActionIcon("🗑", Pro.Red, Pro.RedBg, onDelete)
        }
    }
}

@Composable
private fun ExpenseCard(e: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    e.reason, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(formatDate(e.spentAt), color = Pro.TextFaint, fontSize = 11.sp)
            }
            Text(formatMoney(e.amount), color = Pro.Red, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpenseCategoryPill(e.category)
            Text(e.paymentMethod, color = Pro.TextDim, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            ActionIcon("✎", Pro.TextDim, Pro.Surface, onEdit)
            ActionIcon("🗑", Pro.Red, Pro.RedBg, onDelete)
        }
        if (e.note.isNotBlank()) Text(
            e.note, color = Pro.TextFaint, fontSize = 11.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExpenseCategoryPill(category: String) {
    Box(
        Modifier.background(Pro.AmberBg, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF6B4E12), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(category, color = Pro.Amber, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActionIcon(text: String, color: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(bg).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseDialog(
    initial: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Boshqa") }
    var amount by remember {
        mutableStateOf(
            initial?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var payment by remember { mutableStateOf(initial?.paymentMethod ?: "Naqd") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dateMillis by remember { mutableStateOf(initial?.spentAt ?: currentMillis()) }
    var dateOpen by remember { mutableStateOf(false) }
    var catOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        val sum = amount.toDoubleOrNull() ?: 0.0
        if (reason.isBlank()) {
            error = "Xarajat sababini kiriting"
            return
        }
        if (sum <= 0.0) {
            error = "Summani 0 dan katta kiriting"
            return
        }
        onSave(
            Expense(
                id = initial?.id ?: 0L,
                // Kun bo'yicha saqlaymiz — hisobotda shu kunga tushadi.
                spentAt = startOfDay(dateMillis) + 12L * 3600 * 1000,
                reason = reason.trim(),
                category = category,
                amount = sum,
                paymentMethod = payment,
                note = note.trim()
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "💸 Yangi xarajat" else "✏️ Xarajatni tahrirlash", color = Pro.TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    reason, { reason = it; error = null },
                    label = { Text("Sababi *") },
                    placeholder = { Text("Masalan: elektr energiyasi, oylik, ta'mirlash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategoriya") },
                            trailingIcon = { Text(if (catOpen) "▲" else "▼", color = Pro.TextDim) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                                .clickable { catOpen = true }
                        )
                        DropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                            EXPENSE_CATEGORIES.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c, color = if (c == category) Pro.Green else Pro.TextMain) },
                                    onClick = { category = c; catOpen = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        amount, { v -> amount = v.filter { it.isDigit() || it == '.' }; error = null },
                        label = { Text("Summa *") },
                        suffix = { Text("so'm", color = Pro.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Box {
                    OutlinedTextField(
                        value = formatDate(dateMillis),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sana") },
                        trailingIcon = { Text("📅", color = Pro.TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                            .clickable { dateOpen = true }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("To'lov:", color = Pro.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    listOf("Naqd", "Karta", "Click").forEach { pm ->
                        val sel = payment == pm
                        TextButton(onClick = { payment = pm }) {
                            Text(
                                (if (sel) "● " else "○ ") + pm,
                                color = if (sel) Pro.Green else Pro.TextDim,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    note, { note = it },
                    label = { Text("Izoh (ixtiyoriy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) Text(error!!, color = Pro.Red, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { save() },
                colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A))
            ) { Text("Saqlash", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish", color = Pro.TextDim) } }
    )

    if (dateOpen) {
        ProDatePickerDialog(
            initialMillis = dateMillis,
            onDismiss = { dateOpen = false },
            onPick = { dateMillis = it; dateOpen = false }
        )
    }
}
