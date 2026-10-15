package org.mexa.clube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.domain.ReportSummary
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.formatTime
import org.mexa.clube.domain.parseUzbekDate
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ProDatePickerDialog
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.viewmodel.AppViewModel

private const val DAY_MS = 24L * 3600 * 1000

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(vm: AppViewModel) {
    var period by remember { mutableStateOf(0) } // 0 kunlik, 1 haftalik, 2 oylik, 3 jami, 4 oraliq
    var fromStr by remember { mutableStateOf(formatDate(currentMillis())) }
    var toStr by remember { mutableStateOf(formatDate(currentMillis())) }
    var rangeError by remember { mutableStateOf<String?>(null) }
    val report by vm.report.collectAsState()
    val checks by vm.checks.collectAsState()
    val checkText by vm.lastCheckText.collectAsState()
    val exportResult by vm.exportResult.collectAsState()
    var showCheck by remember { mutableStateOf(false) }

    LaunchedEffect(period) { if (period != 4) vm.loadReport(period) }

    fun applyCustomRange() {
        val from = parseUzbekDate(fromStr)
        val toDay = parseUzbekDate(toStr)
        if (from == null || toDay == null) {
            rangeError = "Sana formati noto'g'ri"
            return
        }
        if (from > toDay) {
            rangeError = "Boshlanish sanasi tugashdan keyin bo'lmasligi kerak"
            return
        }
        rangeError = null
        vm.loadCustomReport(from, toDay + DAY_MS)
    }

    /** Oraliq panelidagi Dan/Gacha dan (from, toExclusive) oladi yoki null + xato. */
    fun resolveRange(): Pair<Long, Long>? {
        val from = parseUzbekDate(fromStr)
        val toDay = parseUzbekDate(toStr)
        if (from == null || toDay == null) {
            rangeError = "Sana formati noto'g'ri"
            return null
        }
        if (from > toDay) {
            rangeError = "Boshlanish sanasi tugashdan keyin bo'lmasligi kerak"
            return null
        }
        rangeError = null
        return from to (toDay + DAY_MS)
    }

    ResponsiveScreen { m ->
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📊", fontSize = 22.sp)
            Text(
                "Hisobotlar", color = Pro.TextMain, fontWeight = FontWeight.Black,
                fontSize = if (m.compact) 18.sp else 20.sp
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Kunlik", "Haftalik", "Oylik", "Jami", "Oraliq").forEachIndexed { i, t ->
                if (i == period) Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A))
                ) { Text(t, fontWeight = FontWeight.Bold) }
                else OutlinedButton(onClick = { period = i }) { Text(t, color = Pro.TextDim) }
            }
            OutlinedButton(onClick = {
                if (period == 4) applyCustomRange() else vm.loadReport(period)
            }) { Text("↻", color = Pro.TextDim) }
        }
        // Oraliq tanlash paneli (DatePicker bilan)
        if (period == 4) {
            ProCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(if (m.compact) 10.dp else 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        DateField(
                            label = "Dan",
                            value = fromStr,
                            modifier = Modifier.weight(1f),
                            onPick = {
                                fromStr = formatDate(it)
                                applyCustomRange()
                            }
                        )
                        DateField(
                            label = "Gacha",
                            value = toStr,
                            modifier = Modifier.weight(1f),
                            onPick = {
                                toStr = formatDate(it)
                                applyCustomRange()
                            }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(onClick = { applyCustomRange() }) { Text("Ko'rsatish") }
                        Button(
                            onClick = {
                                val range = resolveRange()
                                if (range != null) vm.exportRange(range.first, range.second)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Pro.Green,
                                contentColor = Color(0xFF06281A)
                            )
                        ) { Text("📥 Excel", fontWeight = FontWeight.Bold) }
                    }
                    if (rangeError != null) Text(rangeError!!, color = Pro.Red, fontSize = 12.sp)
                    else Text("Kalenlardan sana tanlang — hisobot avtomatik yangilanadi", color = Pro.TextFaint, fontSize = 11.sp)
                }
            }
        }
        val r = report
        if (r != null) {
            if (m.compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro("🎱 Stollar", formatMoney(r.tableRevenue), Pro.Green, Modifier.weight(1f))
                        StatPro("🍺 Bar savdo", formatMoney(r.barRevenue), Color(0xFF29B6F6), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro("💹 Bar foyda", formatMoney(r.barProfit), Pro.Amber, Modifier.weight(1f))
                        StatPro("💰 JAMI", formatMoney(r.totalRevenue), Pro.Green, Modifier.weight(1f))
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.cardSpacing)) {
                    StatPro("🎱 Stollar", formatMoney(r.tableRevenue), Pro.Green, Modifier.weight(1f))
                    StatPro("🍺 Bar savdo", formatMoney(r.barRevenue), Color(0xFF29B6F6), Modifier.weight(1f))
                    StatPro("💹 Bar foyda", formatMoney(r.barProfit), Pro.Amber, Modifier.weight(1f))
                    StatPro("💰 JAMI", formatMoney(r.totalRevenue), Pro.Green, Modifier.weight(1f))
                }
            }
            Text(
                "Sessiyalar: ${r.sessionsCount} • Chegirmalar: ${formatMoney(r.discount)} • Bar tannarxi: ${formatMoney(r.barCost)}",
                color = Pro.TextDim, fontSize = 12.sp
            )
            if (m.compact) {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TopProductsCard(r, Modifier.fillMaxWidth().weight(1f))
                    ChecksCard(checks, onReprint = { vm.reprintCheck(it) { showCheck = true } }, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(m.cardSpacing)) {
                    TopProductsCard(r, Modifier.weight(1f))
                    ChecksCard(checks, onReprint = { vm.reprintCheck(it) { showCheck = true } }, modifier = Modifier.weight(2f))
                }
            }
        } else {
            Text("Yuklanmoqda...", color = Pro.TextFaint)
        }
    }

    exportResult?.let { path ->
        AlertDialog(
            onDismissRequest = { vm.clearExportResult() },
            title = { Text("📥 Eksport tayyor") },
            text = { Text("Fayl saqlandi:\n$path", color = Pro.TextDim) },
            confirmButton = { TextButton(onClick = { vm.clearExportResult() }) { Text("Yopish") } },
            dismissButton = {}
        )
    }

    if (showCheck) {
        CheckDialog(text = checkText.ifBlank { "Yuklanmoqda..." }, onDismiss = { showCheck = false })
    }
}

@Composable
private fun TopProductsCard(r: ReportSummary, modifier: Modifier = Modifier) {
    ProCard(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("TOP mahsulotlar", color = Pro.TextMain, fontWeight = FontWeight.Bold)
            if (r.topProducts.isEmpty()) Text("Hali savdo yo'q", color = Pro.TextFaint)
            r.topProducts.forEachIndexed { i, t ->
                Text(
                    "${i + 1}. ${t.name} — ${t.qty} • ${formatMoney(t.revenue)}",
                    color = Pro.TextDim, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChecksCard(
    checks: List<CheckReceipt>,
    onReprint: (CheckReceipt) -> Unit,
    modifier: Modifier = Modifier
) {
    ProCard(modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("Cheklar (${checks.size})", color = Pro.TextMain, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (checks.isEmpty()) {
                Text("Hali chek yo'q", color = Pro.TextFaint, fontSize = 12.sp)
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(checks, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Pro.Surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#${c.id} ${c.tableName} • ${formatTime(c.createdAt)}",
                                color = Pro.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatMoney(c.totalAmount), color = Pro.Green,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1
                            )
                            TextButton(onClick = { onReprint(c) }) {
                                Text("🖨", color = Pro.TextDim, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateField(label: String, value: String, modifier: Modifier = Modifier, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("📅", color = Pro.TextDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(4.dp))
                .clickable { open = true }
        )
    }
    if (open) {
        ProDatePickerDialog(
            initialMillis = parseUzbekDate(value),
            onDismiss = { open = false },
            onPick = {
                open = false
                onPick(it)
            }
        )
    }
}

@Composable
private fun StatPro(title: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    ProCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Pro.TextDim, fontSize = 12.sp)
            Text(value, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            androidx.compose.foundation.layout.Box(
                Modifier.height(3.dp).fillMaxWidth()
                    .background(accent.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            )
        }
    }
}
