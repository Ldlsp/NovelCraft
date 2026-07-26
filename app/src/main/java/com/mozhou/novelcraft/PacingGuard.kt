package com.mozhou.novelcraft

object PacingGuard {
    private val closureTerms = listOf("大结局", "一切结束", "最终胜利", "尘埃落定", "从此以后")

    fun inspect(project: NovelProject?, chapter: Chapter?): List<QualityIssue> {
        if (project == null || chapter == null) return emptyList()
        val issues = mutableListOf<QualityIssue>()
        val visibleCount = chapter.content.count { !it.isWhitespace() }
        if (chapter.targetWordCount > 0 && visibleCount < chapter.targetWordCount * 0.65) {
            issues += QualityIssue(QualitySeverity.WARNING, "未达到本章节奏目标", "当前 $visibleCount 字，低于计划 ${chapter.targetWordCount} 字的 65%。")
        }
        if (project.targetChapterCount >= 8 && chapter.number < project.targetChapterCount * 0.7) {
            closureTerms.firstOrNull { chapter.content.contains(it) }?.let { term ->
                issues += QualityIssue(QualitySeverity.WARNING, "可能过早收束主线", "第${chapter.number}章出现“$term”，目标为${project.targetChapterCount}章，请确认不是提前解决终局冲突。")
            }
        }
        return issues
    }
}
