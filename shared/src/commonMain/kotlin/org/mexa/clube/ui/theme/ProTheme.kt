package org.mexa.clube.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Pro palette (skrinshotdagidek) ----------
object Pro {
    val Bg = Color(0xFF0A0F1A)
    val Sidebar = Color(0xFF0D1322)
    val Surface = Color(0xFF131A2A)
    val Card = Color(0xFF141B2D)
    val CardBorder = Color(0xFF1F2A44)
    val Input = Color(0xFF0F1626)
    val Green = Color(0xFF00E676)
    val GreenDark = Color(0xFF00C853)
    val GreenBg = Color(0xFF0B2E1F)
    val GreenBorder = Color(0xFF1B5E3A)
    val Amber = Color(0xFFFFB020)
    val AmberBg = Color(0xFF2E220B)
    val Red = Color(0xFFFF5252)
    val RedBg = Color(0xFF2E1116)
    val TextMain = Color(0xFFF1F5F9)
    val TextDim = Color(0xFF8B98AD)
    val TextFaint = Color(0xFF5B6B85)
}

@Composable
fun ProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pro.Green,
            secondary = Pro.GreenDark,
            background = Pro.Bg,
            surface = Pro.Surface,
            surfaceVariant = Pro.Card,
            onBackground = Pro.TextMain,
            onSurface = Pro.TextMain
        ),
        content = content
    )
}

@Composable
fun ProButton(text: String, onClick: () -> Unit, icon: String = "") {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Pro.Green, contentColor = Color(0xFF06281A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            (if (icon.isNotBlank()) "$icon  " else "") + text,
            fontWeight = FontWeight.Bold, fontSize = 13.sp
        )
    }
}

@Composable
fun StatChip(label: String, value: String, accent: Color = Pro.Green) {
    Row(
        Modifier.background(Pro.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = Pro.TextDim, fontSize = 12.sp)
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ProCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Pro.Card),
        border = BorderStroke(1.dp, Pro.CardBorder),
        shape = RoundedCornerShape(14.dp),
        content = { content() }
    )
}

@Composable
fun ProSearch(value: String, onChange: (String) -> Unit, placeholder: String = "Qidirish... (nomi, kategoriya)") {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text("⌕   $placeholder", color = Pro.TextFaint, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Pro.Input, unfocusedContainerColor = Pro.Input,
            focusedBorderColor = Pro.CardBorder, unfocusedBorderColor = Pro.CardBorder,
            focusedTextColor = Pro.TextMain, unfocusedTextColor = Pro.TextMain
        )
    )
}

@Composable
fun CategoryPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.background(
            if (selected) Pro.Green else Color.Transparent,
            RoundedCornerShape(8.dp)
        ).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF06281A) else Pro.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = Pro.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}

@Composable
fun RowScope.TableCellText(text: String, weight: Float, bold: Boolean = false, color: Color = Pro.TextMain) {
    Text(
        text, Modifier.weight(weight),
        color = color, fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}
