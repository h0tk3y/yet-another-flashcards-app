package com.h0tk3y.flashcards.common.model

import com.h0tk3y.flashcards.common.Timestamp

data class Flashcard(
    val id: Long,
    val knownWord: String,
    val unknownWord: String,
    val modified: Timestamp,
    val comment: String?,
    val listId: Long
)

data class CardList(
    val id: Long,
    val name: String,
    val modified: Timestamp
)