package ai.hev.app.data.local.db

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plain [Application]: the real one opens the Android keystore, which Robolectric lacks. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HevMigrationsTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), HevDatabase::class.java)

    private val dbName = "migration-test.db"

    @Test
    fun `2 to 3 keeps every row and normalises question types`() {
        helper.createDatabase(dbName, 2).use { db ->
            db.insert("history", SQLiteDatabase.CONFLICT_NONE, v2Row(1, "choice", choice = "billing"))
            db.insert("history", SQLiteDatabase.CONFLICT_NONE, v2Row(2, "Score", score = 1.5))
            db.insert("history", SQLiteDatabase.CONFLICT_NONE, v2Row(3, "noul", noul = 0.9))
            db.insert("history", SQLiteDatabase.CONFLICT_NONE, v2Row(4, "", choice = "a"))
        }

        helper.runMigrationsAndValidate(dbName, 3, true, *HevMigrations.ALL).use { db ->
            db.query("SELECT id, questionType, refused, providerId, protocol, inputTokens, rawJson FROM history ORDER BY id")
                .use { cursor ->
                    val types = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        types += cursor.getString(1)
                        assertEquals(0, cursor.getInt(2))
                        assertEquals(true, cursor.isNull(3) && cursor.isNull(4) && cursor.isNull(5))
                        assertEquals("{\"id\":${cursor.getLong(0)}}", cursor.getString(6))
                    }
                    assertEquals(listOf("choice", "score", "yes_no", "choice"), types)
                }
        }
    }

    @Test
    fun `migrated rows read back through room`() {
        helper.createDatabase(dbName, 2).use { db ->
            db.insert("history", SQLiteDatabase.CONFLICT_NONE, v2Row(7, "noul", noul = 0.25))
        }
        helper.runMigrationsAndValidate(dbName, 3, true, *HevMigrations.ALL).close()

        val room = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), HevDatabase::class.java, dbName)
            .addMigrations(*HevMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val entry = runBlocking { room.historyDao().getById(7) }!!.let(HistoryMapper::toDomain)

            assertEquals(DecisionKind.YesNo, entry.kind)
            assertEquals(DecisionOutcome.YesNo(0.25), entry.outcome)
            assertEquals("Is it urgent?", entry.instructions)
            assertNull(entry.providerId)
        } finally {
            room.close()
        }
    }

    private fun v2Row(
        id: Long,
        type: String,
        choice: String? = null,
        score: Double? = null,
        noul: Double? = null,
    ) = ContentValues().apply {
        put("id", id)
        put("createdAt", 1_700_000_000_000 + id)
        put("questionType", type)
        put("question", "Is it urgent?")
        putNull("state")
        put("optionsJson", "[]")
        put("probabilitiesJson", "{}")
        putNull("confidence")
        put("choice", choice)
        put("score", score)
        put("noul", noul)
        put("model", "jev-latest")
        put("providerName", "TypeSafe")
        put("rawJson", "{\"id\":$id}")
    }
}
