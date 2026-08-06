package com.mozhou.novelcraft

import org.junit.Assert.assertEquals
import org.junit.Test

class NovelBodyPolicyTest {
    @Test
    fun stripsMarkdownMarkersFromGeneratedNovelBody() {
        val raw = "# 雨夜\n> 雨水顺着屋檐滴落。\n\n```markdown\n> 她没有回头。\n```"

        assertEquals("雨夜\n雨水顺着屋檐滴落。\n\n她没有回头。", sanitizeNovelBody(raw))
    }

    @Test
    fun defaultsAndClampsTheChapterWordRange() {
        assertEquals(ChapterWordRange(2_000, 5_000), normalizeChapterWordRange(0, 0))
        assertEquals(ChapterWordRange(2_000, 2_000), normalizeChapterWordRange(1_500, 1_800))
        assertEquals(ChapterWordRange(4_000, 6_000), normalizeChapterWordRange(4_000, 6_000))
    }
}
