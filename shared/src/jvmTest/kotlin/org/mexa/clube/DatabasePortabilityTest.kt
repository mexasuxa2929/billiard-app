package org.mexa.clube

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.DatabaseLocation
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Portable baza: `.db` faylni nusxalash, tekshirish va yagona fayl bo'lib qolishi.
 */
class DatabasePortabilityTest {

    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun tmpDir(prefix: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "$prefix-${System.nanoTime()}")
        dir.mkdirs()
        tmpDirs += dir
        return dir
    }

    @AfterTest
    fun cleanup() {
        openDbs.forEach { runCatching { it.close() } }
        openDbs.clear()
        tmpDirs.forEach { runCatching { it.deleteRecursively() } }
        tmpDirs.clear()
    }

    @Test
    fun backupCreatesSinglePortableFile() = runBlocking {
        val dir = tmpDir("billiard-portable")
        val dbFile = File(dir, "billiard_club.db")
        val db = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = dbFile.absolutePath)
        )
        openDbs += db
        val repo = BilliardRepository(db)
        repo.addTable("Test stol", 50_000.0)

        // Joriy baza yaroqli bo'lishi kerak
        assertNull(DatabaseLocation.validate(dbFile.absolutePath), "joriy baza yaroqli bo'lishi kerak")

        // Dastur ishlab turgan paytda ham nusxa olinishi kerak (VACUUM INTO)
        val liveDir = File(dir, "live-backup")
        val livePath = DatabaseLocation.backup(dbFile.absolutePath, liveDir.absolutePath)
        assertTrue(File(livePath).isFile, "ochiq baza nusxasi yaratilishi kerak")
        assertNull(DatabaseLocation.validate(livePath), "ochiq baza nusxasi yaroqli bo'lishi kerak")

        db.close()
        openDbs.remove(db)

        // TRUNCATE rejimi: WAL yon fayllari qolmasligi kerak
        assertTrue(!File("${dbFile.path}-wal").exists(), "-wal fayl qolmasligi kerak")
        assertTrue(!File("${dbFile.path}-shm").exists(), "-shm fayl qolmasligi kerak")

        // --- Eski (WAL rejimida yaratilgan) bazani simulyatsiya qilamiz ---
        BundledSQLiteDriver().open(dbFile.absolutePath).use { conn ->
            conn.prepare("PRAGMA journal_mode = WAL").use { it.step() }
        }
        val walMode = BundledSQLiteDriver().open(dbFile.absolutePath).use { conn ->
            conn.prepare("PRAGMA journal_mode").use { st -> if (st.step()) st.getText(0) else "" }
        }
        assertEquals("wal", walMode.lowercase(), "test uchun baza WAL rejimiga o'tishi kerak")

        // Dastur bazani qayta ochadi -> TRUNCATE ga qaytishi va yon fayl qoldirmasligi kerak
        val reopened = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = dbFile.absolutePath)
        )
        openDbs += reopened
        BilliardRepository(reopened).addTable("WAL dan keyin", 40_000.0)
        reopened.close()
        openDbs.remove(reopened)
        assertTrue(!File("${dbFile.path}-wal").exists(), "WAL bazasi TRUNCATE ga o'tishi kerak (-wal)")
        assertTrue(!File("${dbFile.path}-shm").exists(), "WAL bazasi TRUNCATE ga o'tishi kerak (-shm)")
        assertNull(DatabaseLocation.validate(dbFile.absolutePath), "WAL dan keyin baza yaroqli bo'lishi kerak")

        val backupDir = File(dir, "backup")
        val backupPath = DatabaseLocation.backup(dbFile.absolutePath, backupDir.absolutePath)
        val backupFile = File(backupPath)

        assertTrue(backupFile.isFile, "nusxa fayl yaratilishi kerak")
        assertEquals(1, backupDir.listFiles()?.count { it.name.endsWith(".db") }, "papkada bitta .db bo'lishi kerak")
        assertNull(DatabaseLocation.validate(backupPath), "nusxa yaroqli baza bo'lishi kerak")
        assertTrue(!File("$backupPath-wal").exists(), "nusxada -wal bo'lmasligi kerak")
        assertTrue(!File("$backupPath-shm").exists(), "nusxada -shm bo'lmasligi kerak")

        // Nusxadagi ma'lumot o'qilishi kerak
        val copyDb = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = backupFile.absolutePath)
        )
        openDbs += copyDb
        val names = copyDb.tableDao().getAllTables().map { it.name }
        assertTrue("Test stol" in names, "nusxada stol bo'lishi kerak")
    }

    @Test
    fun copiedFileWorksInAnotherFolder() = runBlocking {
        val sourceDir = tmpDir("billiard-source")
        val source = File(sourceDir, "billiard_club.db")
        val db = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = source.absolutePath)
        )
        openDbs += db
        BilliardRepository(db).addTable("Ko'chirilgan stol", 70_000.0)
        db.close()
        openDbs.remove(db)

        // Oddiy fayl nusxalash — portativlik talabi
        val targetDir = tmpDir("billiard-target")
        val target = File(targetDir, "boshqa_nom.db")
        source.copyTo(target, overwrite = true)

        assertEquals(
            1, targetDir.listFiles()?.size,
            "nusxalangandan keyin papkada faqat bitta fayl bo'lishi kerak"
        )
        assertNull(DatabaseLocation.validate(target.absolutePath), "ko'chirilgan fayl yaroqli bo'lishi kerak")

        val copied = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = target.absolutePath)
        )
        openDbs += copied
        val names = copied.tableDao().getAllTables().map { it.name }
        assertTrue("Ko'chirilgan stol" in names, "ko'chirilgan bazada ma'lumot bo'lishi kerak")
    }

    @Test
    fun validateRejectsForeignFiles() {
        val dir = tmpDir("billiard-invalid")

        // Umuman baza emas
        val text = File(dir, "matn.db")
        text.writeText("bu SQLite fayli emas, oddiy matn")
        assertNotNull(DatabaseLocation.validate(text.absolutePath), "matn fayl rad etilishi kerak")

        // Yo'q fayl
        assertNotNull(DatabaseLocation.validate(File(dir, "yoq.db").absolutePath), "yo'q fayl rad etilishi kerak")

        // SQLite, lekin Billiard bazasi emas (user_version = 0)
        val foreign = File(dir, "foreign.db")
        BundledSQLiteDriver().open(foreign.absolutePath).use { conn ->
            conn.prepare("CREATE TABLE foo (id INTEGER PRIMARY KEY)").use { it.step() }
        }
        assertNotNull(DatabaseLocation.validate(foreign.path), "begona SQLite bazasi rad etilishi kerak")
    }
}
