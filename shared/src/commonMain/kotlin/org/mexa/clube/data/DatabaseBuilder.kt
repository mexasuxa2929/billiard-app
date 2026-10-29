package org.mexa.clube.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun getDatabaseBuilder(): RoomDatabase.Builder<BilliardDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<BilliardDatabase>): BilliardDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        // WAL (Write-Ahead Logging) rejimi.
        //
        // Ilgari TRUNCATE ishlatilgan edi, lekin Windows'da SQLite jurnal faylini
        // o'chirishda `SQLITE_IOERR_DELETE_NOENT` (2570, "disk I/O error") qaytaradi
        // va bazaga BIRINCHI yozishdayoq xato beradi. WAL rejimi baza faylida
        // saqlanadi, shuning uchun barcha ulanishlar uchun bir xil ishlaydi
        // (Windows, Linux, macOS) va o'qish bilan yozishni birga bajarishga
        // ruxsat beradi.
        //
        // Dastur yopilganda Room bazani checkpoint qilib yopadi, `-wal` / `-shm`
        // yon fayllari o'chiriladi — baza yana bitta ko'chiriladigan `.db` fayl
        // bo'lib qoladi (noutbuk/PC ko'chirish uchun qulay).
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
