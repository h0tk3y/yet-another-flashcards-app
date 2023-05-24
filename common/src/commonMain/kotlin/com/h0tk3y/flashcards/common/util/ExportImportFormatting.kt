package com.h0tk3y.flashcards.common.util

import com.h0tk3y.flashcards.common.model.Flashcard

fun exportCardLine(card: Flashcard) =
    "${escape(card.unknownWord)} - ${escape(card.knownWord)}${card.comment?.let { " / " + escape(it) }.orEmpty()}"

sealed interface ParseResult {
    data class Success(val unknownWord: String, val knownWord: String, val comment: String?) : ParseResult
    data class Empty(val lineNumber: Int)
    data class Failure(val lineNumber: Int) : ParseResult
}

fun parseFlashcardLine(lines: String): List<Any> {
    return lines.lines().mapIndexed { index, line ->
        val lineNumber = index + 1

        if (line.isBlank()) {
            return@mapIndexed ParseResult.Empty(lineNumber)
        }

        val bodyAndComment = line.split(slashDelimiter)
        if (bodyAndComment.size !in 1..2) {
            ParseResult.Failure(lineNumber)
        } else {
            val comment = bodyAndComment.getOrNull(1)?.let(::unescape)
            val unknownAndKnown = bodyAndComment[0].split(dashDelimiter)
            if (unknownAndKnown.size != 2 || unknownAndKnown.any { it.isBlank() }) {
                ParseResult.Failure(lineNumber)
            } else {
                val unknown = unescape(unknownAndKnown[0])
                val known = unescape(unknownAndKnown[1])
                ParseResult.Success(unknown, known, comment)
            }
        }
    }
}

private const val dashEscape = "-"
private const val slashEscape = "/"
private const val slashDelimiter = " $slashEscape "
private const val dashDelimiter = " $dashEscape "

private fun escape(string: String) = string
    .replace(dashEscape, "\\$dashEscape")
    .replace(slashEscape, "\\$slashEscape")
    .replace("\n", "\\n")

private fun unescape(string: String) = string
    .replace("\\n", "\n")
    .replace("\\$slashEscape", slashEscape)
    .replace("\\$dashEscape", dashEscape)
