package org.mexa.clube.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.domain.formatMoney
import org.mexa.clube.domain.formatTime
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProButton
import org.mexa.clube.ui.theme.ProCard
import org.mexa.clube.ui.viewmodel.AppViewModel

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val logs by vm.logs.collectAsState()
    LaunchedEffect(Unit) { vm.loadLogs() }
    Column(Modifier.fillMaxSize().background(Pro.Bg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🧾", fontSize = 22.sp)
                Text("Tarix / Jurnal (${logs.size})", color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            ProButton("Yangilash", { vm.loadLogs() }, "↻")
        }
        ProCard(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(logs, key = { it.id }) { l ->
                    Row(
                        Modifier.fillMaxWidth().background(Pro.Surface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("[${l.type}] ${l.description}", color = Pro.TextMain, fontSize = 12.sp)
                            Text(formatTime(l.timestamp), color = Pro.TextFaint, fontSize = 11.sp)
                        }
                        if (l.amount != 0.0) Text(formatMoney(l.amount), color = Pro.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val club by vm.clubName.collectAsState()
    val price by vm.defaultPrice.collectAsState()
    val exportDir by vm.exportDir.collectAsState()
    var name by remember(club) { mutableStateOf(club) }
    var pr by remember(price) { mutableStateOf(price.toLong().toString()) }
    var dir by remember(exportDir) { mutableStateOf(exportDir) }
    var saved by remember { mutableStateOf(false) }
    var dirSaved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Pro.Bg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️", fontSize = 22.sp)
            Text("Sozlamalar", color = Pro.TextMain, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        ProCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it; saved = false }, label = { Text("Klub nomi (chekda chiqadi)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pr, { pr = it.filter { c -> c.isDigit() }; saved = false }, label = { Text("Standart soatlik narx (so'm)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        vm.saveSettings(name.ifBlank { "BILLIARD CLUB" }, pr.toDoubleOrNull() ?: 60000.0)
                        saved = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A))
                ) { Text("Saqlash", fontWeight = FontWeight.Bold) }
                if (saved) Text("✓ Saqlandi", color = Pro.Green)
            }
        }
        ProCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📥 Excel eksport papkasi", color = Pro.TextMain, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    dir, { dir = it; dirSaved = false },
                    label = { Text("Papka yo'li") },
                    placeholder = { Text("Masalan: /home/foydalanuvchi/Hisobotlar") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        org.mexa.clube.ui.files.FolderPicker.pickFolder(dir)?.let {
                            dir = it; dirSaved = false
                        }
                    }) { Text("📁 Tanlash", color = Pro.TextDim) }
                    Button(
                        onClick = { vm.saveExportDir(dir); dirSaved = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A))
                    ) { Text("Saqlash", fontWeight = FontWeight.Bold) }
                    if (dirSaved) Text("✓ Saqlandi", color = Pro.Green)
                }
                Text("Bo'sh qoldirilsa: ~/.billiardapp/eksport ishlatiladi", color = Pro.TextFaint, fontSize = 11.sp)
            }
        }
        ProCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dastur haqida", color = Pro.TextMain, fontWeight = FontWeight.Bold)
                Text("Billiard Club Manager v2.4 Pro — KMP Desktop + Room", color = Pro.TextDim, fontSize = 12.sp)
                Text("Ma'lumotlar: ~/.billiardapp/billiard_club.db", color = Pro.TextFaint, fontSize = 12.sp)
                Text("Cheklar: ~/.billiardapp/cheklar/", color = Pro.TextFaint, fontSize = 12.sp)
                Text("Hisoblash: daqiqabay (ceil), min 1 daqiqa", color = Pro.TextFaint, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
