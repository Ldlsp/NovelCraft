package com.mozhou.novelcraft

data class ImportedChapter(
    val number: Int,
    val title: String,
    val content: String,
)

object ChapterImporter {
    private val chapterHeading = Regex("""^\s*第\s*(\d+)\s*章\s*([^\n]*)\s*$""", RegexOption.MULTILINE)

    fun parse(source: String): List<ImportedChapter> {
        val matches = chapterHeading.findAll(source).toList()
        if (matches.isEmpty()) {
            return listOf(ImportedChapter(1, "导入正文", source.trim()))
        }

        return matches.mapIndexed { index, match ->
            val chapterNumber = match.groupValues[1].toInt()
            val title = match.groupValues[2].trim().ifBlank { "第" + chapterNumber + "章" }
            val bodyStart = match.range.last + 1
            val bodyEnd = matches.getOrNull(index + 1)?.range?.first ?: source.length
            ImportedChapter(chapterNumber, title, source.substring(bodyStart, bodyEnd).trim())
        }
    }
}
