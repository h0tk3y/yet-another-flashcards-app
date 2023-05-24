@file:OptIn(ExperimentalAnimationApi::class)

package com.h0tk3y.flashcards.common.scenes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.Timestamp
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.model.Flashcard
import com.h0tk3y.flashcards.common.util.exportCardLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExportScene(
    listId: Long,
    database: Database,
    onDismiss: () -> Unit
) {
    val list by database.selectCardsInList(CardList(id = listId, "", Timestamp(0))).collectAsState(LoadState.LOADING)
    when (val currentList = list) {
        is LoadState.LOADING -> CenterTextBox("Loading")
        is LoadState.Loaded -> ExportDialog(currentList) { onDismiss() }
        else -> CenterTextBox("Something went wrong")
    }
}

@Composable
private fun ExportDialog(
    currentCards: LoadState.Loaded<List<Flashcard>>,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxSize()) {
        val content = currentCards.value.joinToString("\n") { card ->
            exportCardLine(card)
        }
        var showDoneIcon by remember { mutableStateOf(false) }
        var job by remember { mutableStateOf<Job>(Job()) }

        val scope = rememberCoroutineScope()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp).defaultMinSize(400.dp)
        ) {
            Text(
                "Save or share the text below. You can import it into a list using \"Import\"",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(content))
                        job.cancel()
                        job = scope.launch {
                            showDoneIcon = true
                            delay(3000)
                            showDoneIcon = false
                        }
                    }, modifier = Modifier.weight(0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(showDoneIcon, transitionSpec = {
                            expandHorizontally(expandFrom = Alignment.Start) + fadeIn() with shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        }) { isShowingDoneIcon ->
                            if (isShowingDoneIcon) {
                                Icon(Icons.Default.Done, "Done")
                            }
                        }
                        Text("Copy to clipboard")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.weight(0.5f)) {
                    Text("Close")
                }
            }
            TextField(
                value = content,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

