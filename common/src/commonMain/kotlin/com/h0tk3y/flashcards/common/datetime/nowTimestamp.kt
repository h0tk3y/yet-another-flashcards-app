package com.h0tk3y.flashcards.common.datetime

import kotlinx.datetime.Clock

fun nowTimestamp() = Clock.System.now().epochSeconds
