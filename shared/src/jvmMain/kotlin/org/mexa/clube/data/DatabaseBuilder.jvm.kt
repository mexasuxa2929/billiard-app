package org.mexa.clube.data

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<BilliardDatabase> {
    // Baza yo'li DatabaseLocation orqali aniqlanadi: u ham standart joyni,
    // ham foydalanuvchi tanlagan `.db` faylni qo'llab-quvvatlaydi.
    val dbPath = DatabaseLocation.activePath()
    return Room.databaseBuilder<BilliardDatabase>(
        name = dbPath
    )
}
