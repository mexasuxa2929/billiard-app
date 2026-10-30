package org.mexa.clube.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.mexa.clube.data.entity.AppSetting
import org.mexa.clube.data.entity.BarSale
import org.mexa.clube.data.entity.BarSaleItem
import org.mexa.clube.data.entity.BilliardTable
import org.mexa.clube.data.entity.Booking
import org.mexa.clube.data.entity.Category
import org.mexa.clube.data.entity.CheckReceipt
import org.mexa.clube.data.entity.Expense
import org.mexa.clube.data.entity.MovementLog
import org.mexa.clube.data.entity.Note
import org.mexa.clube.data.entity.Product
import org.mexa.clube.data.entity.SessionPart
import org.mexa.clube.data.entity.TableSession

@Dao
interface TableDao {
    @Query("SELECT * FROM `tables` WHERE isActive = 1 ORDER BY sortOrder, id")
    fun observeTables(): Flow<List<BilliardTable>>

    @Query("SELECT * FROM `tables` WHERE isActive = 1 ORDER BY sortOrder, id")
    suspend fun getTables(): List<BilliardTable>

    @Query("SELECT * FROM `tables` ORDER BY sortOrder, id")
    suspend fun getAllTables(): List<BilliardTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(table: BilliardTable): Long

    @Update
    suspend fun update(table: BilliardTable)

    @Query("DELETE FROM `tables` WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE status = 'OPEN' ORDER BY startTime DESC")
    fun observeOpenSessions(): Flow<List<TableSession>>

    @Query("SELECT * FROM sessions WHERE status = 'OPEN' AND tableId = :tableId LIMIT 1")
    suspend fun getOpenByTable(tableId: Long): TableSession?

    @Query("SELECT * FROM sessions WHERE status = 'CLOSED' AND endTime >= :from AND endTime < :to ORDER BY endTime DESC")
    suspend fun getBetween(from: Long, to: Long): List<TableSession>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): TableSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TableSession): Long

    @Update
    suspend fun update(session: TableSession)

    @Query("SELECT COUNT(*) FROM sessions WHERE status='CLOSED' AND endTime >= :from AND endTime < :to")
    suspend fun countClosed(from: Long, to: Long): Int

    @Query("SELECT * FROM sessions WHERE tableId = :tableId AND status = 'CLOSED' ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastClosedByTable(tableId: Long): TableSession?
}

@Dao
interface SessionPartDao {
    @Query("SELECT * FROM session_parts WHERE sessionId = :sessionId ORDER BY partNo")
    suspend fun getBySession(sessionId: Long): List<SessionPart>

    @Query("SELECT * FROM session_parts WHERE sessionId = :sessionId ORDER BY partNo")
    fun observeBySession(sessionId: Long): Flow<List<SessionPart>>

    @Insert
    suspend fun insert(part: SessionPart): Long

    @Query("DELETE FROM session_parts WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM session_parts WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name")
    fun observeProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET stockQty = stockQty - :qty WHERE id = :id")
    suspend fun decreaseStock(id: Long, qty: Double)

    @Query("UPDATE products SET stockQty = stockQty + :qty WHERE id = :id")
    suspend fun increaseStock(id: Long, qty: Double)

    @Query("SELECT COUNT(*) FROM products WHERE category = :name")
    suspend fun countByCategory(name: String): Int

    @Query("UPDATE products SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)
}

@Dao
interface BarSaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: BarSale): Long

    @Insert
    suspend fun insertItems(items: List<BarSaleItem>)

    @Query("SELECT * FROM bar_sales WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: Long): List<BarSale>

    /**
     * Hisobot uchun savdolar. To'g'ridan-to'g'ri savdo to'lov qilingan paytda ([createdAt]),
     * stolga biriktirilgan savdo esa stol yopilgan paytda ([closedAt]) daromadga kiradi.
     * Bekor qilingan sessiyalarning savdolari hisobotga umuman kirmaydi.
     */
    @Query(
        """
        SELECT * FROM bar_sales
        WHERE status != 'CANCELLED'
          AND (
            (sessionId IS NULL AND createdAt >= :from AND createdAt < :to)
            OR (sessionId IS NOT NULL AND closedAt >= :from AND closedAt < :to)
          )
        ORDER BY createdAt DESC
        """
    )
    suspend fun getForReport(from: Long, to: Long): List<BarSale>

    /** Sessiya yopilganda unga biriktirilgan barcha savdolarni "to'langan" deb belgilaydi. */
    @Query("UPDATE bar_sales SET status = 'CLOSED', closedAt = :closedAt WHERE sessionId = :sessionId AND status = 'OPEN'")
    suspend fun closeForSession(sessionId: Long, closedAt: Long)

    /** Sessiya bekor qilinsa — savdolar hisobotdan chiqariladi (stok allaqachon qaytarilgan). */
    @Query("UPDATE bar_sales SET status = 'CANCELLED', closedAt = :closedAt WHERE sessionId = :sessionId AND status != 'CANCELLED'")
    suspend fun cancelForSession(sessionId: Long, closedAt: Long)

    /** Eski bazadagi, sessiyaga biriktirilgan, lekin yopilish vaqti yozilmagan savdolar. */
    @Query("SELECT * FROM bar_sales WHERE sessionId IS NOT NULL AND closedAt IS NULL")
    suspend fun getAttachedWithoutCloseTime(): List<BarSale>

    @Query("UPDATE bar_sales SET status = :status, closedAt = :closedAt WHERE id = :id")
    suspend fun markSaleStatus(id: Long, status: String, closedAt: Long?)

    @Query("SELECT * FROM bar_sale_items WHERE saleId IN (:saleIds)")
    suspend fun getItemsBySaleIds(saleIds: List<Long>): List<BarSaleItem>

    @Query("SELECT * FROM bar_sale_items WHERE saleId = :saleId")
    suspend fun getItems(saleId: Long): List<BarSaleItem>

    @Query("SELECT * FROM bar_sale_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): BarSaleItem?

    @Query("DELETE FROM bar_sale_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT * FROM bar_sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): BarSale?

    @Query("DELETE FROM bar_sales WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Update
    suspend fun updateSale(sale: BarSale)
}

@Dao
interface CheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(check: CheckReceipt): Long

    @Query("SELECT * FROM checks WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    suspend fun getBetween(from: Long, to: Long): List<CheckReceipt>

    @Query("SELECT * FROM checks ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLast(limit: Int): List<CheckReceipt>
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: MovementLog)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLast(limit: Int): List<MovementLog>

    @Query("SELECT * FROM logs WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    suspend fun getBetween(from: Long, to: Long): List<MovementLog>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: AppSetting)

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<AppSetting>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY spentAt DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE spentAt >= :from AND spentAt < :to ORDER BY spentAt DESC, id DESC")
    suspend fun getBetween(from: Long, to: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY noteDate DESC, id DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' ORDER BY timeMillis")
    fun observeActive(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' ORDER BY timeMillis")
    suspend fun getActive(): List<Booking>

    @Query("SELECT * FROM bookings WHERE tableId = :tableId AND status = 'ACTIVE' ORDER BY timeMillis LIMIT 1")
    suspend fun getActiveByTable(tableId: Long): Booking?

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(booking: Booking): Long

    @Update
    suspend fun update(booking: Booking)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
