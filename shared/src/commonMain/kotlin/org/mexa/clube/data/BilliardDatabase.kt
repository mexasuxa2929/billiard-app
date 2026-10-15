package org.mexa.clube.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.mexa.clube.data.dao.BarSaleDao
import org.mexa.clube.data.dao.BookingDao
import org.mexa.clube.data.dao.CategoryDao
import org.mexa.clube.data.dao.CheckDao
import org.mexa.clube.data.dao.LogDao
import org.mexa.clube.data.dao.ProductDao
import org.mexa.clube.data.dao.SessionDao
import org.mexa.clube.data.dao.SettingsDao
import org.mexa.clube.data.dao.TableDao
import org.mexa.clube.data.entity.AppSetting
import org.mexa.clube.data.entity.BarSale
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Booking
import org.mexa.clube.data.entity.Category
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.MovementLog
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.TableSession

@Database(
    entities = [
        BilliardTable::class,
        TableSession::class,
        Product::class,
        BarSale::class,
        BarSaleItem::class,
        CheckReceipt::class,
        MovementLog::class,
        AppSetting::class,
        Category::class,
        Booking::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
@ConstructedBy(BilliardDatabaseConstructor::class)
abstract class BilliardDatabase : RoomDatabase() {
    abstract fun tableDao(): TableDao
    abstract fun sessionDao(): SessionDao
    abstract fun productDao(): ProductDao
    abstract fun barSaleDao(): BarSaleDao
    abstract fun checkDao(): CheckDao
    abstract fun logDao(): LogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bookingDao(): BookingDao

    companion object {
        const val DB_NAME = "billiard_club.db"
    }
}

// Room compiler generates the actual implementation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BilliardDatabaseConstructor : RoomDatabaseConstructor<BilliardDatabase> {
    override fun initialize(): BilliardDatabase
}
