package org.mexa.clube.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun getDatabaseBuilder(): RoomDatabase.Builder<BilliardDatabase>

fun getRoomDatabase(builder: RoomDatabase.Builder<BilliardDatabase>): BilliardDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        // TRUNCATE: WAL rejimidagi `-wal` / `-shm` yon fayllari qolmaydi.
        // Shu sababli baza har doim BITTA ko'chiriladigan `.db` fayl bo'ladi.
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
