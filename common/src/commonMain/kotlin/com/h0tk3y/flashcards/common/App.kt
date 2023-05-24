package com.h0tk3y.flashcards.common

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.scenes.AllCardListsScene
import com.h0tk3y.flashcards.common.scenes.EditCardDetailsScene
import com.h0tk3y.flashcards.common.scenes.ExportScene
import com.h0tk3y.flashcards.common.scenes.ImportScene
import com.h0tk3y.flashcards.common.scenes.LearnScene
import com.h0tk3y.flashcards.common.scenes.SingleCardListScene
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.rememberNavigator
import moe.tlaster.precompose.navigation.transition.NavTransition

sealed interface LoadState<out T> {
    object LOADING : LoadState<Nothing>
    object FAILED : LoadState<Nothing>
    object EMPTY : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
}

fun <T> LoadState<T>.valueOrNull() = when (this) {
    is LoadState.Loaded -> value
    else -> null
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(database: Database, applyTheme: @Composable (@Composable () -> Unit) -> Unit) {
    val navigator = rememberNavigator()
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        applyTheme {
            Surface {
                GlobalKeyboardShortcuts { event ->
                    event.key == Key.Escape && run { navigator.goBack(); true }
                }

                NavHost(
                    navigator = navigator, initialRoute = "/home",
                    navTransition = remember { navTransition() }
                ) {
                    scene("/home") {
                        val lists: LoadState<List<CardList>> by database.selectAllLists().collectAsState(LoadState.LOADING)
                        AllCardListsScene(database, lists) { navigator.navigate("/list/${it.id}") }
                    }
                    scene("/list/{listId:[0-9]+}") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        SingleCardListScene(
                            database,
                            listId,
                            onBack = { navigator.goBack() },
                            onRequestAddNewCard = { navigator.navigate("/list/$listId/newcard") },
                            onSelectCard = {
                                navigator.navigate("/list/$listId/editcard/${it.id}")
                            },
                            onDeleteList = {
                                database.deleteCardList(listId)
                                navigator.goBack()
                            },
                            onExport = {
                                navigator.navigate("/list/$listId/export")
                            },
                            onImport = {
                                navigator.navigate("/list/$listId/import")
                            },
                            onStartLearning = {
                                navigator.navigate("/list/$listId/learn")
                            })
                    }
                    scene("/list/{listId:[0-9]+}/newcard") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        EditCardDetailsScene(
                            database,
                            listId,
                            null,
                            { navigator.goBack() },
                            { card, list ->
                                database.createCard(card.unknownWord, card.knownWord, list, card.comment)
                                navigator.goBack()
                            },
                            onDelete = null
                        )
                    }
                    scene("/list/{listId:[0-9]+}/editcard/{cardId:[0-9]+}") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        val cardId = backStackEntry.pathMap["cardId"]?.toLong() ?: throw IllegalStateException()
                        EditCardDetailsScene(
                            database,
                            listId,
                            cardId,
                            { navigator.goBack() },
                            onApply = { card, _ ->
                                database.updateCard(card.id, card.unknownWord, card.knownWord, card.comment, card.modified)
                                navigator.goBack()
                            },
                            onDelete = {
                                database.deleteCard(cardId)
                                navigator.goBack()
                            }
                        )
                    }
                    scene("/list/{listId:[0-9]+}/learn") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        LearnScene(database, onBack = { navigator.goBack() }, listId = listId)
                    }
                    scene("/list/{listId:[0-9]+}/export") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        ExportScene(listId, database, onDismiss = { navigator.goBack() })
                    }
                    scene("/list/{listId:[0-9]+}/import") { backStackEntry ->
                        val listId = backStackEntry.pathMap["listId"]?.toLong() ?: throw IllegalStateException()
                        ImportScene(listId, database, onDismiss = { navigator.goBack() })
                    }
                }
            }
        }
    }
}

private fun navTransition(): NavTransition {
//    val fIn = fadeIn(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow), 0.3f)
//    val fOut = fadeOut(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow), 0.3f)
    val fIn = fadeIn()
    val fOut = fadeOut()
    return NavTransition(
        createTransition = fIn,
        destroyTransition = fOut,
        pauseTransition = fOut,
        resumeTransition = fIn
    )
}
