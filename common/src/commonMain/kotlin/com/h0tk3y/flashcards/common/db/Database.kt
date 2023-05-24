package com.h0tk3y.flashcards.common.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.Timestamp
import com.h0tk3y.flashcards.common.TimestampAdapter
import com.h0tk3y.flashcards.common.datetime.nowTimestamp
import com.h0tk3y.flashcards.db.AppDatabase
import comh0tk3yflashcardsdb.CardList
import comh0tk3yflashcardsdb.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(
        databaseDriverFactory.createDriver(),
        CardListAdapter = CardList.Adapter(TimestampAdapter()),
        FlashcardAdapter = Flashcard.Adapter(TimestampAdapter())
    )
    private val dbQuery = database.appDatabaseQueries

    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.deleteAllLists()
            dbQuery.deleteAllFlashcards()
        }
    }

    internal fun selectAllLists(): Flow<LoadState<List<com.h0tk3y.flashcards.common.model.CardList>>> {
        return dbQuery.selectAllLists(::cardList)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { LoadState.Loaded(it) }
    }

    internal fun selectListById(id: Long): Flow<LoadState<com.h0tk3y.flashcards.common.model.CardList>> {
        return dbQuery.selectListById(id).asFlow().map { query ->
            val result = query.executeAsOneOrNull()
            if (result != null)
                LoadState.Loaded(com.h0tk3y.flashcards.common.model.CardList(result.id, result.name, result.modified))
            else LoadState.FAILED
        }
    }

    internal fun selectAllFlashcards(): Flow<List<com.h0tk3y.flashcards.common.model.Flashcard>> {
        return dbQuery.selectAllFlashcards(::flashcard).asFlow().mapToList(Dispatchers.Default)
    }

    internal fun selectCardsInList(list: com.h0tk3y.flashcards.common.model.CardList): Flow<LoadState<List<com.h0tk3y.flashcards.common.model.Flashcard>>> {
        return dbQuery.selectFlashCardsInList(list.id, ::flashcard)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { LoadState.Loaded(it) }
    }

    internal fun getCardDetailsById(cardId: Long): Flow<LoadState<com.h0tk3y.flashcards.common.model.Flashcard>> {
        return dbQuery.selectFlashcardById(cardId, ::flashcard)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { if (it != null) LoadState.Loaded(it) else LoadState.FAILED }
    }

    internal fun deleteCard(
        id: Long
    ) {
        dbQuery.deleteFlashcard(id)
    }

    internal fun deleteCardList(
        id: Long
    ) {
        dbQuery.deleteList(id)
    }

    internal fun updateCard(
        id: Long,
        unknownWord: String,
        knownWord: String,
        comment: String?,
        modified: Timestamp
    ) {
        dbQuery.modifyFlashcard(unknownWord, knownWord, comment, modified, id)
    }

    internal fun updateList(
        id: Long,
        name: String,
        modified: Timestamp
    ) {
        dbQuery.modifyList(name, modified, id)
    }

    internal fun createCard(
        unknownWord: String,
        knownWord: String,
        insertToList: com.h0tk3y.flashcards.common.model.CardList,
        comment: String?
    ) {
        dbQuery.addFlashcard(
            id = null,
            unknownWord,
            knownWord,
            Timestamp(nowTimestamp()),
            comment,
            insertToList.id
        )
    }

    internal fun insertAllCards(
        cards: List<com.h0tk3y.flashcards.common.model.Flashcard>,
        listId: Long
    ) {
        dbQuery.transaction {
            cards.forEach { card ->
                dbQuery.addFlashcard(
                    id = null,
                    unknownWord = card.unknownWord,
                    knownWord = card.knownWord,
                    modified = card.modified,
                    comment = card.comment,
                    listId = listId
                )
            }
        }
    }

    internal fun createList(name: String): ItemSaveResult<com.h0tk3y.flashcards.common.model.CardList> {
        val exists = dbQuery.findListByName(name).execute { it.next() }.value
        if (exists) {
            return ItemSaveResult.ALREADY_EXISTS
        }
        val now = Timestamp(nowTimestamp())
        dbQuery.transaction {
            dbQuery.addList(id = null, name, now)
        }
        val lastInsertedId = dbQuery.lastInsertedId().executeAsOne()
        val result = com.h0tk3y.flashcards.common.model.CardList(lastInsertedId, name, now)
        return ItemSaveResult.Ok(result)
    }

    private fun cardList(id: Long, name: String, modified: Timestamp) =
        com.h0tk3y.flashcards.common.model.CardList(id, name, modified)

    private fun flashcard(
        id: Long,
        unknownWord: String,
        knownWord: String,
        modified: Timestamp,
        comment: String?,
        listId: Long
    ) = com.h0tk3y.flashcards.common.model.Flashcard(id, knownWord, unknownWord, modified, comment, listId)
}

sealed interface ItemSaveResult<out T> {
    data class Ok<T>(val createdItem: T) : ItemSaveResult<T>
    object ALREADY_EXISTS : ItemSaveResult<Nothing>
}