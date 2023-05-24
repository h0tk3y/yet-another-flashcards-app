package com.h0tk3y.flashcards.common.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.h0tk3y.flashcards.db.AppDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val path = File(System.getProperty("user.home"), ".com.h0tk3y.flashcards/app.db")
        path.parentFile.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:$path").also {
            if (!path.exists()) {
                AppDatabase.Schema.create(it)
            }
        }
    }
}