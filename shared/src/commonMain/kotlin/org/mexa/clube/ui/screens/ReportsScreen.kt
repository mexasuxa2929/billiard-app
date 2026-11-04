package org.mexa.clube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.formatTime
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ConfirmDialog
import org.mexa.clube.ui.components.DateRangeBar
import org.mexa.clube.ui.components.DateRangePreset
import org.mexa.clube.ui.components.todayRange
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.viewmodel.AppViewModel

@Composable
fun ReportsScreen(vm: AppViewModel) {
    var preset by remember { mutableStateOf(DateRangePreset.TODAY) }
    var range by remember { mutableStateOf(todayRange()) }
    val report by vm.report.collectAsState()
    val checks by vm.checks.collectAsState()
    val checkText by vm.lastCheckText.collectAsState()
    val exportResult by vm.exportResult.collectAsState()
    var showCheck by remember { mutableStateOf(false) }
    var checkToDelete by remember { mutableStateOf<CheckReceipt?>(null) }

    LaunchedEffect(range) { vm.loadCustomReport(range.from, range.toExclusive) }

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
        DateRangeBar(
            range = range,
            preset = preset,
            onChange = { r, p -> range = r; preset = p },
            compact = m.compact
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.exportRange(range.from, range.toExclusive) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pro.Green,
                    contentColor = Color(0xFF06281A)
                )
            ) { Text("📥 Excel", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { vm.loadCustomReport(range.from, range.toExclusive) }) {
                Text("↻ Yangilash", color = Pro.TextDim)
            }
        }
        val r = report
        if (r != null) {
            if (m.compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro("🎱 Stollar (vaqt)", formatMoney(r.tableRevenue), Pro.Green, Modifier.weight(1f))
                        StatPro(
                            "🍺 Bar savdo (jami)", formatMoney(r.barRevenue), Color(0xFF29B6F6), Modifier.weight(1f),
                            subtitle = "stolga ${formatMoney(r.barTableRevenue)}"
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro(
                            "💹 Bar foyda (jami)", formatMoney(r.barProfit), Pro.Amber, Modifier.weight(1f),
                            subtitle = "stolga ${formatMoney(r.barTableProfit)}"
                        )
                        StatPro("💰 JAMI daromad", formatMoney(r.totalRevenue), Pro.Green, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro("💸 Xarajatlar", formatMoney(r.expenses), Pro.Red, Modifier.weight(1f))
                        StatPro(
                            "📝 Zametkalar", formatMoney(r.notes), Pro.Red, Modifier.weight(1f),
                            subtitle = "foydadan ayriladi"
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPro("📈 Sof foyda", formatMoney(r.netProfit), Pro.Green, Modifier.weight(1f))
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.cardSpacing)) {
                    StatPro("🎱 Stollar (vaqt)", formatMoney(r.tableRevenue), Pro.Green, Modifier.weight(1f))
                    StatPro(
                        "🍺 Bar savdo (jami)", formatMoney(r.barRevenue), Color(0xFF29B6F6), Modifier.weight(1f),
                        subtitle = "stolga ${formatMoney(r.barTableRevenue)} • alohida ${formatMoney(r.barDirectRevenue)}"
                    )
                    StatPro(
                        "💹 Bar foyda (jami)", formatMoney(r.barProfit), Pro.Amber, Modifier.weight(1f),
                        subtitle = "stolga ${formatMoney(r.barTableProfit)} • alohida ${formatMoney(r.barDirectProfit)}"
                    )
                    StatPro("💰 JAMI daromad", formatMoney(r.totalRevenue), Pro.Green, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(m.cardSpacing)) {
                    StatPro("💸 Xarajatlar", formatMoney(r.expenses), Pro.Red, Modifier.weight(1f))
                    StatPro(
                        "📝 Zametkalar", formatMoney(r.notes), Pro.Red, Modifier.weight(1f),
                        subtitle = "foydadan ayriladi"
                    )
                    StatPro("📈 Sof foyda", formatMoney(r.netProfit), Pro.Green, Modifier.weight(1f))
                }
            }
            Text(
                "Sessiyalar: ${r.sessionsCount} • Chegirmalar: ${formatMoney(r.discount)} • Bar tannarxi: ${formatMoney(r.barCost)}",
                color = Pro.TextDim, fontSize = 12.sp
            )
            Text(
                "Stollar — faqat vaqt puli. Bar savdo/foyda stolga biriktirilgan savdoni ham o'z ichiga oladi " +
                    "(stol yopilganda hisobga olinadi, bekor qilingan sessiyalar kirmaydi). " +
                    "JAMI daromad = Stollar + Bar savdo − chegirma; " +
                    "Sof foyda = JAMI daromad − xarajatlar − zametkalar. " +
                    "Bar summasi ikki marta hisoblanmaydi.",
                color = Pro.TextFaint, fontSize = 11.sp
            )
            if (m.compact) {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TopProductsCard(r, Modifier.fillMaxWidth().weight(1f))
                    ChecksCard(
                        checks,
                        onReprint = { vm.reprintCheck(it) { showCheck = true } },
                        onDelete = { checkToDelete = it },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(m.cardSpacing)) {
                    TopProductsCard(r, Modifier.weight(1f))
                    ChecksCard(
                        checks,
                        onReprint = { vm.reprintCheck(it) { showCheck = true } },
                        onDelete = { checkToDelete = it },
                        modifier = Modifier.weight(2f)
                    )
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
    checkToDelete?.let { c ->
        ConfirmDialog(
            title = "Chekni o'chirish?",
            message = "#${c.id} ${c.tableName} • ${formatMoney(c.totalAmount)}\n\n" +
                "Faqat chek yozuvi o'chiriladi — sessiya/savdo summasi hisobotda qoladi.",
            onDismiss = { checkToDelete = null },
            onConfirm = { vm.deleteCheck(c) { checkToDelete = null } }
        )
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
    onDelete: (CheckReceipt) -> Unit,
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
                            TextButton(onClick = { onDelete(c) }) {
                                Text("🗑", color = Pro.Red, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPro(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ProCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Pro.TextDim, fontSize = 12.sp)
            Text(value, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle != null) Text(
                subtitle, color = Pro.TextFaint, fontSize = 10.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.Box(
                Modifier.height(3.dp).fillMaxWidth()
                    .background(accent.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            )
        }
    }
}
