package com.mozhou.novelcraft

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocumentExportTest {
    private val project = NovelProject(id = 7, title = "测试小说", genre = "仙侠", premise = "测试设定", summary = "测试简介")
    private val chapters = listOf(Chapter(projectId = 7, number = 1, title = "初入江湖", content = "第一段正文\n第二段正文"))
    private val notes = listOf(ResearchNote(projectId = 7, title = "修仙文化", sourceUrl = "https://example.org/source", content = "公开资料摘要"))

    @Test fun markdownIncludesCitationAndGraph() {
        val output = DocumentExport.markdown(project, chapters, emptyList(), emptyList(), notes)
        assertTrue(output.contains("第1章 初入江湖"))
        assertTrue(output.contains("https://example.org/source"))
        assertTrue(output.contains("知识图谱"))
    }

    @Test fun docxContainsRequiredPartsAndChineseContent() {
        val bytes = ByteArrayOutputStream().also { DocumentExport.writeDocx(it, project, chapters, notes) }.toByteArray()
        val files = unzip(bytes)
        assertTrue(files.containsKey("[Content_Types].xml"))
        assertTrue(files["word/document.xml"]!!.contains("测试小说"))
        assertTrue(files["word/document.xml"]!!.contains("https://example.org/source"))
    }

    @Test fun epubHasMimetypeNavigationChaptersAndReferences() {
        val bytes = ByteArrayOutputStream().also { DocumentExport.writeEpub(it, project, chapters, notes) }.toByteArray()
        val files = unzip(bytes)
        assertTrue(files["mimetype"] == "application/epub+zip")
        assertTrue(files.containsKey("OEBPS/nav.xhtml"))
        assertTrue(files["OEBPS/chapter1.xhtml"]!!.contains("第一段正文"))
        assertTrue(files["OEBPS/references.xhtml"]!!.contains("https://example.org/source"))
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry -> result[entry.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        return result
    }
}
