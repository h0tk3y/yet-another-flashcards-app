package com.h0tk3y.flashcards.common

import app.cash.sqldelight.ColumnAdapter

class Timestamp(val value: Long)

class TimestampAdapter : ColumnAdapter<Timestamp, Long> {
    override fun decode(databaseValue: Long): Timestamp = Timestamp(databaseValue)
    override fun encode(value: Timestamp): Long = value.value
}