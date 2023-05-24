package com.h0tk3y.flashcards.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable actual fun DialogOnTop(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    AlertDialog(backgroundColor = MaterialTheme.colors.surface, onDismissRequest = onDismiss, buttons = {
        Card {
            Column {
                Text(title, modifier = Modifier.padding(16.dp), fontSize = 18.sp)
                content()
            }
        }
    })
}