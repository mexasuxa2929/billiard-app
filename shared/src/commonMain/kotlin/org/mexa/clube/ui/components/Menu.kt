package org.mexa.clube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mexa.clube.ui.theme.Pro

data class MenuEntry(val icon: String, val title: String)

val menuItems = listOf(
    MenuEntry("🎱", "Stollar"),
    MenuEntry("🍺", "Bar"),
    MenuEntry("📊", "Hisobotlar"),
    MenuEntry("🧾", "Tarix / Jurnal"),
    MenuEntry("⚙️", "Sozlamalar")
)

/**
 * Yon menyu. [compact] = true bo'lsa faqat ikonkalar (rail) ko'rinishida,
 * tor oynada ham ekranni egallab qolmaydi.
 */
@Composable
fun SideMenu(selected: Int, compact: Boolean = false, onSelect: (Int) -> Unit) {
    Column(
        Modifier.fillMaxHeight()
            .width(if (compact) 64.dp else 232.dp)
            .background(Pro.Sidebar)
            .padding(if (compact) 8.dp else 14.dp),
        horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (compact) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Pro.Green),
                contentAlignment = Alignment.Center
            ) { Text("8", color = Color(0xFF06281A), fontWeight = FontWeight.Black, fontSize = 20.sp) }
            Spacer(Modifier.height(14.dp))
        } else {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Pro.Green),
                    contentAlignment = Alignment.Center
                ) { Text("8", color = Color(0xFF06281A), fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Column {
                    Text("BILLIARD", color = Pro.Green, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    Text("club manager", color = Pro.TextDim, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        menuItems.forEachIndexed { i, m ->
            val sel = i == selected
            if (compact) {
                Box(
                    Modifier.size(44.dp)
                        .background(
                            if (sel) Color(0xFF10281C) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .then(if (sel) Modifier.border(1.dp, Pro.GreenBorder, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) { Text(m.icon, fontSize = 18.sp) }
                Spacer(Modifier.height(6.dp))
            } else {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            if (sel) Color(0xFF10281C) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .then(if (sel) Modifier.border(1.dp, Pro.GreenBorder, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (sel) Box(Modifier.width(3.dp).height(18.dp).background(Pro.Green, RoundedCornerShape(2.dp)))
                        Text(m.icon, fontSize = 15.sp)
                        Text(
                            m.title,
                            color = if (sel) Pro.Green else Pro.TextDim,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                    if (sel) Box(
                        Modifier.background(Pro.GreenBg, RoundedCornerShape(6.dp))
                            .border(1.dp, Pro.GreenBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text("Aktiv", color = Pro.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
