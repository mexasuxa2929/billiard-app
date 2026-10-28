package org.mexa.clube

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mexa.clube.data.BilliardDatabase
import org.mexa.clube.data.entity.Note
import org.mexa.clube.data.getRoomDatabase
import org.mexa.clube.data.repository.BilliardRepository
import org.mexa.clube.domain.currentMillis
import org.mexa.clube.domain.startOfDay
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DAY = 24L * 3600 * 1000

class NoteTest {
    private val tmpDirs = mutableListOf<File>()
    private val openDbs = mutableListOf<BilliardDatabase>()

    private fun newDb(): BilliardDatabase {
        val dir = File(System.getProperty("java.io.tmpdir"), "billiard-note-${System.nanoTime()}")
        dir.mkdirs()
        tmpDirs += dir
        val db = getRoomDatabase(
            Room.databaseBuilder<BilliardDatabase>(name = File(dir, "test.db").absolutePath)
        )
        openDbs += db
        return db
    }

    @AfterTest
    fun cleanup() {
        openDbs.forEach { runCatching { it.close() } }
        openDbs.clear()
        tmpDirs.forEach { it.deleteRecursively() }
        tmpDirs.clear()
    }

    @Test
    fun noteIsSavedWithDateReasonAndAmount() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val today = startOfDay(currentMillis())

        repo.saveNote(Note(noteDate = today, reason = "Ijara to'lash", amount = 3_000_000.0))
        repo.saveNote(Note(noteDate = today + DAY, reason = "Mahsulot olish", amount = 500_000.0))

        val notes = repo.notes.first()
        assertEquals(2, notes.size)
        assertEquals("Mahsulot olish", notes[0].reason, "keyingi sana yuqorida turadi")
        assertEquals(today + DAY, notes[0].noteDate)
        assertEquals(3_500_000.0, notes.sumOf { it.amount })
    }

    @Test
    fun noteWithoutAmountIsAllowed() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val today = startOfDay(currentMillis())

        repo.saveNote(Note(noteDate = today, reason = "Eslatma: kalitni olib kelish"))
        val notes = repo.notes.first()
        assertEquals(1, notes.size)
        assertEquals(0.0, notes[0].amount)
    }

    @Test
    fun noteRequiresReason() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val today = startOfDay(currentMillis())

        val err = runCatching {
            repo.saveNote(Note(noteDate = today, reason = "   ", amount = 1000.0))
        }.exceptionOrNull()
        assertTrue(err is IllegalArgumentException, "sababsiz zametka saqlanmasligi kerak")
        assertTrue(repo.notes.first().isEmpty())
    }

    @Test
    fun noteCanBeEditedAndDeleted() = runBlocking {
        val db = newDb()
        val repo = BilliardRepository(db)
        val today = startOfDay(currentMillis())

        val id = repo.saveNote(Note(noteDate = today, reason = "Eski", amount = 10_000.0))
        val saved = repo.notes.first().first()

        repo.saveNote(saved.copy(id = id, reason = "Yangi sabab", amount = 20_000.0))
        val updated = repo.notes.first().first()
        assertEquals("Yangi sabab", updated.reason)
        assertEquals(20_000.0, updated.amount)

        repo.deleteNote(id)
        assertTrue(repo.notes.first().isEmpty())
    }
}
