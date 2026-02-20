package com.ipanda.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ipanda.util.androidContext
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = androidContext ?: throw IllegalStateException("androidContext must be initialized")
    val dbFile = context.getDatabasePath("favorite_movies.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}
