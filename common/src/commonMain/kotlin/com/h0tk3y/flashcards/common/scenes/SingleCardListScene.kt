@file:OptIn(
    ExperimentalComposeUiApi::class
)

package com.h0tk3y.flashcards.common.scenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.LocalKeyboardShortcuts
import com.h0tk3y.flashcards.common.Timestamp
import com.h0tk3y.flashcards.common.datetime.nowTimestamp
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.db.ItemSaveResult
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.model.Flashcard
import com.h0tk3y.flashcards.common.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@ExperimentalComposeUiApi
@Composable
fun SingleCardListScene(
    database: Database,
    cardListId: Long,
    onBack: () -> Unit,
    onRequestAddNewCard: () -> Unit,
    onSelectCard: (Flashcard) -> Unit,
    onDeleteList: () -> Unit,
    onStartLearning: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val model = viewModel(SingleCardListViewModel::class, listOf(cardListId)) { SingleCardListViewModel(cardListId, database) }
    val cardList by model.list.collectAsState()
    val cards by model.cardList.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }

    LocalKeyboardShortcuts {
        it.key == Key.N && (it.isCtrlPressed || it.isMetaPressed) && run { onRequestAddNewCard(); true } ||
            it.key == Key.L && (it.isCtrlPressed || it.isMetaPressed) && run { onStartLearning(); true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((cardList as? LoadState.Loaded)?.value?.name ?: "...") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onRequestAddNewCard() }) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create card")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Show menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onDeleteList()
                        }) {
                            Icon(Icons.Default.Delete, "Delete list")
                            Spacer(Modifier.width(8.dp))
                            Text("Delete this list")
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Rename list")
                            Spacer(Modifier.width(8.dp))
                            Text("Rename this list")
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onExport()
                        }) {
                            Icon(Icons.Default.Send, "Export")
                            Spacer(Modifier.width(8.dp))
                            Text("Export")
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onImport()
                        }) {
                            Icon(Icons.Default.AddCircle, "Import")
                            Spacer(Modifier.width(8.dp))
                            Text("Import")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (cards.valueOrNull()?.isNotEmpty() == true) {
                FloatingActionButton(onClick = { onStartLearning() }) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start learning")
                }
            }
        }
    ) {
        val currentCardList = cardList
        if (currentCardList is LoadState.Loaded) {
            when (val currentCards = cards) {
                is LoadState.Loaded -> {
                    if (currentCards.value.isNotEmpty()) {
                        CardListItems(currentCards, onSelectCard)
                    } else {
                        CenterTextBox("This list is empty. Create some cards in it!")
                    }

                    if (showRenameDialog) {
                        CreateOrEditListDialog(currentCardList.value, onDismiss = { showRenameDialog = false }, onSave = {
                            database.updateList(cardListId, it, Timestamp(nowTimestamp()))
                            showRenameDialog = false
                            ItemSaveResult.Ok(currentCardList.value)
                        })
                    }
                }

                LoadState.FAILED -> CenterTextBox("Something went wrong, and this list cannot be displayed.")
                LoadState.LOADING -> CenterTextBox("Loading the cards...")
                LoadState.EMPTY -> CenterTextBox("This list no longer exists")
            }
        }
    }
}

@Composable
private fun CardListItems(
    currentCards: LoadState.Loaded<List<Flashcard>>,
    onSelectCard: (Flashcard) -> Unit
) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(currentCards.value, key = Flashcard::id) { card ->
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onSelectCard(card) }) {
                    val hasComment = card.comment != null
                    CardItemView(card, hasComment)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun BoxScope.CardItemView(card: Flashcard, hasComment: Boolean) {
    Text(
        "${card.unknownWord} - ${card.knownWord}",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(8.dp, 8.dp, if (hasComment) 40.dp else 8.dp, 8.dp)
            .align(Alignment.CenterStart)
    )
    if (hasComment) {
        Icon(
            imageVector = Icons.Rounded.Info,
            tint = Color.Gray,
            contentDescription = "Has a comment",
            modifier = Modifier.Companion.align(Alignment.CenterEnd).padding(end = 8.dp).alpha(0.5f)
        )
    }
}

class SingleCardListViewModel(private val listId: Long, private val database: Database) : ViewModel() {
    private val _list: MutableStateFlow<LoadState<CardList>> = MutableStateFlow(LoadState.LOADING)
    private val _cardList: MutableStateFlow<LoadState<List<Flashcard>>> = MutableStateFlow(LoadState.LOADING)

    val list = _list.asStateFlow()
    val cardList = _cardList.asStateFlow()

    init {
        viewModelScope.launch {
            database.selectListById(listId).collect {
                _list.value = it
                if (it is LoadState.Loaded) {
                    viewModelScope.launch {
                        database.selectCardsInList(it.value).collect {
                            _cardList.value = it
                        }
                    }
                }
            }
        }
    }
}
