package com.h0tk3y.flashcards.common.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.Timestamp
import com.h0tk3y.flashcards.common.datetime.nowTimestamp
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.model.Flashcard
import com.h0tk3y.flashcards.common.util.ParseResult
import com.h0tk3y.flashcards.common.util.parseFlashcardLine

@Composable
fun ImportScene(
    listId: Long,
    database: Database,
    onDismiss: () -> Unit
) {
    val list by database.selectCardsInList(CardList(id = listId, "", Timestamp(0))).collectAsState(LoadState.LOADING)
    when (list) {
        is LoadState.LOADING -> CenterTextBox("Loading")
        is LoadState.Loaded -> ImportDialog(listId, database, onDismiss)
        else -> CenterTextBox("Something went wrong")
    }
}

sealed interface InsertResult {
    object Success : InsertResult
    data class Failures(val lineNumbers: List<Int>) : InsertResult
}

fun tryInsert(listId: Long, success: (List<Flashcard>) -> Unit, text: String): InsertResult {
    val result = parseFlashcardLine(text)
    val failureLines = result.filterIsInstance<ParseResult.Failure>().map { it.lineNumber }
    return if (failureLines.isNotEmpty()) {
        InsertResult.Failures(failureLines)
    } else {
        val cards =
            result.filterIsInstance<ParseResult.Success>()
                .map { Flashcard(-1, it.knownWord, it.unknownWord, Timestamp(nowTimestamp()), it.comment, listId) }
        success(cards)
        InsertResult.Success
    }
}

@Composable
fun ImportDialog(
    listId: Long,
    database: Database,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var text by rememberSaveable { mutableStateOf("") }
    var errorLines by rememberSaveable { mutableStateOf(listOf<Int>()) }
    fun tryInsert() {
        when (val result = tryInsert(listId, success = { database.insertAllCards(it, listId) }, text)) {
            is InsertResult.Failures -> errorLines = result.lineNumbers
            InsertResult.Success -> onDismiss()
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { Text("Import flashcards") },
            navigationIcon = {
                IconButton(onClick = { onDismiss() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = ::tryInsert, enabled = text.isNotBlank() && errorLines.isEmpty()) {
                    Icon(Icons.Default.Done, "Import")
                }
            }
        )
    }) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp).defaultMinSize(400.dp)
        ) {
            Text("Add one or more lines:")
            TextField(
                value = text,
                singleLine = false,
                visualTransformation = ErrorLinesTransformation(MaterialTheme.colors.error, errorLines),
                onValueChange = {
                    text = it
                    errorLines = when (val result = tryInsert(listId, { }, it)) {
                        is InsertResult.Success -> listOf()
                        is InsertResult.Failures -> result.lineNumbers
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth().focusRequester(focusRequester),
                placeholder = {
                    Text("word to learn - translation\nword to learn - translation / comment", modifier = Modifier.alpha(0.6f))
                }
            )

            if (errorLines.isNotEmpty()) {
                Text(text = errorLines.size.toString() + " incorrect line" + "s".takeIf { errorLines.size > 1 }.orEmpty(), color = MaterialTheme.colors.error)
            }
        }
    }
}

class ErrorLinesTransformation(private val errorColor: Color, private val errorLines: List<Int>) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(annotate(text.toString()), OffsetMapping.Identity)
    private fun annotate(text: String): AnnotatedString {
        val errorLinesSet = errorLines.toSet()
        return buildAnnotatedString {
            val lines = text.lines()
            lines.forEachIndexed { index, line ->
                if (index + 1 in errorLinesSet)
                    withStyle(SpanStyle(color = errorColor)) {
                        append(line)
                    }
                else append(line)
                if (index != lines.lastIndex)
                    append("\n")
            }
        }
    }
}