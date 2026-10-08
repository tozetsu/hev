package ai.hev.app.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Path

/** The app database at [file], on the SQLite bundled with the app. */
internal fun openHevDatabase(file: Path): HevDatabase =
    Room.databaseBuilder<HevDatabase>(file.toString()).build(BundledSQLiteDriver())
