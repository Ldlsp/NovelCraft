package com.mozhou.novelcraft

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.kxml2.io.KXmlParser
import java.io.InputStream
import java.util.zip.ZipInputStream
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object DocumentTextExtractor {
    fun read(context: Context, uri: Uri): String {
        val name = uri.lastPathSegment.orEmpty().lowercase()
        return context.contentResolver.openInputStream(uri)?.use { input ->
            when {
                name.endsWith(".docx") -> readDocx(input)
                name.endsWith(".epub") -> readEpub(input)
                name.endsWith(".pdf") -> readPdf(context, input)
                else -> input.bufferedReader().use { it.readText() }
            }
        } ?: error("无法读取导入文件")
    }

    fun extractDocumentXml(xml: String): String {
        val parser = KXmlParser()
        parser.setInput(xml.reader())
        val output = StringBuilder()
        val paragraph = StringBuilder()
        var inTextRun = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "t" || parser.name.endsWith(":t")) inTextRun = true
                XmlPullParser.TEXT -> if (inTextRun) paragraph.append(parser.text)
                XmlPullParser.END_TAG -> when {
                    parser.name == "t" || parser.name.endsWith(":t") -> inTextRun = false
                    parser.name == "p" || parser.name.endsWith(":p") -> {
                        if (paragraph.isNotBlank()) output.appendLine(paragraph.toString())
                        paragraph.clear()
                    }
                }
            }
            parser.next()
        }
        return output.toString().trim()
    }

    private fun readDocx(input: InputStream): String {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") return extractDocumentXml(zip.bufferedReader().use { it.readText() })
                entry = zip.nextEntry
            }
        }
        error("DOCX 文件中没有找到正文")
    }

    /* Legacy ZIP-order EPUB reader kept out of compilation for reference.
        val sections = mutableListOf<String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xhtml") || entry.name.endsWith(".html") || entry.name.endsWith(".htm")) {
                    val html = zip.bufferedReader().use { it.readText() }
                    val text = html.replace(Regex("(?is)<script.*?</script>|<style.*?</style>|<[^>]+>"), " ")
                        .replace("&nbsp;", " ").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim()
                    if (text.length > 40) sections += text
                }
                entry = zip.nextEntry
            }
        }
        if (sections.isEmpty()) error("EPUB 中没有找到可读取正文")
        return sections.joinToString("\n\n")
    }

    */

    private fun readEpub(input: InputStream): String {
        // Archive order is not reading order. The OPF spine provides the author-defined XHTML sequence.
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        val opf = entries.entries.firstOrNull { it.key.lowercase().endsWith(".opf") }
        val orderedNames = opf?.let { (path, bytes) -> epubSpine(path, bytes.toString(Charsets.UTF_8)) }
            ?.mapNotNull { spinePath -> entries.keys.firstOrNull { it.equals(spinePath, ignoreCase = true) } }
            ?.takeIf { it.isNotEmpty() }
            ?: entries.keys.filter { it.isHtmlDocument() }
        val sections = orderedNames.mapNotNull { name ->
            entries[name]?.toString(Charsets.UTF_8)?.let(::htmlToText)?.takeIf { it.length > 40 }
        }
        if (sections.isEmpty()) error("EPUB contains no readable body text")
        return sections.joinToString("\n\n")
    }

    private fun String.isHtmlDocument(): Boolean = lowercase().let {
        it.endsWith(".xhtml") || it.endsWith(".html") || it.endsWith(".htm")
    }

    private fun htmlToText(html: String): String = html
        .replace(Regex("(?is)<script.*?</script>|<style.*?</style>|<[^>]+>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim()

    private fun epubSpine(opfPath: String, xml: String): List<String> {
        val manifest = mutableMapOf<String, String>()
        val spineIds = mutableListOf<String>()
        val parser = KXmlParser()
        parser.setInput(xml.reader())
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) when (parser.name.lowercase()) {
                "item" -> parser.getAttributeValue(null, "id")?.let { id ->
                    parser.getAttributeValue(null, "href")?.let { href -> manifest[id] = href }
                }
                "itemref" -> parser.getAttributeValue(null, "idref")?.let(spineIds::add)
            }
            parser.next()
        }
        val parent = opfPath.substringBeforeLast('/', "")
        return spineIds.mapNotNull(manifest::get).map { href ->
            normalizeEpubPath(if (parent.isBlank()) href else "$parent/$href")
        }.filter { it.isHtmlDocument() }
    }

    private fun normalizeEpubPath(path: String): String {
        val segments = mutableListOf<String>()
        path.replace('\\', '/').split('/').forEach { segment -> when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments += segment
        } }
        return segments.joinToString("/")
    }

    private fun readPdf(context: Context, input: InputStream): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(input).use { document ->
            val text = PDFTextStripper().getText(document).trim()
            if (text.length < 40) error("PDF 没有可提取文字，扫描版需要先 OCR")
            return text
        }
    }
}
