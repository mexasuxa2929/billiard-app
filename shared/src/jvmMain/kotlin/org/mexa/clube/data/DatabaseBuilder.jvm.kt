package org.mexa.clube.data

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<BilliardDatabase> {
    val home = System.getProperty("user.home") ?: "."
    val dir = File(home, ".billiardapp")
    if (!dir.exists()) dir.mkdirs()
    val dbFile = File(dir, BilliardDatabase.DB_NAME)
    return Room.databaseBuilder<BilliardDatabase>(
        name = dbFile.absolutePath
    )
}
