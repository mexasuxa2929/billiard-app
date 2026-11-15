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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.data.entity.Note
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.ui.ResponsiveScreen
import org.mexa.clube.ui.components.ConfirmDialog
import org.mexa.clube.ui.components.DateRangeBar
import org.mexa.clube.ui.components.DateRangePreset
import org.mexa.clube.ui.components.allRange
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.theme.StatChip
import org.mexa.clube.ui.viewmodel.AppViewModel

/**
 * «Topshirilgan atchot» bo'limi.
 *
 * Zametkalar bo'limida «Atchotga topshirish» qilingan yozuvlar shu yerga
 * yig'iladi: sana oralig'i bo'yicha ro'yxat va umumiy topshirilgan summa.
 * Bu summa hisobotlarda «Sof foyda» sifatida ko'rsatiladi.
 * Xato yozuvni o'chirish (🗑) yoki zametkalarga qaytarish (↩) mumkin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmittedReportScreen(vm: AppViewModel) {
    val notes by vm.notes.collectAsState()
    var preset by remember { mutableStateOf(DateRangePreset.ALL) }
    var range by remember { mutableStateOf(allRange()) }
    var toDelete by remember { mutableStateOf<Note?>(null) }

    val submitted = remember(notes, range) {
        notes.filter { it.handedOver && range.contains(it.noteDate) }
    }
    val total = submitted.sumOf { it.amount }
    val allTotal = notes.filter { it.handedOver }.sumOf { it.amount }

    ResponsiveScreen { m ->
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📤", fontSize = 22.sp)
                Text(
                    "Topshirilgan atchot", color = Pro.TextMain, fontWeight = FontWeight.Black,
                    fontSize = if (m.compact) 18.sp else 20.sp
                )
            }
            StatChip("Yozuvlar:", "${submitted.size} ta")
            StatChip("Davr summasi:", formatMoney(total), Pro.Green)
            StatChip("Jami topshirilgan:", formatMoney(allTotal), Pro.Green)
        }

        DateRangeBar(
            range = range,
            preset = preset,
            onChange = { r, p -> range = r; preset = p },
            compact = m.compact
        )

        Text(
            "Bu yerda atchotga topshirilgan summalar yig'iladi va hisobotlarda «Sof foyda» sifatida " +
                "ko'rsatiladi. Xato yozuvni 🗑 bilan o'chirish yoki ↩ bilan zametkalarga qaytarish mumkin.",
            color = Pro.TextFaint, fontSize = 11.sp
        )

        ProCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                if (submitted.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Hali topshirilgan atchot yo'q", color = Pro.TextFaint, fontSize = 13.sp)
                            Text(
                                "Zametkalar bo'limida yozuvni tahrirlab, «📤 Atchotga topshirish» ni tanlang.",
                                color = Pro.TextFaint, fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(submitted, key = { it.id }) { n ->
                            SubmittedRow(
                                n,
                                onReturn = { vm.saveNote(n.copy(handedOver = false)) },
                                onDelete = { toDelete = n }
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
                    Text("Ko'rsatilmoqda: ${submitted.size} ta", color = Pro.TextFaint, fontSize = 12.sp)
                    Text(
                        "Jami: ${formatMoney(total)}",
                        color = Pro.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }
    }

    toDelete?.let { n ->
        ConfirmDialog(
            title = "Topshirilgan atchotni o'chirish?",
            message = "«${n.reason}»${if (n.amount > 0) " (${formatMoney(n.amount)})" else ""} butunlay " +
                "o'chiriladi va hisobotdagi «Sof foyda» dan ham chiqadi. Davom etasizmi?",
            onDismiss = { toDelete = null },
            onConfirm = { vm.deleteNote(n.id) { toDelete = null } }
        )
    }
}

@Composable
private fun SubmittedRow(n: Note, onReturn: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                n.reason, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(n.noteDate), color = Pro.TextFaint, fontSize = 11.sp)
                Box(
                    Modifier.background(Pro.GreenBg, RoundedCornerShape(8.dp))
                        .border(1.dp, Pro.GreenBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) { Text("✅ Topshirildi", color = Pro.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            if (n.note.isNotBlank()) Text(
                n.note, color = Pro.TextFaint, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (n.amount > 0) formatMoney(n.amount) else "—",
            color = if (n.amount > 0) Pro.Green else Pro.TextFaint,
            fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1
        )
        Box(
            Modifier.background(Pro.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, Pro.CardBorder, RoundedCornerShape(8.dp))
                .clickable { onReturn() }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("↩", color = Pro.TextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Box(
            Modifier.background(Pro.RedBg, RoundedCornerShape(8.dp))
                .border(1.dp, Pro.Red.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { onDelete() }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("🗑", color = Pro.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}
