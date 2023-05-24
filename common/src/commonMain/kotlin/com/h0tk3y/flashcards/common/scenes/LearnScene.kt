@file:OptIn(ExperimentalComposeUiApi::class)

package com.h0tk3y.flashcards.common.scenes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h0tk3y.flashcards.common.CenterTextBox
import com.h0tk3y.flashcards.common.LoadState
import com.h0tk3y.flashcards.common.LocalKeyboardShortcuts
import com.h0tk3y.flashcards.common.datetime.nowTimestamp
import com.h0tk3y.flashcards.common.db.Database
import com.h0tk3y.flashcards.common.model.CardList
import com.h0tk3y.flashcards.common.model.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LearnScene(
    database: Database,
    listId: Long,
    onBack: () -> Unit,
    model: LearnViewModel = viewModel(LearnViewModel::class, listOf(listId)) { LearnViewModel(listId, database) }
) {
    val learnState by model.learnState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning ${learnState.learning?.list?.name.orEmpty()}") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AnimatedContent(learnState.stage, transitionSpec = { fadeIn() with fadeOut() }) { stage ->
                when (val ls = learnState.state(stage)) {
                    is NotReady -> CenterTextBox("Loading...")
                    is Done -> EndLearnView(ls, model, onBack)
                    is Learning -> LearnView(ls, model)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LearnView(
    state: Learning,
    model: LearnViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) f@{
        Text(
            when {
                state.isShowMode -> "Try to remember:"
                state.showingBack -> "Did you remember this?"
                else -> "Can you remember this?"
            }
        )

        AnimatedContent(state.card to state.showingBack, modifier = Modifier.fillMaxWidth(), transitionSpec = {
            cardAnimation(state.isPrevSuccess)
        }) { (card, back) ->
            Card(
                backgroundColor = MaterialTheme.colors.primaryVariant,
            ) {
                if (back) {
                    cardBack(card, state.isShowMode, state, model)
                } else {
                    cardFront(card, model)
                }
            }
        }
        Text("${state.currentIndex + 1} of ${state.total}")
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentScope<Pair<Flashcard, Boolean>>.cardAnimation(isPrevSuccess: Boolean) =
    if (initialState.first == targetState.first) {
        val animTime = 150
        val animIn = expandHorizontally(
            expandFrom = Alignment.CenterHorizontally,
            animationSpec = tween(animTime, animTime)
        ) { 0 }
            .plus(fadeIn(tween(animTime, animTime), 0f))
            .plus(slideInVertically(tween(animTime, animTime)) { -it / 16 })

        val animOut = shrinkHorizontally(
            shrinkTowards = Alignment.CenterHorizontally,
            animationSpec = tween(animTime)
        ) { 10 }
            .plus(fadeOut(tween(animTime), 0.2f))
            .plus(slideOutVertically(tween(animTime)) { -it / 16 })

        animIn.with(animOut).using(SizeTransform(clip = false))
    } else {
        (fadeIn(spring(stiffness = Spring.StiffnessLow)) + scaleIn(spring(stiffness = Spring.StiffnessLow)) with
            fadeOut(spring(stiffness = Spring.StiffnessLow)) + slideOut(spring(stiffness = Spring.StiffnessMediumLow)) {
            IntOffset(
                it.width * (if (isPrevSuccess) 1 else -1),
                0
            )
        }).using(SizeTransform(clip = false))
    }

@Composable
private fun EndLearnView(
    state: Done,
    model: LearnViewModel,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Done! You remember ${state.success} of ${state.total}", fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))

        Button(onClick = { model.restartWithAllCards() }, modifier = Modifier.fillMaxWidth()) {
            Text("Restart all")
        }

        if (state.failures > 0) {
            Text("Try to remember ${state.failures} more?")
            BoxWithConstraints {
                if (this.maxWidth > this.maxHeight) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        retryButtons(true, Modifier.weight(0.5f, true), model)
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        retryButtons(false, Modifier.fillMaxWidth(), model)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        TextButton(onClick = { onBack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Finish")
        }
    }
}

@Composable
fun retryButtons(arrow: Boolean, modifier: Modifier, model: LearnViewModel) {
    Button(onClick = { model.showFailures() }, modifier) {
        Text("Show them")
    }
    if (arrow) {
        Icon(Icons.Default.ArrowForward, null)
    }
    Button(onClick = { model.restartWithFailures() }, modifier) {
        Text("Restart them")
    }
}

@Composable
private fun cardFront(
    card: Flashcard,
    model: LearnViewModel
) {
    LocalKeyboardShortcuts {
        it.key == Key.Y && run { model.oneSuccess(); true } ||
            it.key == Key.N && run { model.oneFailure(); true } ||
            it.key == Key.S && run { model.oneShowBack(); true }
    }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.sizeIn(minWidth = 500.dp, minHeight = 200.dp).padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = card.knownWord, fontSize = 24.sp, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            Button(onClick = { model.oneFailure() }) {
                Text("No")
            }
            Button(onClick = { model.oneShowBack() }) {
                Text("Show")
            }
            Button(onClick = { model.oneSuccess() }) {
                Text("Yes")
            }
        }
    }
}

@Composable
private fun cardBack(card: Flashcard, isShowMode: Boolean, learning: Learning, model: LearnViewModel) {
    LocalKeyboardShortcuts {
        it.key == Key.Y && run { model.oneSuccess(); true } ||
            it.key == Key.N && run { model.oneFailure(); true }
    }
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.sizeIn(minWidth = 500.dp, minHeight = 200.dp).padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = card.knownWord, modifier = Modifier.alpha(0.6f), textAlign = TextAlign.Center)
            Text(text = card.unknownWord, fontSize = 24.sp, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            if (isShowMode) {
                Button(onClick = { model.oneFailure() }) {
                    val isLast = learning.currentIndex == learning.total - 1
                    Text(if (isLast) "Done" else "Next")
                }
            } else {
                Button(onClick = { model.oneFailure() }) {
                    Text("No")
                }
                Button(onClick = { model.oneSuccess() }) {
                    Text("Yes")
                }
            }
        }
    }
}

class LearnViewModel(private val listId: Long, private val database: Database) : ViewModel() {
    private var _seed = 0

    private var currentIndex = 0
    private var showingBackOne = false
    private var showingBackAll = false

    private val success = LinkedHashSet<Flashcard>()

    private val failures = LinkedHashSet<Flashcard>()

    private val _learnState = MutableStateFlow(CurrentLearnStates(NotReady, null, null, LearnStateStage.NOT_READY))
    val learnState = _learnState.asStateFlow()

    init {
        initSeed()
        viewModelScope.launch {
            database.selectListById(listId).collect {
                if (it is LoadState.Loaded) {
                    val loadedList = it.value
                    database.selectCardsInList(loadedList).collect { loadedCards ->
                        if (loadedCards is LoadState.Loaded) {
                            reloadList(loadedList, loadedCards.value)
                        }
                    }
                }
            }
        }
        updateState()
    }

    private fun initSeed() {
        _seed = nowTimestamp().toInt()
    }

    private fun reloadList(list: CardList, cards: List<Flashcard>) {
        this.list = list
        this.cards = cards
        this.currentRoundCards = cards.shuffled(Random(_seed))
        updateState()
    }

    private fun advance() {
        currentIndex++
        showingBackOne = showingBackAll
    }

    fun restartWithAllCards() {
        initSeed()
        success.clear()
        failures.clear()
        currentRoundCards = checkNotNull(cards).shuffled(Random(_seed))
        currentIndex = 0
        showingBackOne = false
        showingBackAll = false
        updateState()
    }

    fun restartWithFailures() {
        initSeed()
        currentRoundCards = failures.toList().shuffled(Random(_seed))
        currentIndex = 0
        showingBackOne = false
        showingBackAll = false
        updateState()
    }

    fun showFailures() {
        currentRoundCards = failures.toList()
        currentIndex = 0
        showingBackAll = true
        updateState()
    }

    fun oneSuccess() {
        _learnState.value.learning?.let { state ->
            failures -= state.card
            success += state.card
            advance()
            updateState()
        } ?: error("cannot mark the item as successful when not learning")
    }

    fun oneShowBack() {
        showingBackOne = true
        updateState()
    }

    fun oneFailure() {
        _learnState.value.learning?.let { state ->
            success -= state.card
            failures += state.card
            advance()
            updateState()
        } ?: error("cannot mark the item as successful when not learning")
    }

    private var list: CardList? = null

    private var cards: List<Flashcard>? = null
    private var currentRoundCards: List<Flashcard> = emptyList()

    private fun updateState() {
        val currentCards = cards
        _learnState.value = if (currentCards != null) {
            val inBounds = currentRoundCards.let { currentIndex in it.indices }

            if (inBounds) {
                val prev = currentRoundCards.getOrNull(currentIndex - 1)
                val isPrevSuccess = prev in success
                val learning = Learning(
                    checkNotNull(list),
                    currentRoundCards[currentIndex],
                    isPrevSuccess,
                    success.size,
                    failures.size,
                    currentIndex,
                    showingBackOne || showingBackAll,
                    showingBackAll,
                    currentRoundCards.size
                )
                _learnState.value.copy(learning = learning, stage = LearnStateStage.LEARNING)
            } else {
                val done = Done(checkNotNull(list), success.size, failures.size, currentCards.size)
                _learnState.value.copy(done = done, stage = LearnStateStage.DONE)
            }
        } else _learnState.value.copy(stage = LearnStateStage.NOT_READY)
    }
}

data class CurrentLearnStates(
    val notReady: NotReady?,
    val learning: Learning?,
    val done: Done?,
    val stage: LearnStateStage
) {
    fun state(stage: LearnStateStage): LearnState = when (stage) {
        LearnStateStage.NOT_READY -> checkNotNull(notReady)
        LearnStateStage.LEARNING -> checkNotNull(learning)
        LearnStateStage.DONE -> checkNotNull(done)
    }
}

enum class LearnStateStage {
    NOT_READY, LEARNING, DONE
}

sealed interface LearnState
object NotReady : LearnState

data class Learning(
    val list: CardList,
    val card: Flashcard,
    val isPrevSuccess: Boolean,
    val success: Int,
    val failures: Int,
    val currentIndex: Int,
    val showingBack: Boolean,
    val isShowMode: Boolean,
    val total: Int,
) : LearnState

data class Done(val list: CardList, val success: Int, val failures: Int, val total: Int) : LearnState
