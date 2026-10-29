package org.mexa.clube

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.mexa.clube.data.getDatabaseBuilder
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.ui.viewmodel.AppViewModel
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(width = 1280.dp, height = 800.dp)

    // Baza butun dastur uchun bitta.
    val db = remember {
        val builder = getDatabaseBuilder()
        getRoomDatabase(builder)
    }
    val repo = remember(db) { BilliardRepository(db) }
    val vm = remember(repo) { AppViewModel(repo) }

    Window(
        onCloseRequest = {
            // WAL rejimida oxirgi yozuvlar `-wal` faylida bo'ladi. Yopishdan oldin
            // bazani checkpoint qilib yopamiz — shunda hamma ma'lumot `.db` faylga
            // tushadi va yon fayllar o'chiriladi (baza yagona fayl bo'lib qoladi).
            runCatching { db.close() }
            exitApplication()
        },
        title = "Billiard Club Manager v2.5 Pro",
        state = state
    ) {
        // UI shu o'lchamdan kichik bo'lsa ham buzilmaydi, lekin
        // undan kichraytirib bo'lmaydi (eng kichik ishlaydigan oyna).
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(780, 560)
        }
        App(vm)
    }
}
