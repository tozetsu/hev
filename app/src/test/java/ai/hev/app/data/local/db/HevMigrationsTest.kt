package ai.hev.app.data.local.db

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.DecisionOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plain [Application]: the real one opens the Android keystore, which Robolectric lacks. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HevMigrationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile = context.getDatabasePath("migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = dbFile,
        driver = AndroidSQLiteDriver(),
        databaseClass = HevDatabase::class,
    )

    @Test
    fun `2 to 3 keeps every row and normalises question types`() {
        helper.createDatabase(2).use { db ->
            db.insertV2Row(1, "choice", choice = "billing")
            db.insertV2Row(2, "Score", score = 1.5)
            db.insertV2Row(3, "noul", noul = 0.9)
            db.insertV2Row(4, "", choice = "a")
        }

        helper.runMigrationsAndValidate(3, HevMigrations.ALL.toList()).use { db ->
            db.prepare("SELECT id, questionType, refused, providerId, protocol, inputTokens, rawJson FROM history ORDER BY id")
                .use { row ->
                    val types = mutableListOf<String>()
                    while (row.step()) {
                        types += row.getText(1)
                        assertEquals(0L, row.getLong(2))
                        assertTrue(row.isNull(3) && row.isNull(4) && row.isNull(5))
                        assertEquals("{\"id\":${row.getLong(0)}}", row.getText(6))
                    }
                    assertEquals(listOf("choice", "score", "yes_no", "choice"), types)
                }
        }
    }

    @Test
    fun `migrated rows read back through room`() {
        helper.createDatabase(2).use { db -> db.insertV2Row(7, "noul", noul = 0.25) }
        helper.runMigrationsAndValidate(3, HevMigrations.ALL.toList()).close()

        val room = Room.databaseBuilder<HevDatabase>(context, dbFile.path)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(*HevMigrations.ALL)
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

    private fun SQLiteConnection.insertV2Row(
        id: Long,
        type: String,
        choice: String? = null,
        score: Double? = null,
        noul: Double? = null,
    ) = prepare(
        """
        INSERT INTO history (id, createdAt, questionType, question, state, optionsJson, probabilitiesJson,
            confidence, choice, score, noul, model, providerName, rawJson)
        VALUES (?, ?, ?, 'Is it urgent?', NULL, '[]', '{}', NULL, ?, ?, ?, 'jev-latest', 'TypeSafe', ?)
        """.trimIndent(),
    ).use { row ->
        row.bindLong(1, id)
        row.bindLong(2, 1_700_000_000_000 + id)
        row.bindText(3, type)
        if (choice != null) row.bindText(4, choice) else row.bindNull(4)
        if (score != null) row.bindDouble(5, score) else row.bindNull(5)
        if (noul != null) row.bindDouble(6, noul) else row.bindNull(6)
        row.bindText(7, "{\"id\":$id}")
        row.step()
    }
}
