package com.gorilla.music.utils

import com.gorilla.music.data.model.LyricsEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsParserTest {

    private val lines = listOf(
        LyricsEntry(time = 11_901L, text = "First"),
        LyricsEntry(time = 16_478L, text = "Second"),
        LyricsEntry(time = 21_197L, text = "Third"),
    )

    @Test
    fun noLineIsActiveBeforeTheFirstTimestamp() {
        assertEquals(-1, LyricsParser.findCurrentLineIndex(lines, 2_000L))
        assertEquals(-1, LyricsParser.findCurrentLineIndex(lines, 11_900L))
    }

    @Test
    fun lineChangesAtItsExactTimestamp() {
        assertEquals(0, LyricsParser.findCurrentLineIndex(lines, 11_901L))
        assertEquals(0, LyricsParser.findCurrentLineIndex(lines, 16_477L))
        assertEquals(1, LyricsParser.findCurrentLineIndex(lines, 16_478L))
    }

    @Test
    fun duplicateTimestampsSelectTheLastMatchingLine() {
        val duplicateLines = listOf(
            LyricsEntry(time = 1_000L, text = "Lead"),
            LyricsEntry(time = 1_000L, text = "Background"),
            LyricsEntry(time = 2_000L, text = "Next"),
        )

        assertEquals(1, LyricsParser.findCurrentLineIndex(duplicateLines, 1_000L))
    }
}
