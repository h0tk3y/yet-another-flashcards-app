package com.h0tk3y.flashcards.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyScreen() {
    Column {
        // Outer content state
        var outerContentState by remember { mutableStateOf(1) }

        // Inner content state
        var innerContentState by remember { mutableStateOf(2) }

        Box(Modifier.size(240.dp)) {
            // Animated outer view
            AnimatedContent(
                targetState = outerContentState,
                transitionSpec = {
                    // Define animations for the outer view
                    fadeIn() with fadeOut()
                }
            ) { targetState ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Green)
                ) {
                    // Outer views
                    Text(
                        text = "$targetState",
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp).align(Alignment.BottomStart)
                    )

                    // Animated inner view
                    AnimatedContent(
                        targetState = innerContentState,
                        transitionSpec = {
                            // Define animations for the inner view
                            slideInHorizontally() with slideOutHorizontally()
                        }
                    ) { innerTargetState ->
                        Box(
                            Modifier
                                .size(100.dp)
                                .background(Color.Blue)
                                .padding(16.dp)
                        ) {
                            // Inner views
                            Text(
                                text = "$innerTargetState",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Update state when needed
        Button(onClick = { outerContentState = outerContentState + 1 }) {
            Text("Change Outer Content")
        }

        Button(onClick = { innerContentState = innerContentState + 1 }) {
            Text("Change Inner Content")
        }
    }
}