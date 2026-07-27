package com.mozhou.novelcraft

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundGenerationPolicyTest {
    @Test
    fun keepsTheAppForegroundWhileAnyAiTaskIsActive() {
        assertFalse(shouldKeepGenerationForeground(emptySet()))
        assertTrue(shouldKeepGenerationForeground(setOf(GenerationTask.OPENING_CHAPTER)))
        assertTrue(shouldKeepGenerationForeground(setOf(GenerationTask.CONTINUATION, GenerationTask.CHAPTER_LIFECYCLE)))
    }
}
