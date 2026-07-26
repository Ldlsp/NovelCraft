package com.mozhou.novelcraft

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File

class NovelRepository(private val database: NovelDatabase) {
    fun projects(): Flow<List<NovelProject>> = database.projectDao().observeAll()
    fun project(projectId: Long): Flow<NovelProject?> = database.projectDao().observe(projectId)
    fun chapters(projectId: Long): Flow<List<Chapter>> = database.chapterDao().observeByProject(projectId)
    fun latestRevision(chapterId: Long): Flow<ChapterRevision?> = database.chapterRevisionDao().observeLatest(chapterId)
    fun resumableAutoWriteRun(projectId: Long): Flow<AutoWriteRun?> = database.autoWriteRunDao().observeResumable(projectId)
    fun storyItems(projectId: Long): Flow<List<StoryItem>> = database.storyItemDao().observeByProject(projectId)
    fun chapterMentions(projectId: Long): Flow<List<ChapterStoryMention>> = database.chapterStoryMentionDao().observeByProject(projectId)
    fun researchNotes(projectId: Long): Flow<List<ResearchNote>> = database.researchNoteDao().observeByProject(projectId)
    fun latestEditorialReview(chapterId: Long): Flow<EditorialReview?> = database.editorialReviewDao().observeLatest(chapterId)
    fun anchors(projectId: Long): Flow<List<StoryAnchor>> = database.storyAnchorDao().observeByProject(projectId)
    fun edges(projectId: Long): Flow<List<StoryEdge>> = database.storyEdgeDao().observeByProject(projectId)

    suspend fun createProject(title: String, genre: String, premise: String): Long {
        val projectId = database.projectDao().insert(
            NovelProject(title = title.trim(), genre = genre.trim(), premise = premise.trim()),
        )
        database.chapterDao().insert(Chapter(projectId = projectId, number = 1, title = "开篇"))
        return projectId
    }

    suspend fun importProject(context: Context, uri: Uri, title: String): Long {
        val text = DocumentTextExtractor.read(context, uri)
        return database.withTransaction {
            val projectId = database.projectDao().insert(
                NovelProject(title = title.ifBlank { "导入作品" }, genre = "待分类", premise = "从已有正文导入"),
            )
            database.chapterDao().insertAll(
                ChapterImporter.parse(text).map {
                    Chapter(projectId = projectId, number = it.number, title = it.title, content = it.content)
                },
            )
            projectId
        }
    }

    suspend fun updateChapter(chapter: Chapter, content: String) {
        database.withTransaction {
            val timestamp = System.currentTimeMillis()
            database.chapterDao().update(chapter.copy(content = content, updatedAt = timestamp))
            database.projectDao().touch(chapter.projectId, timestamp)
        }
    }

    suspend fun updateProjectStyle(project: NovelProject, styleGuide: String) {
        database.projectDao().update(project.copy(styleGuide = styleGuide.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateLongFormBlueprint(project: NovelProject, blueprint: String) {
        database.projectDao().update(project.copy(longFormBlueprint = blueprint.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun updatePacing(project: NovelProject, targetChapters: Int, targetWords: Int, profile: String) {
        database.projectDao().update(
            project.copy(
                targetChapterCount = targetChapters.coerceAtLeast(0),
                targetWordCount = targetWords.coerceAtLeast(0),
                pacingProfile = profile.trim().ifBlank { "均衡" },
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateProjectProfile(
        project: NovelProject,
        title: String,
        genre: String,
        premise: String,
        summary: String,
        tags: String,
        targetAudience: String,
        protagonistName: String,
    ) {
        database.projectDao().update(
            project.copy(
                title = title.trim().ifBlank { project.title },
                genre = genre.trim().ifBlank { "待分类" },
                premise = premise.trim(),
                summary = summary.trim(),
                tags = tags.trim(),
                targetAudience = targetAudience.trim(),
                protagonistName = protagonistName.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateCover(project: NovelProject, coverPath: String) {
        database.projectDao().update(project.copy(coverPath = coverPath, updatedAt = System.currentTimeMillis()))
        if (project.coverPath.isNotBlank() && project.coverPath != coverPath) File(project.coverPath).delete()
    }

    suspend fun deleteProject(project: NovelProject) {
        database.projectDao().deleteById(project.id)
        if (project.coverPath.isNotBlank()) File(project.coverPath).delete()
    }

    suspend fun applyOutlineCascade(project: NovelProject, report: OutlineCascadeReport, items: List<StoryItem>, anchors: List<StoryAnchor>, edges: List<StoryEdge>) = database.withTransaction {
        database.storyItemDao().updateAll(items.filter { it.id in report.affectedItemIds }.map { it.copy(cascadePending = true) })
        database.storyAnchorDao().updateAll(anchors.filter { it.id in report.affectedAnchorIds }.map { it.copy(cascadePending = true) })
        database.storyEdgeDao().updateAll(edges.filter { it.id in report.affectedEdgeIds }.map { it.copy(cascadePending = true) })
        database.projectDao().update(project.copy(outlineRevisionReport = report.summary, updatedAt = System.currentTimeMillis()))
    }

    suspend fun resolveOutlineCascade(project: NovelProject, items: List<StoryItem>, anchors: List<StoryAnchor>, edges: List<StoryEdge>) = database.withTransaction {
        database.storyItemDao().updateAll(items.filter { it.cascadePending }.map { it.copy(cascadePending = false) })
        database.storyAnchorDao().updateAll(anchors.filter { it.cascadePending }.map { it.copy(cascadePending = false) })
        database.storyEdgeDao().updateAll(edges.filter { it.cascadePending }.map { it.copy(cascadePending = false) })
        database.projectDao().update(project.copy(outlineRevisionReport = "", updatedAt = System.currentTimeMillis()))
    }

    suspend fun renameChapter(chapter: Chapter, title: String) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(chapter.copy(title = title.trim().ifBlank { "第${chapter.number}章" }, updatedAt = timestamp))
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun updateChapterPlan(chapter: Chapter, outline: String, targetWordCount: Int) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(
            chapter.copy(outline = outline.trim(), targetWordCount = targetWordCount.coerceAtLeast(0), updatedAt = timestamp),
        )
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun updateChapterBeatSheet(chapter: Chapter, beatSheet: String) {
        val timestamp = System.currentTimeMillis()
        database.chapterDao().update(chapter.copy(beatSheet = beatSheet.trim(), updatedAt = timestamp))
        database.projectDao().touch(chapter.projectId, timestamp)
    }

    suspend fun addChapter(projectId: Long): Long {
        val number = (database.chapterDao().maxNumber(projectId) ?: 0) + 1
        val timestamp = System.currentTimeMillis()
        val id = database.chapterDao().insert(Chapter(projectId = projectId, number = number, title = "第${number}章", updatedAt = timestamp))
        database.projectDao().touch(projectId, timestamp)
        return id
    }

    suspend fun deleteChapter(chapter: Chapter) {
        require(database.chapterDao().countByProject(chapter.projectId) > 1) { "至少保留一章" }
        database.withTransaction {
            database.chapterDao().deleteById(chapter.id)
            database.projectDao().touch(chapter.projectId, System.currentTimeMillis())
        }
    }

    suspend fun addGeneratedDraftChapter(projectId: Long, content: String, autoWriteRunId: Long = 0): Chapter = database.withTransaction {
        val number = (database.chapterDao().maxNumber(projectId) ?: 0) + 1
        val timestamp = System.currentTimeMillis()
        val chapter = Chapter(
            projectId = projectId,
            number = number,
            title = "第${number}章",
            content = content.trim(),
            lifecycleStatus = ChapterLifecycleStatus.PROCESSING,
            lifecycleDetail = "AI 正在同步本章记忆与门禁结果",
            autoWriteRunId = autoWriteRunId,
            updatedAt = timestamp,
        )
        val id = database.chapterDao().insert(chapter)
        database.projectDao().touch(projectId, timestamp)
        chapter.copy(id = id)
    }

    suspend fun updateChapterLifecycle(
        chapter: Chapter,
        lifecycleStatus: String,
        lifecycleDetail: String = "",
        qualityStatus: String = chapter.qualityStatus,
        qualityIssueSummary: String = chapter.qualityIssueSummary,
        memoryUpdatedAt: Long = chapter.memoryUpdatedAt,
    ) {
        val timestamp = System.currentTimeMillis()
        database.withTransaction {
            val current = database.chapterDao().findById(chapter.id) ?: return@withTransaction
            database.chapterDao().update(
                current.copy(
                    lifecycleStatus = lifecycleStatus,
                    lifecycleDetail = lifecycleDetail.trim(),
                    qualityStatus = qualityStatus,
                    qualityIssueSummary = qualityIssueSummary.trim(),
                    memoryUpdatedAt = memoryUpdatedAt,
                    updatedAt = timestamp,
                ),
            )
            database.projectDao().touch(chapter.projectId, timestamp)
        }
    }

    suspend fun hasUnchangedContent(chapter: Chapter): Boolean =
        database.chapterDao().findById(chapter.id)?.content == chapter.content

    suspend fun markChapterQualityRepaired(chapter: Chapter) {
        updateChapterLifecycle(
            chapter = chapter,
            lifecycleStatus = ChapterLifecycleStatus.PASSED,
            lifecycleDetail = "作者已确认处理门禁提示",
            qualityStatus = ChapterQualityStatus.READY,
            qualityIssueSummary = "",
        )
    }

    suspend fun replaceChapterWithRevision(chapter: Chapter, replacement: String, reason: String): Chapter = database.withTransaction {
        val timestamp = System.currentTimeMillis()
        database.chapterRevisionDao().insert(
            ChapterRevision(
                projectId = chapter.projectId,
                chapterId = chapter.id,
                previousContent = chapter.content,
                reason = reason,
            ),
        )
        val updated = chapter.copy(
            content = replacement.trim(),
            qualityStatus = ChapterQualityStatus.READY,
            qualityIssueSummary = "",
            lifecycleStatus = ChapterLifecycleStatus.PROCESSING,
            lifecycleDetail = "AI 改写完成，正在重新同步记忆与门禁",
            updatedAt = timestamp,
        )
        database.chapterDao().update(updated)
        database.projectDao().touch(chapter.projectId, timestamp)
        updated
    }

    suspend fun restoreRevision(chapter: Chapter, revision: ChapterRevision): Chapter = database.withTransaction {
        val timestamp = System.currentTimeMillis()
        val restored = chapter.copy(
            content = revision.previousContent,
            lifecycleStatus = ChapterLifecycleStatus.MANUAL,
            lifecycleDetail = "已撤回 AI 改写：${revision.reason}",
            qualityStatus = ChapterQualityStatus.READY,
            qualityIssueSummary = "",
            updatedAt = timestamp,
        )
        database.chapterDao().update(restored)
        database.chapterRevisionDao().deleteById(revision.id)
        database.projectDao().touch(chapter.projectId, timestamp)
        restored
    }

    suspend fun createAutoWriteRun(projectId: Long, requestedCount: Int): AutoWriteRun {
        val run = AutoWriteRun(projectId = projectId, requestedCount = requestedCount)
        return run.copy(id = database.autoWriteRunDao().insert(run))
    }

    suspend fun updateAutoWriteRun(run: AutoWriteRun, completedCount: Int, status: String, detail: String): AutoWriteRun {
        val updated = run.copy(
            completedCount = completedCount.coerceIn(0, run.requestedCount),
            status = status,
            detail = detail.trim(),
            updatedAt = System.currentTimeMillis(),
        )
        database.autoWriteRunDao().update(updated)
        return updated
    }

    suspend fun pauseInterruptedAutoWriteRuns() {
        database.autoWriteRunDao().pauseInterruptedRuns(
            detail = "应用上次退出时写作尚未结束，可在处理当前章节后继续",
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun recoverInterruptedWritingState() = database.withTransaction {
        database.chapterDao().markInterruptedLifecycles(
            lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
            detail = "上次处理未完成，请重新运行章节闭环",
        )
        val now = System.currentTimeMillis()
        database.autoWriteRunDao().findRecoverableRuns().forEach { run ->
            val generatedCount = database.chapterDao().countByAutoWriteRun(run.id)
            database.autoWriteRunDao().update(
                run.copy(
                    completedCount = maxOf(run.completedCount, generatedCount).coerceAtMost(run.requestedCount),
                    status = AutoWriteRunStatus.PAUSED,
                    detail = "已从本地草稿恢复进度，可在处理当前章节后继续",
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun addStoryItem(projectId: Long, kind: String, name: String, detail: String, status: String): Long {
        return database.storyItemDao().insert(
            StoryItem(projectId = projectId, kind = kind, name = name, detail = detail, status = status),
        )
    }

    suspend fun replaceChapterMentions(chapter: Chapter, itemIds: Collection<Long>) = database.withTransaction {
        database.chapterStoryMentionDao().deleteByChapter(chapter.id)
        database.chapterStoryMentionDao().insertAll(
            itemIds.distinct().map { itemId ->
                ChapterStoryMention(projectId = chapter.projectId, chapterId = chapter.id, storyItemId = itemId)
            },
        )
    }

    suspend fun addResearchNote(projectId: Long, title: String, sourceUrl: String, tags: String, content: String) {
        database.researchNoteDao().insert(ResearchNote(projectId = projectId, title = title.trim(), sourceUrl = sourceUrl.trim(), tags = tags.trim(), content = content.trim()))
    }

    suspend fun updateResearchNote(note: ResearchNote, title: String, sourceUrl: String, tags: String, content: String) {
        database.researchNoteDao().update(note.copy(title = title.trim(), sourceUrl = sourceUrl.trim(), tags = tags.trim(), content = content.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteResearchNote(noteId: Long) = database.researchNoteDao().deleteById(noteId)

    suspend fun addEditorialReview(projectId: Long, chapterId: Long, content: String) {
        database.editorialReviewDao().insert(EditorialReview(projectId = projectId, chapterId = chapterId, content = content.trim()))
    }

    suspend fun updateStoryItem(item: StoryItem, kind: String, name: String, detail: String, status: String) {
        database.storyItemDao().update(item.copy(kind = kind, name = name, detail = detail, status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addAnchor(
        projectId: Long,
        startChapter: Int,
        endChapter: Int,
        title: String,
        coreConflict: String,
        allowedPlot: String,
        forbiddenReveals: String,
        mandatoryTension: String,
    ) {
        database.storyAnchorDao().insert(
            StoryAnchor(
                projectId = projectId,
                startChapter = startChapter.coerceAtLeast(1),
                endChapter = endChapter.coerceAtLeast(startChapter.coerceAtLeast(1)),
                title = title.trim(),
                coreConflict = coreConflict.trim(),
                allowedPlot = allowedPlot.trim(),
                forbiddenReveals = forbiddenReveals.trim(),
                mandatoryTension = mandatoryTension.trim(),
            ),
        )
    }

    suspend fun addEdge(projectId: Long, sourceItemId: Long, targetItemId: Long, relation: String, description: String, sinceChapter: Int): Long {
        require(sourceItemId != targetItemId) { "关系的两端不能是同一资料卡" }
        return database.storyEdgeDao().insert(
            StoryEdge(
                projectId = projectId,
                sourceItemId = sourceItemId,
                targetItemId = targetItemId,
                relation = relation.trim(),
                description = description.trim(),
                sinceChapter = sinceChapter.coerceAtLeast(1),
            ),
        )
    }
}
