package ai.hev.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Every schema step, in order. History is always carried forward; never add a destructive fallback. */
object HevMigrations {

    /** Adds provider, protocol, refusal and token columns; normalises the stored question type. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE history ADD COLUMN providerId TEXT")
            db.execSQL("ALTER TABLE history ADD COLUMN protocol TEXT")
            db.execSQL("ALTER TABLE history ADD COLUMN refused INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE history ADD COLUMN inputTokens INTEGER")
            db.execSQL(
                """
                UPDATE history SET questionType = CASE lower(questionType)
                    WHEN 'noul' THEN 'yes_no'
                    WHEN 'score' THEN 'score'
                    ELSE 'choice'
                END
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}
