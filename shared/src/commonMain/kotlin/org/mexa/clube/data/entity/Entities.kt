package org.mexa.clube.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "tables")
data class BilliardTable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hourlyPrice: Double = 60000.0,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "'Standart'") val zone: String = "Standart"
)

val TABLE_ZONES = listOf("Standart", "VIP", "VIP LOUNGE", "Snooker")

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val tableName: String,
    val clientName: String = "",
    val phone: String = "",
    val timeMillis: Long = 0L,
    val note: String = "",
    val status: String = "ACTIVE" // ACTIVE, DONE, CANCELLED
)

@Entity(tableName = "sessions")
data class TableSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val tableName: String,
    val startTime: Long, // epoch millis
    val endTime: Long? = null,
    val hourlyRate: Double,
    val status: String = "OPEN", // OPEN, CLOSED, CANCELLED
    val tableSum: Double = 0.0,
    val barSum: Double = 0.0,
    val discount: Double = 0.0,
    val totalSum: Double = 0.0,
    val note: String = ""
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Umumiy",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stockQty: Double = 0.0,
    val unit: String = "dona",
    val isActive: Boolean = true
)

@Entity(tableName = "bar_sales")
data class BarSale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null, // null = to'g'ridan-to'g'ri savdo
    val tableId: Long? = null,
    val createdAt: Long,
    val closedAt: Long? = null,
    val status: String = "CLOSED", // OPEN (stolga yozilgan), CLOSED
    val totalBuy: Double = 0.0,
    val totalSell: Double = 0.0
)

@Entity(tableName = "bar_sale_items")
data class BarSaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double = 1.0,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0
)

@Entity(tableName = "checks")
data class CheckReceipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val saleId: Long? = null,
    val createdAt: Long,
    val tableName: String = "",
    val tableAmount: Double = 0.0,
    val barAmount: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "Naqd",
    val details: String = ""
)

@Entity(tableName = "logs")
data class MovementLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String, // TABLE_START, TABLE_STOP, BAR_SALE, PRODUCT, SYSTEM
    val description: String,
    val amount: Double = 0.0
)

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
