package com.mozhou.novelcraft

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.kxml2.io.KXmlParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocumentTextExtractor {
    fun read(context: Context, uri: Uri): String {
        val name = uri.lastPathSegment.orEmpty().lowercase()
        return context.contentResolver.openInputStream(uri)?.use { input ->
            if (name.endsWith(".docx")) readDocx(input) else input.bufferedReader().use { it.readText() }
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
}
