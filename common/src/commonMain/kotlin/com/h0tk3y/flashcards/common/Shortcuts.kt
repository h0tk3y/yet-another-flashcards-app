package com.h0tk3y.flashcards.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.key.KeyEvent
import java.util.Stack

typealias KeyHandler = (KeyEvent) -> Boolean

fun KeyHandler(handler: KeyHandler) = handler

object KeyHandlers {
    private val keyHandlersStack =
        Stack<KeyboardShortcuts>().apply { push(KeyboardShortcuts(isLocal = false) { false }) }

    fun enterReplacing(keyboardShortcuts: KeyboardShortcuts) {
        keyHandlersStack.push(keyboardShortcuts)
    }

    fun leave(keyboardShortcuts: KeyboardShortcuts) {
        check(keyboardShortcuts in keyHandlersStack)
        keyHandlersStack.remove(keyboardShortcuts)
    }

    fun handler() = KeyHandler { event ->
        var foundLocal = false
        keyHandlersStack.asReversed().forEach {
            if (!it.isLocal || !foundLocal) {
                val result = it.handler(event)
                if (result)
                    return@KeyHandler true
            }
            if (it.isLocal) {
                foundLocal = true
            }
        }
        false
    }
}

@Composable
fun LocalKeyboardShortcuts(handler: KeyHandler) {
    val keyboardShortcuts = KeyboardShortcuts(isLocal = true, handler = handler)
    DisposableEffect(handler) {
        KeyHandlers.enterReplacing(keyboardShortcuts)
        onDispose {
            KeyHandlers.leave(keyboardShortcuts)
        }
    }
}

@Composable
fun GlobalKeyboardShortcuts(handler: KeyHandler) {
    val keyboardShortcuts = KeyboardShortcuts(isLocal = false, handler = handler)
    DisposableEffect(handler) {
        KeyHandlers.enterReplacing(keyboardShortcuts)
        onDispose {
            KeyHandlers.leave(keyboardShortcuts)
        }
    }
}

data class KeyboardShortcuts(
    val isLocal: Boolean,
    val handler: KeyHandler
)