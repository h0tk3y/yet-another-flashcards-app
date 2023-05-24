package com.h0tk3y.flashcards.common.scenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.h0tk3y.flashcards.common.LocalKeyboardShortcuts
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.DialogOnTop
import com.h0tk3y.flashcards.common.KeyHandler
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.db.ItemSaveResult
import com.h0tk3y.flashcards.common.model.CardList

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AllCardListsScene(
    database: Database,
    lists: LoadState<List<CardList>>,
    onListSelected: (CardList) -> Unit
) {
    var showNewListDialog by rememberSaveable { mutableStateOf(false) }

    LocalKeyboardShortcuts {
        it.key == Key.N && (it.isCtrlPressed || it.isMetaPressed) && run { showNewListDialog = true; true }
    }

    if (showNewListDialog) {
        CreateOrEditListDialog(existing = null, onDismiss = { showNewListDialog = false }, onSave = {
            val result = database.createList(it)
            if (result is ItemSaveResult.Ok) {
                onListSelected(result.createdItem)
                showNewListDialog = false
            }
            result
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("h0tk3y's Flashcards") },
                actions = {
                    IconButton(onClick = { showNewListDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create list")
                    }
                }
            )
        },
        content = {
            Box(Modifier.fillMaxSize()) {
                when (lists) {
                    is LoadState.Loaded -> {
                        CardListsListView(lists.value, onListSelected)
                    }

                    LoadState.LOADING -> CenterTextBox("Loading lists...")
                    LoadState.EMPTY -> CenterTextBox("This list no longer exists")
                    LoadState.FAILED -> CenterTextBox("Something went wrong")
                }
            }
        }
    )
}

@ExperimentalComposeUiApi
@Composable
fun CreateOrEditListDialog(
    existing: CardList?,
    onDismiss: () -> Unit,
    onSave: (String) -> ItemSaveResult<CardList>
) {
    var itemName by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var isErrorEmptyName by rememberSaveable { mutableStateOf(false) }
    var itemAlreadyExists by rememberSaveable { mutableStateOf(false) }

    fun trySave() {
        if (itemName.isBlank()) {
            isErrorEmptyName = true
        } else {
            val result = onSave(itemName)
            itemAlreadyExists = result == ItemSaveResult.ALREADY_EXISTS
        }
    }

    val keys = KeyHandler {
        it.key == Key.Enter && run { trySave(); true } ||
            it.key == Key.Escape && run { onDismiss(); true }
    }
    LocalKeyboardShortcuts(handler = keys)

    DialogOnTop(onDismiss, title = "Create a new card list", modifier = Modifier.onPreviewKeyEvent(keys)) {
        val focusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = itemName,
                modifier = Modifier.focusRequester(focusRequester).onPreviewKeyEvent(keys),
                singleLine = true,
                onValueChange = {
                    itemAlreadyExists = false
                    isErrorEmptyName = false
                    itemName = it
                },
                isError = itemAlreadyExists || isErrorEmptyName,
                label = { Text("List name") },
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Box(Modifier.height(24.dp)) {
                if (itemAlreadyExists) {
                    Text(text = "There is already a list with this name", color = MaterialTheme.colors.error)
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        trySave()
                    },
                    enabled = itemName.isNotBlank() && !itemAlreadyExists
                ) {
                    Text("Save")
                }
            }

        }
    }
}

@Composable
fun CardListsListView(
    lists: List<CardList>,
    onListSelected: (CardList) -> Unit
) {
    if (lists.isNotEmpty()) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(lists) { cardList ->
                Box(Modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .clickable { onListSelected(cardList) }
                ) {
                    Text(
                        cardList.name,
                        modifier = Modifier.padding(8.dp).align(Alignment.CenterStart)
                    )
                }
                Divider()
            }
        }
    } else {
        CenterTextBox("There is no card lists yet")
    }
}

