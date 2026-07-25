package com.mozhou.novelcraft

data class ContextPacket(
    val relevantItems: List<StoryItem> = emptyList(),
    val relevantEdges: List<StoryEdge> = emptyList(),
    val relevantChapters: List<Chapter> = emptyList(),
    val activeAnchor: StoryAnchor? = null,
    val prompt: String = "",
)

/**
 * A deterministic, offline retrieval pass. It deliberately keeps the selection explainable:
 * named items score highest, then shared Chinese or Latin terms, followed by recent chapters.
 */
object ContextEngine {
    private val termPattern = Regex("[\\p{IsHan}]{2,}|[A-Za-z0-9_]{3,}")

    fun build(
        project: NovelProject,
        current: Chapter,
        chapters: List<Chapter>,
        storyItems: List<StoryItem>,
        anchors: List<StoryAnchor> = emptyList(),
        edges: List<StoryEdge> = emptyList(),
    ): ContextPacket {
        val query = listOf(project.title, project.premise, current.title, current.content.takeLast(1_600)).joinToString("\n")
        val queryTerms = terms(query)
        val relevantItems = storyItems
            .filter { it.status != StoryItemStatus.RESOLVED }
            .map { it to score(query, queryTerms, it.name + "\n" + it.detail) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }

        val retrievedChapters = chapters
            .filter { it.id != current.id && it.content.isNotBlank() }
            .map { it to score(query, queryTerms, it.title + "\n" + it.content.takeLast(900)) }
            .sortedWith(compareByDescending<Pair<Chapter, Int>> { it.second }.thenByDescending { it.first.number })
            .filter { it.second > 0 }
            .take(2)
            .map { it.first }
            .ifEmpty { chapters.filter { it.number < current.number && it.content.isNotBlank() }.takeLast(2) }

        val anchor = anchors.firstOrNull { current.number in it.startChapter..it.endChapter }
        val itemsById = storyItems.associateBy { it.id }
        val seedItemIds = relevantItems.map { it.id }.toSet()
        val relevantEdges = edges
            .filter { it.sourceItemId in seedItemIds || it.targetItemId in seedItemIds }
            .take(8)
        val edgeItemIds = relevantEdges.flatMap { listOf(it.sourceItemId, it.targetItemId) }.toSet()
        val packetItems = (relevantItems + edgeItemIds.mapNotNull(itemsById::get))
            .distinctBy { it.id }
        val packet = ContextPacket(packetItems, relevantEdges, retrievedChapters, anchor)
        return packet.copy(prompt = buildPrompt(project, current, packet))
    }

    private fun buildPrompt(project: NovelProject, current: Chapter, packet: ContextPacket): String = buildString {
        appendLine("作品：${project.title}")
        appendLine("题材：${project.genre}")
        if (project.premise.isNotBlank()) appendLine("核心设定：${project.premise}")
        if (project.styleGuide.isNotBlank()) appendLine("项目文风档案（必须遵守）：${project.styleGuide}")
        appendLine("当前章节：第${current.number}章 ${current.title}")
        if (current.outline.isNotBlank()) appendLine("本章计划：${current.outline}")
        if (current.beatSheet.isNotBlank()) appendLine("本章分镜（必须按顺序展开，不得跳过）：${current.beatSheet}")
        packet.activeAnchor?.let { anchor ->
            appendLine("当前大纲锚点：第${anchor.startChapter}-${anchor.endChapter}章 ${anchor.title}")
            appendLine("本段核心冲突：${anchor.coreConflict}")
            if (anchor.allowedPlot.isNotBlank()) appendLine("本章允许推进：${anchor.allowedPlot}")
            if (anchor.forbiddenReveals.isNotBlank()) appendLine("本章严禁揭露：${anchor.forbiddenReveals}")
            if (anchor.mandatoryTension.isNotBlank()) appendLine("章末必须保留：${anchor.mandatoryTension}")
        }
        appendLine("当前正文末尾：")
        appendLine(current.content.takeLast(2_200))
        if (packet.relevantItems.isNotEmpty()) {
            appendLine("必须优先遵守的本地设定：")
            packet.relevantItems.forEach { appendLine("- [${it.kind}] ${it.name}：${it.detail}") }
        }
        if (packet.relevantEdges.isNotEmpty()) {
            val namesById = packet.relevantItems.associateBy({ it.id }, { it.name })
            appendLine("相关人物与设定关系：")
            packet.relevantEdges.forEach { edge ->
                val source = namesById[edge.sourceItemId] ?: "资料#${edge.sourceItemId}"
                val target = namesById[edge.targetItemId] ?: "资料#${edge.targetItemId}"
                appendLine("- $source ${edge.relation} $target：${edge.description}")
            }
        }
        if (packet.relevantChapters.isNotEmpty()) {
            appendLine("相关已写章节摘录：")
            packet.relevantChapters.forEach {
                appendLine("- 第${it.number}章 ${it.title}：${it.content.takeLast(500)}")
            }
        }
        appendLine("续写要求：延续当前叙事视角和时态；先推进一个可见动作；不要复述设定；不提前揭露未解谜底；结尾保留具体、可继续写的钩子。")
    }

    private fun score(query: String, queryTerms: Set<String>, candidate: String): Int {
        if (candidate.isBlank()) return 0
        val normalized = candidate.lowercase()
        var result = 0
        queryTerms.forEach { term -> if (normalized.contains(term.lowercase())) result += term.length }
        val directNameBonus = terms(candidate).maxOfOrNull { term -> if (query.contains(term)) term.length * 2 else 0 } ?: 0
        return result + directNameBonus
    }

    private fun terms(text: String): Set<String> = termPattern.findAll(text)
        .map { it.value.trim() }
        .filter { it.length >= 2 }
        .toSet()
}
