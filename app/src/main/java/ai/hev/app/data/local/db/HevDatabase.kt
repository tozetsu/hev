package ai.hev.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 2, exportSchema = false)
abstract class HevDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var instance: HevDatabase? = null

        fun get(context: Context): HevDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HevDatabase::class.java,
                    "hev.db",
                )
                    // v1 had no Migration objects; wipe on upgrade rather than crash.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
