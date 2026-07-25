package com.mozhou.novelcraft

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextAndQualityTest {
    private val project = NovelProject(id = 1, title = "雾港来信", genre = "悬疑", premise = "沈舟寻找失踪的姐姐")

    @Test
    fun retrievesNamedMemoryAndRelevantPreviousChapter() {
        val previous = Chapter(id = 2, projectId = 1, number = 1, title = "旧码头", content = "沈舟在旧码头发现姐姐留下的月契。")
        val current = Chapter(id = 3, projectId = 1, number = 2, title = "雨夜", content = "沈舟握紧月契，朝仓库的灯光走去。")
        val memory = StoryItem(id = 4, projectId = 1, kind = "人物", name = "沈舟", detail = "姐姐失踪后独自追查。")
        val resolved = StoryItem(id = 5, projectId = 1, kind = "伏笔", name = "月契", detail = "已经在上一章回收。", status = StoryItemStatus.RESOLVED)

        val packet = ContextEngine.build(project, current, listOf(previous, current), listOf(memory, resolved))

        assertEquals(memory, packet.relevantItems.single())
        assertEquals(previous, packet.relevantChapters.single())
        assertTrue(packet.prompt.contains("必须优先遵守的本地设定"))
        assertTrue(packet.prompt.contains("旧码头"))
    }

    @Test
    fun warnsWithoutBlockingForDuplicatePlaceholderAndForbiddenReveal() {
        val chapter = Chapter(
            projectId = 1,
            number = 1,
            title = "试写",
            content = "TODO\n\n这是一段重复的测试正文，需要足够长度才能被视为段落。\n\n这是一段重复的测试正文，需要足够长度才能被视为段落。",
        )
        val forbidden = StoryItem(projectId = 1, kind = "禁区", name = "真实身份", detail = "第五十章前不得揭露")

        val issues = QualityGate.inspect(chapter, listOf(forbidden))

        assertTrue(issues.any { it.title == "发现占位文本" })
        assertTrue(issues.any { it.title == "发现重复段落" })
        assertTrue(issues.all { it.severity != QualitySeverity.INFO || it.title != "本地检查通过" })
    }

    @Test
    fun extractsParagraphsFromDocxDocumentXml() {
        val xml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body><w:p><w:r><w:t>第1章 雨夜</w:t></w:r></w:p>
              <w:p><w:r><w:t>灯火映在水面。</w:t></w:r></w:p></w:body>
            </w:document>
        """.trimIndent()

        val text = DocumentTextExtractor.extractDocumentXml(xml)

        assertEquals("第1章 雨夜\n灯火映在水面。", text)
    }
}
