package com.h0tk3y.flashcards.android

import com.h0tk3y.flashcards.common.App
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.h0tk3y.flashcards.common.KeyHandlers
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.db.DatabaseDriverFactory
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent

class MainActivity : PreComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Database(DatabaseDriverFactory(this))
        setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.surface)
                        .systemBarsPadding().imePadding()
                ) {
                    App(database, applyTheme = {
                        val systemUiController = rememberSystemUiController()
                        systemUiController.setNavigationBarColor(
                            color = MaterialTheme.colors.primaryVariant
                        )
                        systemUiController.setSystemBarsColor(
                            color = MaterialTheme.colors.primaryVariant
                        )

                        it()
                    })
                }
            }
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event != null) {
            KeyHandlers.handler().invoke(androidx.compose.ui.input.key.KeyEvent(event))
        } else false
    }
}