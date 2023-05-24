package com.h0tk3y.flashcards.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

@Composable fun CenterTextBox(text: String) {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
expect fun DialogOnTop(onDismiss: () -> Unit, title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit)

