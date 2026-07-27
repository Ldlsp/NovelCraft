package com.mozhou.novelcraft

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File

data class ImportedProject(
    val project: NovelProject,
    val chapters: List<Chapter>,
)

data class CreatedProject(
    val project: NovelProject,
    val openingChapter: Chapter,
)

data class LifecycleFinishResult(
    val saved: Boolean = false,
    val nextChapterId: Long? = null,
    val nextChapterAction: NextChapterAction? = null,
)

class NovelRepository(private val database: NovelDatabase) {
    fun projects(): Flow<List<NovelProject>> = database.projectDao().observeAll()
    fun project(projectId: Long): Flow<NovelProject?> = database.projectDao().observe(projectId)
    fun chapters(projectId: Long): Flow<List<Chapter>> = database.chapterDao().observeByProject(projectId)
    fun latestRevision(chapterId: Long): Flow<ChapterRevision?> = database.chapterRevisionDao().observeLatest(chapterId)
    fun resumableAutoWriteRun(projectId: Long): Flow<AutoWriteRun?> = database.autoWriteRunDao().observeResumable(projectId)
    fun importAnalysis(projectId: Long): Flow<ImportAnalysisRun?> = database.importAnalysisDao().observe(projectId)
    fun storyItems(projectId: Long): Flow<List<StoryItem>> = database.storyItemDao().observeByProject(projectId)
    fun chapterMentions(projectId: Long): Flow<List<ChapterStoryMention>> = database.chapterStoryMentionDao().observeByProject(projectId)
    fun researchNotes(projectId: Long): Flow<List<ResearchNote>> = database.researchNoteDao().observeByProject(projectId)
    fun latestEditorialReview(chapterId: Long): Flow<EditorialReview?> = database.editorialReviewDao().observeLatest(chapterId)
    fun anchors(projectId: Long): Flow<List<StoryAnchor>> = database.storyAnchorDao().observeByProject(projectId)
    fun edges(projectId: Long): Flow<List<StoryEdge>> = database.storyEdgeDao().observeByProject(projectId)
    fun ideationDraft(): Flow<IdeationDraft?> = database.ideationDraftDao().observeLatest()
    fun pacingEvents(projectId: Long): Flow<List<ChapterPacingEvent>> = database.chapterPacingEventDao().observeByProject(projectId)
    fun styleProfiles(): Flow<List<StyleProfile>> = database.styleProfileDao().observeAll()
    fun batchReviewRuns(projectId: Long): Flow<List<BatchReviewRun>> = database.batchReviewDao().observeByProject(projectId)
    fun reviewIssues(projectId: Long): Flow<List<ReviewIssue>> = database.reviewIssueDao().observeByProject(projectId)
    fun eventMatrixRules(projectId: Long): Flow<List<EventMatrixRule>> = database.eventMatrixRuleDao().observeByProject(projectId)
    fun gateReports(chapterId: Long): Flow<List<ChapterGateReport>> = database.chapterGateReportDao().observeByChapter(chapterId)
    fun ragChunks(projectId: Long): Flow<List<RagChunk>> = database.ragChunkDao().observeByProject(projectId)

    suspend fun updateImportAnalysis(projectId: Long, status: String, stage: String, progress: Int, detail: String) {
        database.importAnalysisDao().upsert(
            ImportAnalysisRun(
                projectId = projectId,
                status = status,
                stage = stage,
                progress = progress.coerceIn(0, 100),
                detail = detail,
            ),
        )
    }

    private fun defaultEventMatrixRules(projectId: Long) = listOf(
        EventMatrixRule(projectId = projectId, ruleKey = "conflict_thrill", label = "冲突爽点", cooldown = 2, category = "冲突"),
        EventMatrixRule(projectId = projectId, ruleKey = "bond_deepening", label = "人物羁绊", cooldown = 1, category = "关系"),
        EventMatrixRule(projectId = projectId, ruleKey = "faction_building", label = "势力经营", cooldown = 2, category = "势力"),
        EventMatrixRule(projectId = projectId, ruleKey = "world_painting", label = "风土人情", cooldown = 3, category = "世界"),
        EventMatrixRule(projectId = projectId, ruleKey = "tension_escalation", label = "危机升级", cooldown = 2, category = "悬念"),
    )

    suspend fun ensureEventMatrixRules(projectId: Long) {
        if (database.eventMatrixRuleDao().listByProject(projectId).isEmpty()) {
            database.eventMatrixRuleDao().insertAll(defaultEventMatrixRules(projectId))
        }
    }

    suspend fun createProject(title: String, genre: String, premise: String): Long {
        val projectId = database.projectDao().insert(
            NovelProject(title = title.trim(), genre = genre.trim(), premise = premise.trim()),
        )
        database.chapterDao().insert(Chapter(projectId = projectId, number = 1, title = "开篇"))
        ensureEventMatrixRules(projectId)
        return projectId
    }

    suspend fun saveIdeationDraft(draft: IdeationDraft): IdeationDraft {
        val updated = draft.copy(updatedAt = System.currentTimeMillis())
        return if (updated.id == 0L) updated.copy(id = database.ideationDraftDao().insert(updated)) else {
            database.ideationDraftDao().update(updated)
            updated
        }
    }

    suspend fun finishIdeationDraft(draft: IdeationDraft): CreatedProject = database.withTransaction {
        val completed = draft.copy(
            title = draft.title.trim().ifBlank { "未命名故事" },
            genre = draft.genre.trim().ifBlank { "现代幻想" },
            premise = draft.premise.trim().ifBlank { "一个普通人意外卷入超出常识的事件，必须找到自己的立足之地。" },
            protagonist = draft.protagonist.trim().ifBlank { "主角" },
            conflict = draft.conflict.trim().ifBlank { "主角必须在代价不断升级的选择中守住最重要的人与目标。" },
            promise = draft.promise.trim().ifBlank { "每次推进都会揭开新线索，并带来更高层级的挑战。" },
            targetAudience = draft.targetAudience.trim().ifBlank { "偏好成长、悬念与爽点的中文网文读者" },
            writingStyle = draft.writingStyle.trim().ifBlank { "第三人称近距离叙事，节奏明快，以行动和对话推动剧情。" },
            forbiddenContent = draft.forbiddenContent.trim().ifBlank { "不提前揭露核心谜底，不改变已建立的人物关系。" },
        )
        val project = NovelProject(
            title = completed.title, genre = completed.genre,
            premise = completed.premise, protagonistName = completed.protagonist,
            summary = completed.promise, targetAudience = completed.targetAudience, styleGuide = completed.writingStyle,
            forbiddenContent = completed.forbiddenContent, automationLevel = completed.automationLevel,
            targetChapterWordCount = normalizeChapterWordRange(completed.targetChapterWordCount, completed.targetChapterWordCountMax).min,
            targetChapterWordCountMax = normalizeChapterWordRange(completed.targetChapterWordCount, completed.targetChapterWordCountMax).max,
            targetWordCount = completed.targetWordCount.coerceAtLeast(0),
            longFormBlueprint = "核心冲突：${completed.conflict}\n读者承诺：${completed.promise}",
        )
        val projectId = database.projectDao().insert(project)
        val openingChapter = Chapter(
            projectId = projectId,
            number = 1,
            title = "第1章：${completed.protagonist}的选择",
            content = openingDraft(completed),
            outline = "开篇目标：让${completed.protagonist}在日常被打破时做出第一次选择，并留下与${completed.conflict}相关的具体钩子。",
        )
        val openingChapterId = database.chapterDao().insert(openingChapter)
        database.storyItemDao().insert(StoryItem(projectId = projectId, kind = "角色", name = completed.protagonist, detail = "开书向导创建"))
        database.storyAnchorDao().insert(StoryAnchor(projectId = projectId, startChapter = 1, endChapter = 10, title = "第一卷起点", coreConflict = completed.conflict, mandatoryTension = completed.promise))
        database.eventMatrixRuleDao().insertAll(defaultEventMatrixRules(projectId))
        database.ideationDraftDao().deleteById(draft.id)
        CreatedProject(project.copy(id = projectId), openingChapter.copy(id = openingChapterId))
    }

    private fun openingDraft(draft: IdeationDraft): String = """${draft.protagonist}原以为，今天和过去的每一天没有区别。

可当那件与自己毫无关系的事忽然找上门时，熟悉的生活开始露出裂缝。${draft.protagonist}没有立刻明白这意味着什么，只知道如果转身离开，往后每一次想起都会后悔。

${draft.protagonist}伸出手，做出了第一个选择。

远处有人低声说出了一个名字，而那正是${draft.conflict}的开端。"""

    suspend fun importProject(context: Context, uri: Uri, title: String): ImportedProject {
        val text = DocumentTextExtractor.read(context, uri)
        return database.withTransaction {
            val importedChapters = ChapterImporter.parse(text)
            val projectId = database.projectDao().insert(
                NovelProject(
                    title = title.ifBlank { "导入作品" },
                    genre = "待 AI 分析",
                    premise = "已导入 ${importedChapters.size} 章正文，等待自动整理作品设定",
                    tags = "已导入",
                    targetAudience = "待 AI 分析",
                ),
            )
            database.chapterDao().insertAll(
                importedChapters.map {
                    Chapter(projectId = projectId, number = it.number, title = it.title, content = it.content)
                },
            )
            val storedChapters = database.chapterDao().listByProject(projectId)
            for (chapter in storedChapters) rebuildRagChunks(chapter)
            database.eventMatrixRuleDao().insertAll(defaultEventMatrixRules(projectId))
            ImportedProject(database.projectDao().findById(projectId)!!, storedChapters)
        }
    }

    suspend fun updateChapter(chapter: Chapter, content: String) {
        database.withTransaction {
            val timestamp = System.currentTimeMillis()
            database.chapterDao().update(chapter.copy(content = content, updatedAt = timestamp))
            rebuildRagChunks(chapter.copy(content = content, updatedAt = timestamp))
            database.projectDao().touch(chapter.projectId, timestamp)
        }
    }

    suspend fun saveContinuitySnapshot(snapshot: ChapterContinuitySnapshot) {
        database.chapterContinuitySnapshotDao().upsert(snapshot)
    }

    suspend fun enqueueChapterLifecycle(chapter: Chapter, afterSuccessAction: NextChapterAction? = null): ChapterLifecycleJob = database.withTransaction {
        val now = System.currentTimeMillis()
        val existing = database.chapterLifecycleJobDao().findByChapter(chapter.id)
        val job = ChapterLifecycleJob(
            chapterId = chapter.id,
            projectId = chapter.projectId,
            contentFingerprint = chapter.contentFingerprint(),
            status = ChapterLifecycleJobStatus.QUEUED,
            attempts = if (existing?.contentFingerprint == chapter.contentFingerprint()) existing.attempts else 0,
            detail = "等待后台闭环",
            afterSuccessAction = afterSuccessAction?.storageValue ?: existing?.afterSuccessAction.orEmpty(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        database.chapterLifecycleJobDao().upsert(job)
        job
    }

    suspend fun nextQueuedChapterLifecycle(projectId: Long): ChapterLifecycleJob? = database.chapterLifecycleJobDao().nextQueued(projectId)

    suspend fun claimChapterLifecycle(job: ChapterLifecycleJob): ChapterLifecycleJob? = database.withTransaction {
        val current = database.chapterLifecycleJobDao().findByChapter(job.chapterId) ?: return@withTransaction null
        if (current.status != ChapterLifecycleJobStatus.QUEUED || current.contentFingerprint != job.contentFingerprint) return@withTransaction null
        val claimed = current.copy(
            status = ChapterLifecycleJobStatus.RUNNING,
            attempts = current.attempts + 1,
            detail = "正在后台同步记忆与审阅",
            updatedAt = System.currentTimeMillis(),
        )
        database.chapterLifecycleJobDao().upsert(claimed)
        claimed
    }

    suspend fun finishChapterLifecycle(job: ChapterLifecycleJob, passed: Boolean, detail: String): LifecycleFinishResult = database.withTransaction {
        val current = database.chapterLifecycleJobDao().findByChapter(job.chapterId) ?: return@withTransaction LifecycleFinishResult()
        if (current.contentFingerprint != job.contentFingerprint) return@withTransaction LifecycleFinishResult()
        database.chapterLifecycleJobDao().upsert(
            current.copy(
                status = if (passed) ChapterLifecycleJobStatus.COMPLETED else ChapterLifecycleJobStatus.FAILED,
                detail = detail.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        if (passed) database.chapterContinuitySnapshotDao().updateForPredecessor(
            predecessorChapterId = job.chapterId,
            status = ContinuitySnapshotStatus.CONFIRMED,
            updatedAt = System.currentTimeMillis(),
        )
        val action = if (passed) NextChapterAction.fromStorage(current.afterSuccessAction) else null
        val nextChapterId = action?.let { createChapterInTransaction(current.projectId) }
        LifecycleFinishResult(saved = true, nextChapterId = nextChapterId, nextChapterAction = action)
    }

    suspend fun requeueChapterLifecycle(job: ChapterLifecycleJob, detail: String) = database.withTransaction {
        val current = database.chapterLifecycleJobDao().findByChapter(job.chapterId) ?: return@withTransaction
        if (current.contentFingerprint == job.contentFingerprint) {
            database.chapterLifecycleJobDao().upsert(
                current.copy(status = ChapterLifecycleJobStatus.QUEUED, detail = detail.trim(), updatedAt = System.currentTimeMillis()),
            )
        }
    }

    suspend fun chapterById(chapterId: Long): Chapter? = database.chapterDao().findById(chapterId)

    private fun Chapter.contentFingerprint(): String = "${content.length}:${content.hashCode()}"

    private suspend fun rebuildRagChunks(chapter: Chapter) {
        database.ragChunkDao().deleteByChapter(chapter.id)
        val chunks = chapter.content.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.chunked(3).mapIndexed { index, parts ->
            val body = parts.joinToString("\n\n").take(1600)
            RagChunk(projectId = chapter.projectId, chapterId = chapter.id, chapterNumber = chapter.number, ordinal = index, content = body, terms = Regex("[\\p{IsHan}]{2,}").findAll(body).joinToString(" ") { it.value })
        }
        if (chunks.isNotEmpty()) database.ragChunkDao().insertAll(chunks)
    }

    suspend fun updateProjectStyle(project: NovelProject, styleGuide: String) {
        val current = database.projectDao().findById(project.id) ?: project
        database.projectDao().update(current.copy(styleGuide = styleGuide.trim(), updatedAt = System.currentTimeMillis()))
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

    suspend fun updateWritingPolicy(project: NovelProject, forbiddenContent: String, automationLevel: String, chapterWordCountMin: Int, chapterWordCountMax: Int) {
        val range = normalizeChapterWordRange(chapterWordCountMin, chapterWordCountMax)
        database.projectDao().update(
            project.copy(
                forbiddenContent = forbiddenContent.trim(),
                automationLevel = automationLevel.trim().ifBlank { "半自动" },
                targetChapterWordCount = range.min,
                targetChapterWordCountMax = range.max,
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

    suspend fun updateCoverByProjectId(projectId: Long, coverPath: String) {
        val project = database.projectDao().findById(projectId) ?: return
        database.projectDao().update(project.copy(coverPath = coverPath, updatedAt = System.currentTimeMillis()))
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
        return database.withTransaction { createChapterInTransaction(projectId) }
    }

    private suspend fun createChapterInTransaction(projectId: Long): Long {
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

    suspend fun addGeneratedDraftChapter(
        projectId: Long,
        content: String,
        title: String = "",
        autoWriteRunId: Long = 0,
        outline: String = "",
        beatSheet: String = "",
    ): Chapter = database.withTransaction {
        val number = (database.chapterDao().maxNumber(projectId) ?: 0) + 1
        val timestamp = System.currentTimeMillis()
        val chapter = Chapter(
            projectId = projectId,
            number = number,
            title = chapterTitleOrFallback(title, "第${number}章：新的篇章"),
            content = sanitizeNovelBody(content),
            outline = outline.trim(),
            beatSheet = beatSheet.trim(),
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

    suspend fun recordGateOutcome(chapter: Chapter, passed: Boolean, detail: String, issueSummary: String = "") {
        database.withTransaction {
            val current = database.chapterDao().findById(chapter.id) ?: return@withTransaction
            val failures = if (passed) 0 else current.gateFailureCount + 1
            database.chapterDao().update(
                current.copy(
                    lifecycleStatus = if (passed) ChapterLifecycleStatus.PASSED else ChapterLifecycleStatus.WAITING_REVIEW,
                    lifecycleDetail = detail.trim(),
                    qualityStatus = if (passed) ChapterQualityStatus.READY else ChapterQualityStatus.NEEDS_REPAIR,
                    qualityIssueSummary = issueSummary.trim(),
                    gateFailureCount = failures,
                    requiresHumanReview = failures >= 2,
                    memoryUpdatedAt = if (passed) System.currentTimeMillis() else current.memoryUpdatedAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            database.projectDao().touch(chapter.projectId, System.currentTimeMillis())
        }
    }

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
        database.chapterLifecycleJobDao().recoverRunning(
            detail = "应用恢复后等待重新执行",
            updatedAt = System.currentTimeMillis(),
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

    suspend fun addResearchNote(projectId: Long, title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        database.researchNoteDao().insert(ResearchNote(projectId = projectId, title = title.trim(), sourceUrl = sourceUrl.trim(), tags = tags.trim(), content = content.trim(), rightsConfirmed = rightsConfirmed))
    }

    suspend fun updateResearchNote(note: ResearchNote, title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        database.researchNoteDao().update(note.copy(title = title.trim(), sourceUrl = sourceUrl.trim(), tags = tags.trim(), content = content.trim(), rightsConfirmed = rightsConfirmed, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteResearchNote(noteId: Long) = database.researchNoteDao().deleteById(noteId)

    suspend fun appendResearchAnalysis(noteId: Long, analysis: String) = database.withTransaction {
        val current = database.researchNoteDao().findById(noteId) ?: return@withTransaction
        database.researchNoteDao().update(
            current.copy(content = current.content.trimEnd() + "\n\n【AI 结构提炼】\n" + analysis.trim(), updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun addEditorialReview(projectId: Long, chapterId: Long, content: String) {
        database.editorialReviewDao().insert(EditorialReview(projectId = projectId, chapterId = chapterId, content = content.trim()))
    }

    suspend fun addGateReport(report: ChapterGateReport) = database.chapterGateReportDao().insert(report)

    suspend fun updateEventMatrixRule(rule: EventMatrixRule) = database.eventMatrixRuleDao().update(
        rule.copy(label = rule.label.trim(), cooldown = rule.cooldown.coerceIn(0, 20)),
    )

    suspend fun addEventMatrixRule(projectId: Long, label: String, cooldown: Int, category: String) {
        val key = label.lowercase().replace(Regex("[^a-z0-9\\p{IsHan}]+"), "_").trim('_').ifBlank { "event_${System.currentTimeMillis()}" }
        database.eventMatrixRuleDao().insert(EventMatrixRule(projectId = projectId, ruleKey = key, label = label.trim(), cooldown = cooldown.coerceIn(0, 20), category = category.trim().ifBlank { "自定义" }))
    }

    suspend fun deleteEventMatrixRule(id: Long) = database.eventMatrixRuleDao().deleteById(id)

    suspend fun replaceChapterPacingEvent(chapter: Chapter, eventType: String, pace: String, note: String) = database.withTransaction {
        database.chapterPacingEventDao().deleteByChapter(chapter.id)
        database.chapterPacingEventDao().insert(ChapterPacingEvent(projectId = chapter.projectId, chapterId = chapter.id, chapterNumber = chapter.number, eventType = eventType, pace = pace, note = note.trim()))
    }

    suspend fun saveStyleProfile(name: String, genre: String, guide: String, sourceProjectId: Long, fingerprint: StyleFingerprint): Long {
        require(name.isNotBlank() && guide.isNotBlank())
        return database.styleProfileDao().insert(StyleProfile(name = name.trim(), genre = genre.trim(), guide = guide.trim(), sourceProjectId = sourceProjectId, metrics = fingerprint.metrics, keywords = fingerprint.keywords))
    }

    suspend fun deleteStyleProfile(id: Long) = database.styleProfileDao().deleteById(id)

    suspend fun addBatchReview(projectId: Long, startChapter: Int, endChapter: Int, round: Int, report: String, issues: List<ReviewIssue>): Long = database.withTransaction {
        val runId = database.batchReviewDao().insert(BatchReviewRun(projectId = projectId, startChapter = startChapter, endChapter = endChapter, round = round, report = report.trim()))
        database.reviewIssueDao().insertAll(issues.map { it.copy(id = 0, projectId = projectId, reviewRunId = runId) })
        runId
    }

    suspend fun setReviewIssueStatus(id: Long, resolved: Boolean) = database.reviewIssueDao().updateStatus(id, if (resolved) "resolved" else "open")

    suspend fun backupSnapshot(projectId: Long): ProjectBackupSnapshot = database.withTransaction {
        ProjectBackupSnapshot(
            project = requireNotNull(database.projectDao().findById(projectId)),
            chapters = database.chapterDao().listByProject(projectId),
            revisions = database.chapterRevisionDao().listByProject(projectId),
            autoWriteRuns = database.autoWriteRunDao().listByProject(projectId),
            items = database.storyItemDao().listByProject(projectId),
            mentions = database.chapterStoryMentionDao().listByProject(projectId),
            notes = database.researchNoteDao().listByProject(projectId),
            editorialReviews = database.editorialReviewDao().listByProject(projectId),
            anchors = database.storyAnchorDao().listByProject(projectId),
            edges = database.storyEdgeDao().listByProject(projectId),
            pacingEvents = database.chapterPacingEventDao().listByProject(projectId),
            batchReviews = database.batchReviewDao().listByProject(projectId),
            reviewIssues = database.reviewIssueDao().listByProject(projectId),
            styleProfiles = database.styleProfileDao().listAll().filter { it.sourceProjectId == projectId },
            eventMatrixRules = database.eventMatrixRuleDao().listByProject(projectId),
            gateReports = database.chapterGateReportDao().listByProject(projectId),
        )
    }

    suspend fun restoreSnapshot(snapshot: ProjectBackupSnapshot): Long = database.withTransaction {
        val projectId = database.projectDao().insert(snapshot.project.copy(id = 0, coverPath = "", createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        val autoRunIds = mutableMapOf<Long, Long>()
        snapshot.autoWriteRuns.forEach { run -> autoRunIds[run.id] = database.autoWriteRunDao().insert(run.copy(id = 0, projectId = projectId)) }
        val chapterIds = mutableMapOf<Long, Long>()
        snapshot.chapters.forEach { chapter -> chapterIds[chapter.id] = database.chapterDao().insert(chapter.copy(id = 0, projectId = projectId, autoWriteRunId = autoRunIds[chapter.autoWriteRunId] ?: 0)) }
        val itemIds = mutableMapOf<Long, Long>()
        snapshot.items.forEach { item -> itemIds[item.id] = database.storyItemDao().insert(item.copy(id = 0, projectId = projectId)) }
        snapshot.anchors.forEach { database.storyAnchorDao().insert(it.copy(id = 0, projectId = projectId)) }
        snapshot.edges.forEach { edge ->
            val source = itemIds[edge.sourceItemId] ?: return@forEach
            val target = itemIds[edge.targetItemId] ?: return@forEach
            database.storyEdgeDao().insert(edge.copy(id = 0, projectId = projectId, sourceItemId = source, targetItemId = target))
        }
        snapshot.notes.forEach { database.researchNoteDao().insert(it.copy(id = 0, projectId = projectId)) }
        snapshot.mentions.forEach { mention ->
            val chapterId = chapterIds[mention.chapterId] ?: return@forEach
            val itemId = itemIds[mention.storyItemId] ?: return@forEach
            database.chapterStoryMentionDao().insertAll(listOf(mention.copy(id = 0, projectId = projectId, chapterId = chapterId, storyItemId = itemId)))
        }
        snapshot.editorialReviews.forEach { review -> chapterIds[review.chapterId]?.let { database.editorialReviewDao().insert(review.copy(id = 0, projectId = projectId, chapterId = it)) } }
        snapshot.pacingEvents.forEach { event -> chapterIds[event.chapterId]?.let { database.chapterPacingEventDao().insert(event.copy(id = 0, projectId = projectId, chapterId = it)) } }
        val runIds = mutableMapOf<Long, Long>()
        snapshot.batchReviews.forEach { run -> runIds[run.id] = database.batchReviewDao().insert(run.copy(id = 0, projectId = projectId)) }
        snapshot.reviewIssues.forEach { issue -> runIds[issue.reviewRunId]?.let { database.reviewIssueDao().insertAll(listOf(issue.copy(id = 0, projectId = projectId, reviewRunId = it))) } }
        snapshot.revisions.forEach { revision -> chapterIds[revision.chapterId]?.let { database.chapterRevisionDao().insert(revision.copy(id = 0, projectId = projectId, chapterId = it)) } }
        snapshot.styleProfiles.forEach { profile -> database.styleProfileDao().insert(profile.copy(id = 0, sourceProjectId = projectId)) }
        if (snapshot.eventMatrixRules.isEmpty()) ensureEventMatrixRules(projectId) else snapshot.eventMatrixRules.forEach { rule -> database.eventMatrixRuleDao().insert(rule.copy(id = 0, projectId = projectId)) }
        snapshot.gateReports.forEach { report -> chapterIds[report.chapterId]?.let { chapterId -> database.chapterGateReportDao().insert(report.copy(id = 0, projectId = projectId, chapterId = chapterId)) } }
        projectId
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
