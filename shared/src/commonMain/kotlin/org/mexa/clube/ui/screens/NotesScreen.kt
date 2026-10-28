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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import org.mexa.clube.data.entity.Note
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.formatDate
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.startOfDay
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

private val NOTE_FILTERS = listOf("Hammasi", "Kelayotgan", "O'tgan")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(vm: AppViewModel) {
    val notes by vm.notes.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Hammasi") }
    var showAdd by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Note?>(null) }
    var delete by remember { mutableStateOf<Note?>(null) }

    val todayStart = startOfDay(currentMillis())

    val filtered = remember(notes, search, filter, todayStart) {
        notes.filter { n ->
            val matchesFilter = when (filter) {
                "Kelayotgan" -> n.noteDate >= todayStart
                "O'tgan" -> n.noteDate < todayStart
                else -> true
            }
            matchesFilter && (search.isBlank() ||
                n.reason.contains(search, ignoreCase = true) ||
                n.note.contains(search, ignoreCase = true))
        }
    }

    val upcoming = notes.filter { it.noteDate >= todayStart }
    val upcomingSum = upcoming.sumOf { it.amount }
    val filteredSum = filtered.sumOf { it.amount }

    ResponsiveScreen { m ->
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📝", fontSize = 22.sp)
                Text(
                    "Zametkalar", color = Pro.TextMain, fontWeight = FontWeight.Black,
                    fontSize = if (m.compact) 18.sp else 20.sp
                )
            }
            StatChip("Jami:", "${notes.size} ta")
            StatChip("Kelayotgan:", "${upcoming.size} ta", Pro.Amber)
            StatChip("Kelayotgan summa:", formatMoney(upcomingSum), Pro.Amber)
            ProButton("Zametka qo'shish", { showAdd = true }, "+")
        }

        ProCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(if (m.compact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProSearch(search, { search = it }, placeholder = "Qidirish... (sabab, izoh)")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NOTE_FILTERS.forEach { f ->
                        CategoryPill(f, filter == f) { filter = f }
                    }
                }
            }
        }

        ProCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Hali zametka yo'q", color = Pro.TextFaint, fontSize = 13.sp)
                            ProButton("Birinchi zametkani qo'shish", { showAdd = true }, "+")
                        }
                    }
                } else if (m.useCards) {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(filtered, key = { it.id }) { n ->
                            NoteCard(n, todayStart, onEdit = { edit = n }, onDelete = { delete = n })
                            HorizontalDivider(color = Pro.CardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TableHeaderCell("SANA", Modifier.weight(1.0f))
                        TableHeaderCell("SABAB", Modifier.weight(3.0f))
                        TableHeaderCell("SUMMA", Modifier.weight(1.3f))
                        TableHeaderCell("AMAL", Modifier.weight(0.9f))
                    }
                    HorizontalDivider(color = Pro.CardBorder, thickness = 1.dp)
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(filtered, key = { it.id }) { n ->
                            NoteRow(n, todayStart, onEdit = { edit = n }, onDelete = { delete = n })
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
                    Text(
                        "Jami: ${formatMoney(filteredSum)}",
                        color = Pro.Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showAdd) {
        NoteDialog(initial = null, onDismiss = { showAdd = false }) {
            vm.saveNote(it) { showAdd = false }
        }
    }
    edit?.let { n ->
        NoteDialog(initial = n, onDismiss = { edit = null }) {
            vm.saveNote(it) { edit = null }
        }
    }
    delete?.let { n ->
        ConfirmDialog(
            title = "Zametkani o'chirish?",
            message = "«${n.reason}»${if (n.amount > 0) " (${formatMoney(n.amount)})" else ""} o'chiriladi. Davom etasizmi?",
            onDismiss = { delete = null },
            onConfirm = { vm.deleteNote(n.id) { delete = null } }
        )
    }
}

@Composable
private fun NoteRow(n: Note, todayStart: Long, onEdit: () -> Unit, onDelete: () -> Unit) {
    val upcoming = n.noteDate >= todayStart
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            formatDate(n.noteDate), Modifier.weight(1.0f),
            color = if (upcoming) Pro.Amber else Pro.TextDim, fontSize = 12.sp, maxLines = 1
        )
        Column(Modifier.weight(3.0f)) {
            Text(
                n.reason, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (n.note.isNotBlank()) Text(
                n.note, color = Pro.TextFaint, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (n.amount > 0) formatMoney(n.amount) else "—",
            Modifier.weight(1.3f),
            color = if (n.amount > 0) Pro.Amber else Pro.TextFaint,
            fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1
        )
        Row(Modifier.weight(0.9f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NoteIcon("✎", Pro.TextDim, Pro.Surface, onEdit)
            NoteIcon("🗑", Pro.Red, Pro.RedBg, onDelete)
        }
    }
}

@Composable
private fun NoteCard(n: Note, todayStart: Long, onEdit: () -> Unit, onDelete: () -> Unit) {
    val upcoming = n.noteDate >= todayStart
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    n.reason, color = Pro.TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDate(n.noteDate),
                        color = if (upcoming) Pro.Amber else Pro.TextFaint,
                        fontSize = 11.sp
                    )
                    if (upcoming) NotePill("Kelayotgan")
                }
            }
            Text(
                if (n.amount > 0) formatMoney(n.amount) else "—",
                color = if (n.amount > 0) Pro.Amber else Pro.TextFaint,
                fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (n.note.isNotBlank()) Text(
                n.note, color = Pro.TextFaint, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            ) else Spacer(Modifier.weight(1f))
            NoteIcon("✎", Pro.TextDim, Pro.Surface, onEdit)
            NoteIcon("🗑", Pro.Red, Pro.RedBg, onDelete)
        }
    }
}

@Composable
private fun NotePill(text: String) {
    Box(
        Modifier.background(Pro.AmberBg, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF6B4E12), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = Pro.Amber, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun NoteIcon(text: String, color: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(bg).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun NoteDialog(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit
) {
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var amount by remember {
        mutableStateOf(
            initial?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dateMillis by remember { mutableStateOf(initial?.noteDate ?: startOfDay(currentMillis())) }
    var dateOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (reason.isBlank()) {
            error = "Zametka sababini kiriting"
            return
        }
        val sum = if (amount.isBlank()) 0.0 else amount.toDoubleOrNull()
        if (sum == null || sum < 0.0) {
            error = "Summani to'g'ri kiriting"
            return
        }
        onSave(
            Note(
                id = initial?.id ?: 0L,
                noteDate = startOfDay(dateMillis),
                reason = reason.trim(),
                amount = sum,
                note = note.trim(),
                createdAt = initial?.createdAt ?: 0L
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "📝 Yangi zametka" else "✏️ Zametkani tahrirlash",
                color = Pro.TextMain, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    reason, { reason = it; error = null },
                    label = { Text("Sababi *") },
                    placeholder = { Text("Masalan: ijara to'lash, mahsulot olish") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                OutlinedTextField(
                    amount, { v -> amount = v.filter { it.isDigit() || it == '.' }; error = null },
                    label = { Text("Qancha pul") },
                    suffix = { Text("so'm", color = Pro.TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
