package org.mexa.clube

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.mexa.clube.data.getDatabaseBuilder
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.ui.viewmodel.AppViewModel

fun main() = application {
    val state = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Billiard Club Manager v2.4 Pro",
        state = state
    ) {
        val db = remember {
            val builder = getDatabaseBuilder()
            getRoomDatabase(builder)
        }
        val repo = remember(db) { BilliardRepository(db) }
        val vm = remember(repo) { AppViewModel(repo) }
        App(vm)
    }
}
