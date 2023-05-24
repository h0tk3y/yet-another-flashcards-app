package com.h0tk3y.flashcards.common

import androidx.compose.ui.graphics.Color

fun lightColors() = androidx.compose.material.lightColors(
    primary = Color(0xFF30a04e),
    primaryVariant = Color(0xFF15902e),
    secondary = Color(0xFFe9dd3d),
    secondaryVariant = Color(0xFFe9dd3d),
    background = Color(0xFFEEEEEE)
)

fun darkColors() = androidx.compose.material.darkColors(
    primary = Color(0xFF40803e),
    primaryVariant = Color(0xFF103017),
    onPrimary = Color.White,
    surface = Color(0xFF042003),
    secondary = Color(0xFFa58e1c)
)