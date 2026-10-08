package ai.hev.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(entities = [HistoryEntity::class], version = 3, exportSchema = true)
abstract class HevDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "hev.db"

        @Volatile private var instance: HevDatabase? = null

        fun get(context: Context): HevDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<HevDatabase>(context.applicationContext, NAME)
                    .setDriver(AndroidSQLiteDriver())
                    .addMigrations(*HevMigrations.ALL)
                    .build()
                    .also { instance = it }
            }
    }
}
