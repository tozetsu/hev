package ai.hev.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

/** The app database in the standard databases directory, on the platform SQLite. */
internal fun openHevDatabase(context: Context): HevDatabase =
    Room.databaseBuilder<HevDatabase>(context.applicationContext, HevDatabase.NAME).build(AndroidSQLiteDriver())
