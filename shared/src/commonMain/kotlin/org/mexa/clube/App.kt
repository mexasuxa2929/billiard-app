package org.mexa.clube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mexa.clube.ui.components.SideMenu
import org.mexa.clube.ui.screens.BarScreen
import org.mexa.clube.ui.screens.ExpensesScreen
import org.mexa.clube.ui.screens.HistoryScreen
import org.mexa.clube.ui.screens.NotesScreen
import org.mexa.clube.ui.screens.ReportsScreen
import org.mexa.clube.ui.screens.SettingsScreen
import org.mexa.clube.ui.screens.TablesScreen
import org.mexa.clube.ui.theme.Pro
import org.mexa.clube.ui.theme.ProTheme
import org.mexa.clube.ui.viewmodel.AppViewModel

@Composable
fun App(vm: AppViewModel) {
    ProTheme {
        var screen by remember { mutableStateOf(1) } // skrinshotdagidek default: Bar
        val scope = rememberCoroutineScope()
        val snack = remember { SnackbarHostState() }
        val err by vm.error.collectAsState()
        LaunchedEffect(err) {
            if (err != null) {
                scope.launch { snack.showSnackbar(err!!) }
                vm.clearError()
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().background(Pro.Bg)) {
            // Tor oynada yon menyu faqat ikonkalar bilan (rail) ko'rinadi
            val compactMenu = maxWidth < 900.dp
            Row(Modifier.fillMaxSize()) {
                SideMenu(screen, compactMenu) { screen = it }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (screen) {
                        0 -> TablesScreen(vm)
                        1 -> BarScreen(vm)
                        2 -> ReportsScreen(vm)
                        3 -> ExpensesScreen(vm)
                        4 -> NotesScreen(vm)
                        5 -> HistoryScreen(vm)
                        6 -> SettingsScreen(vm)
                    }
                }
            }
        }
        SnackbarHost(snack)
    }
}
