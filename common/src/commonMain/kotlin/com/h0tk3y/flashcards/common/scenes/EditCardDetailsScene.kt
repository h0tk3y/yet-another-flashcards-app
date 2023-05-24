package com.h0tk3y.flashcards.common.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.h0tk3y.flashcards.common.LocalKeyboardShortcuts
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.Timestamp
import com.h0tk3y.flashcards.common.datetime.nowTimestamp
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.model.Flashcard
import com.h0tk3y.flashcards.common.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@OptIn(ExperimentalComposeUiApi::class)
@Composable fun EditCardDetailsScene(
    database: Database,
    listId: Long,
    existingCardId: Long?,
    onBack: () -> Unit,
    onApply: (Flashcard, CardList) -> Unit,
    onDelete: (() -> Unit)?
) {
    val viewModel = viewModel(EditCardDetailsViewModel::class) {
        EditCardDetailsViewModel(
            database,
            listId,
            existingCardId,
            onBack,
            onApply,
            onDelete,
        )
    }

    val existingCard by viewModel.existingCard.collectAsState()
    val cardList by viewModel.cardList.collectAsState()

    val unknownWordValue = viewModel.unknownWordValue()
    val knownWordValue = viewModel.knownWordValue()
    val commentValue = viewModel.commentValue()

    val showErrors by viewModel.showErrors.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(existingCardId) {
        if (existingCardId == null) {
            focusRequester.requestFocus()
        }
    }

    LocalKeyboardShortcuts { event ->
        when {
            (event.isMetaPressed || event.isCtrlPressed) && event.key == Key.Enter -> viewModel.tryApply().let { true }
            event.isMetaPressed && event.key == Key.Backspace || event.isCtrlPressed && event.key == Key.Delete -> onDelete?.invoke()
                .let { true }

            event.key == Key.Escape -> onBack().let { true }
            else -> false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (existingCard) {
                        is LoadState.Loaded -> Text("Edit flashcard")
                        LoadState.EMPTY -> Text("Create flashcard")
                        else -> Unit
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    when (cardList) {
                        is LoadState.Loaded -> {
                            if (viewModel.canDelete) {
                                IconButton(onClick = viewModel::delete) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete card")
                                }
                            }

                            IconButton(onClick = { viewModel.tryApply() }) {
                                Icon(Icons.Rounded.Done, contentDescription = "Done")
                            }
                        }

                        LoadState.FAILED, LoadState.LOADING, LoadState.EMPTY -> Unit
                    }
                }
            )
        }
    ) {
        when (val currentCardList = cardList) {
            is LoadState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val list = currentCardList.value

                    val keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.None)

                    item {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            value = unknownWordValue,
                            singleLine = true,
                            onValueChange = { viewModel.unknownWord = it },
                            keyboardOptions = keyboardOptions,
                            isError = unknownWordValue.isBlank() && showErrors,
                            label = { Text("Word to learn") },
                        )
                    }

                    item {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = knownWordValue,
                            singleLine = true,
                            onValueChange = { viewModel.knownWord = it },
                            keyboardOptions = keyboardOptions,
                            isError = knownWordValue.isBlank() && showErrors,
                            label = { Text("Known translation") },
                        )
                    }

                    item {
                        TextField(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            value = commentValue,
                            onValueChange = {
                                viewModel.comment = it
                            },
                            placeholder = { Text("Comments or examples") },
                        )
                    }

                    item {
                        Text(if (existingCard is LoadState.Loaded) "In list: ${list.name}" else "Will be added to list: ${list.name}")
                    }
                }
            }

            LoadState.LOADING -> {
                CenterTextBox("Loading data...")
            }

            LoadState.FAILED -> {
                CenterTextBox("Could not prepare the list for creating the card")
            }

            LoadState.EMPTY -> {
                CenterTextBox("The list does not exist anymore")
            }
        }
    }
}

class EditCardDetailsViewModel(
    private val database: Database,
    private val listId: Long,
    private val existingCardId: Long?,
    private val onBack: () -> Unit,
    private val onApply: (Flashcard, CardList) -> Unit,
    private val onDelete: (() -> Unit)?
) : ViewModel() {
    private val _cardList: MutableStateFlow<LoadState<CardList>> = MutableStateFlow(LoadState.LOADING)
    val cardList = _cardList.asStateFlow()

    private val _existingCard: MutableStateFlow<LoadState<Flashcard>> = MutableStateFlow(LoadState.LOADING)
    val existingCard = _existingCard.asStateFlow()

    var unknownWord by mutableStateOf(null as String?)
    var knownWord by mutableStateOf(null as String?)
    var comment by mutableStateOf(null as String?)

    fun unknownWordValue(): String = unknownWord ?: existingCard.value.valueOrNull()?.unknownWord.orEmpty()
    fun knownWordValue(): String = knownWord ?: existingCard.value.valueOrNull()?.knownWord.orEmpty()
    fun commentValue(): String = comment ?: existingCard.value.valueOrNull()?.comment.orEmpty()

    init {
        viewModelScope.launch {
            database.selectListById(listId).collect { _cardList.value = it }
        }
        if (existingCardId != null) {
            viewModelScope.launch {
                database.getCardDetailsById(existingCardId).collect {
                    _existingCard.value = it
                }
            }
        } else {
            _existingCard.value = LoadState.EMPTY
        }
    }

    private val _showErrors = MutableStateFlow(false)
    val showErrors = _showErrors.asStateFlow()

    fun tryApply() {
        val uw = unknownWordValue()
        val kw = knownWordValue()

        if (uw.isNotBlank() && kw.isNotBlank()) {
            val cm = commentValue().takeIf { it.isNotEmpty() }
            onApply(
                Flashcard(existingCardId ?: -1, kw, uw, Timestamp(nowTimestamp()), cm, listId),
                checkNotNull(cardList.value.valueOrNull())
            )
            onBack()
        } else {
            _showErrors.value = true
        }
    }

    val canDelete: Boolean
        get() = existingCardId != null

    fun delete() {
        check(existingCardId != null && onDelete != null)
        onDelete.invoke()
        onBack()
    }
}