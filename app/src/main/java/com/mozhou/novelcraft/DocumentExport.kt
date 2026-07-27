package com.mozhou.novelcraft

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentExport {
    fun markdown(project: NovelProject, chapters: List<Chapter>, items: List<StoryItem>, edges: List<StoryEdge>, notes: List<ResearchNote>) = buildString {
        appendLine("# ${project.title}")
        if (project.genre.isNotBlank()) appendLine("题材：${project.genre}")
        if (project.summary.isNotBlank()) appendLine("简介：${project.summary}")
        appendLine()
        chapters.forEach { chapter ->
            appendLine("## 第${chapter.number}章 ${chapter.title}")
            appendLine()
            appendLine(chapter.content.trim())
            appendLine()
        }
        appendLine("## 资料来源")
        appendReferences(notes)
        appendLine()
        appendLine("## 知识图谱")
        appendLine("```mermaid")
        appendLine(StoryGraphExport.asMermaid(items, edges))
        appendLine("```")
    }

    fun writeDocx(output: OutputStream, project: NovelProject, chapters: List<Chapter>, notes: List<ResearchNote>) {
        ZipOutputStream(output).use { zip ->
            zip.text("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            zip.text("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            val body = buildString {
                append(paragraph(project.title, "Title"))
                project.genre.takeIf { it.isNotBlank() }?.let { append(paragraph("题材：$it")) }
                project.summary.takeIf { it.isNotBlank() }?.let { append(paragraph("简介：$it")) }
                chapters.forEach { chapter ->
                    append(paragraph("第${chapter.number}章 ${chapter.title}", "Heading1"))
                    chapter.content.lines().forEach { line -> append(paragraph(line)) }
                }
                if (notes.isNotEmpty()) {
                    append(paragraph("资料来源", "Heading1"))
                    notes.filter { it.sourceUrl.isNotBlank() }.forEach { note -> append(paragraph("${note.title}：${note.sourceUrl}")) }
                }
            }
            zip.text("word/document.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>""")
        }
    }

    fun writeEpub(output: OutputStream, project: NovelProject, chapters: List<Chapter>, notes: List<ResearchNote>) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype").apply { method = ZipEntry.STORED; size = "application/epub+zip".toByteArray().size.toLong(); compressedSize = size; crc = java.util.zip.CRC32().apply { update("application/epub+zip".toByteArray()) }.value })
            zip.write("application/epub+zip".toByteArray()); zip.closeEntry()
            zip.text("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            val hasReferences = notes.any { it.sourceUrl.isNotBlank() }
            val manifest = chapters.indices.joinToString("") { "<item id=\"c$it\" href=\"chapter${it + 1}.xhtml\" media-type=\"application/xhtml+xml\"/>" } + if (hasReferences) "<item id=\"references\" href=\"references.xhtml\" media-type=\"application/xhtml+xml\"/>" else ""
            val spine = chapters.indices.joinToString("") { "<itemref idref=\"c$it\"/>" } + if (hasReferences) "<itemref idref=\"references\"/>" else ""
            zip.text("OEBPS/content.opf", """<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="book">novelcraft-${project.id}</dc:identifier><dc:title>${xml(project.title)}</dc:title><dc:language>zh-CN</dc:language></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>$manifest</manifest><spine>$spine</spine></package>""")
            val nav = chapters.joinToString("") { "<li><a href=\"chapter${it.number}.xhtml\">第${it.number}章 ${xml(it.title)}</a></li>" }
            zip.text("OEBPS/nav.xhtml", xhtml(project.title, "<nav epub:type=\"toc\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><ol>$nav</ol></nav>"))
            chapters.forEach { chapter ->
                val content = chapter.content.lines().filter { it.isNotBlank() }.joinToString("") { "<p>${xml(it)}</p>" }
                zip.text("OEBPS/chapter${chapter.number}.xhtml", xhtml("第${chapter.number}章 ${chapter.title}", "<h1>第${chapter.number}章 ${xml(chapter.title)}</h1>$content"))
            }
            if (hasReferences) {
                val references = notes.filter { it.sourceUrl.isNotBlank() }.joinToString("") { "<li>${xml(it.title)}：<a href=\"${xml(it.sourceUrl)}\">${xml(it.sourceUrl)}</a></li>" }
                zip.text("OEBPS/references.xhtml", xhtml("资料来源", "<h1>资料来源</h1><ol>$references</ol>"))
            }
        }
    }

    fun writePdf(output: OutputStream, project: NovelProject, chapters: List<Chapter>, notes: List<ResearchNote>) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(24, 37, 54); textSize = 15f }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f
        fun newPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page!!.canvas
            y = 60f
            paint.textSize = 11f
            canvas!!.drawText(project.title, 48f, 34f, paint)
            canvas!!.drawText(pageNumber.toString(), 535f, 34f, paint)
        }
        fun line(text: String, size: Float = 15f) {
            if (page == null || y > 780f) newPage()
            paint.textSize = size
            val words = text.ifBlank { " " }
            var offset = 0
            while (offset < words.length) {
                if (y > 780f) newPage()
                val count = paint.breakText(words, offset, words.length, true, 499f, null).coerceAtLeast(1)
                canvas!!.drawText(words.substring(offset, offset + count), 48f, y, paint)
                offset += count; y += size * 1.7f
            }
        }
        newPage()
        line(project.title, 26f); y += 12f
        project.summary.takeIf { it.isNotBlank() }?.let { line(it, 14f); y += 10f }
        chapters.forEach { chapter -> line("第${chapter.number}章 ${chapter.title}", 20f); y += 8f; chapter.content.lines().forEach { line(it, 14f) }; y += 12f }
        if (notes.any { it.sourceUrl.isNotBlank() }) { line("资料来源", 20f); notes.filter { it.sourceUrl.isNotBlank() }.forEach { line("${it.title}：${it.sourceUrl}", 10f) } }
        page?.let(document::finishPage)
        document.writeTo(output); document.close()
    }

    private fun StringBuilder.appendReferences(notes: List<ResearchNote>) {
        val sourced = notes.filter { it.sourceUrl.isNotBlank() }
        if (sourced.isEmpty()) appendLine("无") else sourced.forEachIndexed { index, note -> appendLine("${index + 1}. ${note.title}：${note.sourceUrl}") }
    }
    private fun ZipOutputStream.text(name: String, content: String) { putNextEntry(ZipEntry(name)); write(content.toByteArray(Charsets.UTF_8)); closeEntry() }
    private fun paragraph(value: String, style: String? = null): String = "<w:p>${style?.let { "<w:pPr><w:pStyle w:val=\"$it\"/></w:pPr>" }.orEmpty()}<w:r><w:t xml:space=\"preserve\">${xml(value)}</w:t></w:r></w:p>"
    private fun xhtml(title: String, body: String): String = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>${xml(title)}</title><meta charset=\"UTF-8\"/></head><body>$body</body></html>"
    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
