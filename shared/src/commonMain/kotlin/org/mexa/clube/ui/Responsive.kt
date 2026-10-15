package org.mexa.clube.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mexa.clube.ui.theme.Pro

/**
 * Ekran (oyna) o'lchamiga qarab UI zichligini tanlash uchun sinflar.
 *  - Compact  : tor oyna  (< 760dp)  — karta ko'rinishlari, ustma-ust joylashuv
 *  - Medium   : o'rta oyna (760–1040dp)
 *  - Expanded : keng oyna  (>= 1040dp) — to'liq jadval va ko'p ustunlar
 */
enum class ScreenSize { Compact, Medium, Expanded }

data class ScreenMetrics(
    val width: Dp,
    val height: Dp,
    val size: ScreenSize
) {
    val compact: Boolean get() = size == ScreenSize.Compact
    val medium: Boolean get() = size == ScreenSize.Medium
    val expanded: Boolean get() = size == ScreenSize.Expanded

    /** Ekran chetlaridagi bo'sh joy */
    val padding: Dp
        get() = when (size) {
            ScreenSize.Compact -> 10.dp
            ScreenSize.Medium -> 14.dp
            ScreenSize.Expanded -> 20.dp
        }

    /** Bo'limlar orasidagi vertikal masofa */
    val spacing: Dp
        get() = when (size) {
            ScreenSize.Compact -> 8.dp
            ScreenSize.Medium -> 11.dp
            ScreenSize.Expanded -> 14.dp
        }

    /** Kartalar orasidagi masofa */
    val cardSpacing: Dp
        get() = if (compact) 8.dp else 12.dp

    /** Tor ekranda jadval o'rniga kartalardan foydalanish */
    val useCards: Boolean get() = compact
}

fun screenMetrics(width: Dp, height: Dp): ScreenMetrics = ScreenMetrics(
    width = width,
    height = height,
    size = when {
        width < 760.dp -> ScreenSize.Compact
        width < 1040.dp -> ScreenSize.Medium
        else -> ScreenSize.Expanded
    }
)

/**
 * Har bir ekran uchun umumiy "scaffold": fon, chet bo'shliqlari va
 * o'lchamga moslashgan metrikalar. Ichidagi kontent ColumnScope da.
 */
@Composable
fun ResponsiveScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.(ScreenMetrics) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Pro.Bg)) {
        val metrics = remember(maxWidth, maxHeight) { screenMetrics(maxWidth, maxHeight) }
        val base = Modifier.fillMaxSize().padding(metrics.padding)
        Column(
            if (scrollable) base.verticalScroll(rememberScrollState()) else base,
            verticalArrangement = Arrangement.spacedBy(metrics.spacing)
        ) {
            content(metrics)
        }
    }
}
