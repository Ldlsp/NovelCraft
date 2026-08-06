package com.mozhou.novelcraft

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

private data class WritingContextInput(
    val project: NovelProject?,
    val chapter: Chapter?,
    val chapters: List<Chapter>,
    val items: List<StoryItem>,
    val anchors: List<StoryAnchor>,
)

data class ProjectProfileSuggestion(
    val title: String,
    val genre: String,
    val premise: String,
    val summary: String,
    val tags: String,
    val targetAudience: String,
    val protagonistName: String,
    val conflict: String,
    val promise: String,
    val writingStyle: String,
    val forbiddenContent: String,
)

private data class MandatoryGateResult(
    val passed: Boolean,
    val summary: String,
    val reports: List<Pair<String, String>>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class NovelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(NovelDatabase.create(application))
    private val modelPreferences = ModelPreferences(application)
    private val modelClient = OpenAiCompatibleClient()
    private val saveChapterJobs = mutableMapOf<Long, Job>()
    private var renameChapterJob: Job? = null
    private var savePlanJob: Job? = null
    private var saveBeatSheetJob: Job? = null
    private var saveStyleJob: Job? = null
    private var onlineResearchJob: Job? = null
    private var autoWritePreparing = false
    private val generationJobs = mutableMapOf<GenerationTask, Job>()
    private val generationRequests = mutableMapOf<GenerationTask, GenerationRequest>()
    private val pendingChapterContent = mutableMapOf<Long, String>()
    private var lifecycleQueueRunner: Job? = null
    private var activeLifecycleJob: ChapterLifecycleJob? = null

    val projects = repository.projects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val selectedProjectId = MutableStateFlow<Long?>(null)
    private val selectedChapterId = MutableStateFlow<Long?>(null)

    val selectedProject = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.project(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val importAnalysis = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.importAnalysis(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val chapters = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.chapters(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val storyItems = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.storyItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chapterMentions = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.chapterMentions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val researchNotes = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.researchNotes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val onlineResearchResults = MutableStateFlow<List<OnlineResearchResult>>(emptyList())
    val isOnlineResearching = MutableStateFlow(false)

    val anchors = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.anchors(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val edges = selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.edges(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ideationDraft = repository.ideationDraft().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val styleProfiles = repository.styleProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pacingEvents = selectedProjectId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.pacingEvents(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val eventMatrixRules = selectedProjectId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.eventMatrixRules(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val batchReviewRuns = selectedProjectId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.batchReviewRuns(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reviewIssues = selectedProjectId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.reviewIssues(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ragChunks = selectedProjectId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.ragChunks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedChapter = combine(chapters, selectedChapterId) { all, id ->
        all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestRevision = selectedChapter.flatMapLatest { chapter ->
        if (chapter == null) flowOf(null) else repository.latestRevision(chapter.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestEditorialReview = selectedChapter.flatMapLatest { chapter ->
        if (chapter == null) flowOf(null) else repository.latestEditorialReview(chapter.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gateReports = selectedChapter.flatMapLatest { chapter ->
        if (chapter == null) flowOf(emptyList()) else repository.gateReports(chapter.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resumableAutoWriteRun = selectedProjectId.flatMapLatest { projectId ->
        if (projectId == null) flowOf(null) else repository.resumableAutoWriteRun(projectId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val writingContextInput = combine(selectedProject, selectedChapter, chapters, storyItems, anchors) { project, chapter, allChapters, items, projectAnchors ->
        WritingContextInput(project, chapter, allChapters, items, projectAnchors)
    }

    val contextPacket = combine(writingContextInput, edges, chapterMentions, researchNotes, ragChunks) { input, graphEdges, mentions, notes, chunks ->
        if (input.project == null || input.chapter == null) ContextPacket() else ContextEngine.build(input.project, input.chapter, input.chapters, input.items, input.anchors, graphEdges, mentions, notes, chunks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContextPacket())

    val qualityIssues = combine(listOf(selectedProject, selectedChapter, storyItems, anchors, pacingEvents, eventMatrixRules)) { values ->
        val project = values[0] as NovelProject?
        val chapter = values[1] as Chapter?
        @Suppress("UNCHECKED_CAST") val items = values[2] as List<StoryItem>
        @Suppress("UNCHECKED_CAST") val projectAnchors = values[3] as List<StoryAnchor>
        @Suppress("UNCHECKED_CAST") val events = values[4] as List<ChapterPacingEvent>
        @Suppress("UNCHECKED_CAST") val rules = values[5] as List<EventMatrixRule>
        QualityGate.inspect(chapter, items, projectAnchors, project) + if (project != null && chapter != null) PacingPlanner.warnings(project, chapter, events, rules) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val modelConfig = MutableStateFlow(modelPreferences.load())
    val message = MutableStateFlow<String?>(null)
    val generationTasks = MutableStateFlow<Set<GenerationTask>>(emptySet())
    val repairPlan = MutableStateFlow<String?>(null)
    val projectProfileSuggestion = MutableStateFlow<ProjectProfileSuggestion?>(null)
    val streamedContinuation = MutableStateFlow("")
    private val outlineCascadePending = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.recoverInterruptedWritingState()
            processLifecycleQueue()
        }
    }

    fun saveIdeationDraft(draft: IdeationDraft) = viewModelScope.launch { repository.saveIdeationDraft(draft) }

    fun finishIdeationDraft(draft: IdeationDraft): Boolean {
        if (!modelConfig.value.hasTextGenerationConfiguration()) {
            message.value = "请先到“我的”配置并保存文本创作模型，再新建作品"
            return false
        }
        viewModelScope.launch {
            val created = repository.finishIdeationDraft(draft)
            selectProject(created.project.id)
            selectChapter(created.openingChapter.id)
            message.value = "《${created.project.title}》已创建，第 1 章已备好开篇草稿；点击\"AI 生成第一章\"才会生成正文"
        }
        return true
    }

    fun generateOpeningChapter() {
        val project = selectedProject.value ?: return
        val openingChapter = chapters.value.firstOrNull { it.projectId == project.id && it.number == 1 } ?: return
        val config = modelConfig.value
        if (!config.hasTextGenerationConfiguration()) {
            message.value = "开书资料包与开篇草稿已创建；请先到\"我的\"配置文本创作模型，再一键生成完整第一章"
            return
        }
        val request = beginGeneration(GenerationTask.OPENING_CHAPTER) ?: return
        message.value = "AI 正在生成第一章..."
        generationJobs[GenerationTask.OPENING_CHAPTER] = viewModelScope.launch {
            try {
                val context = buildString {
                    appendLine("作品：${project.title}")
                    appendLine("题材：${project.genre}")
                    appendLine("核心设定：${project.premise}")
                    appendLine("主角：${project.protagonistName}")
                    appendLine("长期冲突：${project.longFormBlueprint.substringAfter("核心冲突：").substringBefore("\\n")}")
                    appendLine("文风：${project.styleGuide}")
                    appendLine("禁区：${project.forbiddenContent}")
                    appendLine("叙事视角：必须使用第三人称叙事，严禁使用“我”作为叙述主语。")
                    appendLine("现在写第一章。开头从主角的具体日常或迫在眉睫的异常切入，第一章内必须发生不可逆的第一次选择，结尾留下可继续追问的具体钩子；正文不得少于${MIN_GENERATED_CHAPTER_CHARS}个字符。")
                }
                streamedContinuation.value = ""
                generateOpeningContent(config, context, request).fold(
                    onSuccess = { content ->
                        val fallbackTitle = "第1章：${project.protagonistName.ifBlank { "命运的裂缝" }}的选择"
                        val title = modelClient.generateChapterTitle(config, content, request)
                            .getOrDefault(fallbackTitle)
                            .let { chapterTitleOrFallback(it, fallbackTitle) }
                        val current = chapters.value.firstOrNull { it.id == openingChapter.id } ?: openingChapter
                        val pendingContent = pendingChapterContent.remove(current.id)
                        if (pendingContent != null) saveChapterJobs.remove(current.id)?.cancel()
                        val savedContent = mergeOpeningChapterContent(
                            openingDraft = openingChapter.content,
                            currentContent = pendingContent ?: current.content,
                            generatedContent = sanitizeNovelBody(content),
                        )
                        repository.updateChapter(current.copy(title = title), savedContent)
                        streamedContinuation.value = ""
                        message.value = "第一章已生成，可以直接续写"
                    },
                    onFailure = {
                        streamedContinuation.value = ""
                        message.value = "第一章未达到完整篇幅，已保留可继续写的开篇草稿：${it.message ?: "模型请求失败"}"
                    },
                )
            } finally {
                finishGeneration(GenerationTask.OPENING_CHAPTER, request)
            }
        }
    }

    private suspend fun generateOpeningContent(config: ModelConfig, context: String, request: GenerationRequest): Result<String> {
        val firstPass = modelClient.writeFullChapter(config, context, request) { delta ->
            streamedContinuation.value = sanitizeNovelBody(streamedContinuation.value + delta)
        }
        return firstPass.fold(
            onSuccess = { content -> expandOpeningChapter(config, content, request) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun expandOpeningChapter(config: ModelConfig, opening: String, request: GenerationRequest): Result<String> {
        var content = sanitizeNovelBody(opening)
        repeat(MAX_OPENING_CHAPTER_GENERATION_ATTEMPTS - 1) {
            if (!needsChapterExpansion(content)) return Result.success(content)
            val remainingChars = MIN_GENERATED_CHAPTER_CHARS - content.length
            streamedContinuation.value += "\n\n"
            val continuation = modelClient.continueWriting(
                config = config,
                context = "以下是已生成的第一章正文。只从最后一句之后继续补写，不要重复、总结、改写或添加标题；至少补写${remainingChars}个中文字符，使整章总长度不少于${MIN_GENERATED_CHAPTER_CHARS}个字符。\n\n已生成正文：\n$content",
                request = request,
            ) { delta -> streamedContinuation.value = sanitizeNovelBody(streamedContinuation.value + delta) }.getOrElse { return Result.failure(it) }
            content = sanitizeNovelBody(content.trimEnd() + "\n\n" + continuation)
        }
        return if (needsChapterExpansion(content)) {
            Result.failure(IllegalStateException("模型在${MAX_OPENING_CHAPTER_GENERATION_ATTEMPTS}次生成后仍未达到${MIN_GENERATED_CHAPTER_CHARS}个字符"))
        } else {
            Result.success(content)
        }
    }

    fun generateGuidedIdeation(seed: String, genre: String) {
        val config = modelConfig.value
        if (!config.hasTextGenerationConfiguration()) {
            message.value = "请先到“我的”配置文本创作模型，再让 AI 整理开书资料"
            return
        }
        val request = beginGeneration(GenerationTask.PROJECT_PROFILE) ?: return
        message.value = "AI 正在整理开书资料..."
        val prompt = buildString {
            appendLine("作者目前只有一个模糊想法，请你主动补足完整开书资料。")
            appendLine("灵感：${seed.trim().ifBlank { "请自由创作一个适合中文网文连载的故事" }}")
            if (genre.isNotBlank()) appendLine("作者偏好的题材：${genre.trim()}")
            appendLine("资料应有明确的主角起点、长期冲突和可持续悬念；不要反问作者。")
        }
        generationJobs[GenerationTask.PROJECT_PROFILE] = viewModelScope.launch {
            try {
                modelClient.generateProjectProfile(config, prompt, request).fold(
                    onSuccess = { raw ->
                        val profile = parseProjectProfile(raw)
                        repository.saveIdeationDraft(
                            IdeationDraft(
                                title = profile.title,
                                genre = profile.genre.ifBlank { genre },
                                premise = profile.premise.ifBlank { seed.trim() },
                                protagonist = profile.protagonistName,
                                conflict = profile.conflict,
                                promise = profile.promise.ifBlank { profile.summary },
                                targetAudience = profile.targetAudience,
                                writingStyle = profile.writingStyle,
                                forbiddenContent = profile.forbiddenContent,
                            ),
                        )
                        message.value = "AI 已整理好开书资料，确认后即可开始写作"
                    },
                    onFailure = { message.value = it.message ?: "AI 整理开书资料失败" },
                )
            } finally {
                finishGeneration(GenerationTask.PROJECT_PROFILE, request)
            }
        }
    }

    fun savePacingEvent(eventType: String, pace: String, note: String) {
        val chapter = selectedChapter.value ?: return
        viewModelScope.launch {
            repository.replaceChapterPacingEvent(chapter, eventType, pace, note)
            message.value = "已登记本章节奏事件"
        }
    }

    fun saveEventMatrixRule(rule: EventMatrixRule) = viewModelScope.launch {
        repository.updateEventMatrixRule(rule)
        message.value = "事件规则已更新"
    }

    fun addEventMatrixRule(label: String, cooldown: Int, category: String) {
        val projectId = selectedProjectId.value ?: return
        viewModelScope.launch {
            if (label.isBlank()) { message.value = "请填写事件名称"; return@launch }
            repository.addEventMatrixRule(projectId, label, cooldown, category)
            message.value = "已添加事件规则"
        }
    }

    fun deleteEventMatrixRule(rule: EventMatrixRule) = viewModelScope.launch {
        repository.deleteEventMatrixRule(rule.id)
        message.value = "已删除事件规则"
    }

    fun pacingRecommendation(): PacingRecommendation? = selectedProject.value?.let { project ->
        PacingPlanner.recommend(project, pacingEvents.value, eventMatrixRules.value, selectedChapter.value?.number ?: (chapters.value.maxOfOrNull { it.number } ?: 0) + 1)
    }

    fun saveCurrentStyleProfile(name: String) {
        val project = selectedProject.value ?: return
        if (project.styleGuide.isBlank()) { message.value = "请先生成或填写当前作品文风"; return }
        viewModelScope.launch {
            val sample = selectedChapter.value?.content?.takeIf { it.isNotBlank() } ?: project.styleGuide
            repository.saveStyleProfile(name, project.genre, project.styleGuide, project.id, StyleFingerprintAnalyzer.analyze(sample))
            message.value = "文风档案已保存到跨作品文风库"
        }
    }

    fun aiTraceReport(): AiTraceReport = AiTraceDetector.inspect(selectedChapter.value?.content.orEmpty())

    fun researchPlan(): ResearchPlan? = selectedProject.value?.let { ResearchPlanner.build(it, researchNotes.value) }

    fun applyStyleProfile(profile: StyleProfile) {
        val project = selectedProject.value ?: return
        viewModelScope.launch { repository.updateProjectStyle(project, profile.guide); message.value = "已应用文风档案：${profile.name}" }
    }

    fun deleteStyleProfile(profile: StyleProfile) = viewModelScope.launch { repository.deleteStyleProfile(profile.id) }

    fun setReviewIssueResolved(issue: ReviewIssue, resolved: Boolean) = viewModelScope.launch { repository.setReviewIssueStatus(issue.id, resolved) }

    fun exportProjectBackup(uri: Uri) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val raw = ProjectBackupCodec.encode(repository.backupSnapshot(project.id))
                getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(raw) } ?: error("无法写入备份文件")
            }.onSuccess { message.value = "完整项目备份已导出" }.onFailure { message.value = "备份失败：${it.message}" }
        }
    }

    fun restoreProjectBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("无法读取备份文件")
                val snapshot = ProjectBackupCodec.decode(raw)
                val projectId = repository.restoreSnapshot(snapshot)
                ProjectBackupCodec.coverData(raw)?.let { bytes ->
                    val file = File(getApplication<Application>().filesDir, "covers/restored-$projectId.jpg").apply { parentFile?.mkdirs(); writeBytes(bytes) }
                    repository.updateCoverByProjectId(projectId, file.absolutePath)
                }
                projectId
            }.onSuccess { projectId -> selectProject(projectId); message.value = "完整项目已恢复为新作品" }.onFailure { message.value = "恢复失败：${it.message}" }
        }
    }

    private fun beginGeneration(task: GenerationTask): GenerationRequest? {
        importAnalysis.value?.takeIf { it.status in setOf(ImportAnalysisStatus.QUEUED, ImportAnalysisStatus.RUNNING) }?.let { analysis ->
            message.value = "导入分析正在${analysis.stage}，请等待完成或先在作品页取消"
            return null
        }
        if (task in generationTasks.value) {
            message.value = "${task.label}正在生成"
            return null
        }
        val activeTask = generationTasks.value.firstOrNull { active -> !task.canRunAlongside(active) }
        if (activeTask != null) {
            message.value = "正在执行${activeTask.label}，请等待完成或先取消"
            return null
        }
        return GenerationRequest().also { request ->
            generationRequests[task] = request
            generationTasks.value = generationTasks.value + task
            AiGenerationForegroundService.start(getApplication<Application>(), task.label)
        }
    }

    private fun finishGeneration(task: GenerationTask, request: GenerationRequest) {
        if (generationRequests[task] !== request) return
        generationRequests.remove(task)
        generationJobs.remove(task)
        generationTasks.value = generationTasks.value - task
        val activeTasks = generationTasks.value
        if (shouldKeepGenerationForeground(activeTasks)) {
            AiGenerationForegroundService.start(getApplication<Application>(), activeTasks.first().label)
        } else {
            AiGenerationForegroundService.stop(getApplication<Application>())
        }
        if (task != GenerationTask.CHAPTER_LIFECYCLE) processLifecycleQueue()
    }

    fun selectProject(projectId: Long) {
        selectedProjectId.value = projectId
        selectedChapterId.value = null
        projectProfileSuggestion.value = null
        viewModelScope.launch {
            repository.ensureEventMatrixRules(projectId)
            delay(250)
            processLifecycleQueue()
        }
    }

    fun selectChapter(chapterId: Long) {
        selectedChapterId.value = chapterId
    }

    fun createProject(title: String, genre: String, premise: String) {
        if (title.isBlank()) {
            message.value = "请填写书名"
            return
        }
        viewModelScope.launch {
            val projectId = repository.createProject(title, genre.ifBlank { "待分类" }, premise)
            selectProject(projectId)
            message.value = "已创建《" + title.trim() + "》"
        }
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            message.value = "正在导入正文并建立本地索引..."
            runCatching {
                repository.importProject(
                    getApplication(),
                    uri,
                    uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "导入作品",
                )
            }.onSuccess { imported ->
                selectProject(imported.project.id)
                autoFillImportedProject(imported)
            }.onFailure {
                message.value = it.message ?: "导入失败"
            }
        }
    }

    /** Imports always index locally; cloud analysis continues through WorkManager when the app is backgrounded. */
    private suspend fun autoFillImportedProject(imported: ImportedProject) {
        val config = modelConfig.value
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            repository.updateImportAnalysis(
                imported.project.id,
                ImportAnalysisStatus.WAITING_FOR_CONFIG,
                "等待模型配置",
                0,
                "请在“我的”中完成云端文本模型配置后点击开始分析",
            )
            message.value = "已导入 ${imported.chapters.size} 章并建立本地索引；配置云端模型后可自动补全作品资料"
            return
        }
        repository.updateImportAnalysis(
            imported.project.id,
            ImportAnalysisStatus.QUEUED,
            "等待后台开始",
            0,
            "已建立本地索引，AI 将提炼作品资料和文风",
        )
        ImportAnalysisScheduler.enqueue(getApplication(), imported.project.id)
        message.value = "已导入 ${imported.chapters.size} 章并建立本地索引；AI 分析已转入后台"
    }

    fun startImportAnalysis() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            val config = modelConfig.value
            if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
                repository.updateImportAnalysis(project.id, ImportAnalysisStatus.WAITING_FOR_CONFIG, "等待模型配置", 0, "请在“我的”中完成云端文本模型配置")
                message.value = "请先在“我的”配置云端文本模型"
                return@launch
            }
            repository.updateImportAnalysis(project.id, ImportAnalysisStatus.QUEUED, "等待后台开始", 0, "AI 将提炼作品资料和文风")
            ImportAnalysisScheduler.enqueue(getApplication(), project.id)
            message.value = "导入分析已加入后台队列"
        }
    }

    fun cancelImportAnalysis() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            ImportAnalysisScheduler.cancel(getApplication(), project.id)
            repository.updateImportAnalysis(project.id, ImportAnalysisStatus.CANCELLED, "已取消", 0, "导入分析已取消，可随时重新开始")
            message.value = "已取消后台导入分析"
        }
    }

    private fun parseProjectProfile(raw: String): ProjectProfileSuggestion {
        val json = org.json.JSONObject(raw.trim().removePrefix("```json").removeSuffix("```").trim())
        return ProjectProfileSuggestion(
            title = json.optString("title").trim(),
            genre = json.optString("genre").trim(),
            premise = json.optString("premise").trim(),
            summary = json.optString("summary").trim(),
            tags = json.optString("tags").trim(),
            targetAudience = json.optString("targetAudience").trim(),
            protagonistName = json.optString("protagonistName").trim(),
            conflict = json.optString("conflict").trim(),
            promise = json.optString("promise").trim(),
            writingStyle = json.optString("writingStyle").trim(),
            forbiddenContent = json.optString("forbiddenContent").trim(),
        )
    }

    fun exportDocument(uri: Uri) = exportProject(uri, ExportFormat.MARKDOWN)
    fun exportDocx(uri: Uri) = exportProject(uri, ExportFormat.DOCX)
    fun exportEpub(uri: Uri) = exportProject(uri, ExportFormat.EPUB)
    fun exportPdf(uri: Uri) = exportProject(uri, ExportFormat.PDF)

    private fun exportProject(uri: Uri, format: ExportFormat) {
        val project = selectedProject.value ?: run {
            message.value = "请先选择要导出的作品"
            return
        }
        val chaptersToExport = chapters.value
        val itemsToExport = storyItems.value
        val edgesToExport = edges.value
        val notesToExport = researchNotes.value
        viewModelScope.launch {
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    when (format) {
                        ExportFormat.MARKDOWN -> output.bufferedWriter().use { it.write(DocumentExport.markdown(project, chaptersToExport, itemsToExport, edgesToExport, notesToExport)) }
                        ExportFormat.DOCX -> DocumentExport.writeDocx(output, project, chaptersToExport, notesToExport)
                        ExportFormat.EPUB -> DocumentExport.writeEpub(output, project, chaptersToExport, notesToExport)
                        ExportFormat.PDF -> DocumentExport.writePdf(output, project, chaptersToExport, notesToExport)
                    }
                }
                    ?: error("无法创建导出文件")
            }.onSuccess {
                message.value = "已导出${format.label}：${project.title}"
            }.onFailure {
                message.value = it.message ?: "导出失败"
            }
        }
    }

    fun saveChapter(content: String) {
        val chapter = selectedChapter.value ?: return
        pendingChapterContent[chapter.id] = content
        saveChapterJobs.remove(chapter.id)?.cancel()
        saveChapterJobs[chapter.id] = viewModelScope.launch {
            delay(500)
            val pending = pendingChapterContent[chapter.id] ?: return@launch
            repository.updateChapter(chapter, pending)
            if (pendingChapterContent[chapter.id] == pending) pendingChapterContent.remove(chapter.id)
            saveChapterJobs.remove(chapter.id)
        }
    }

    fun renameChapter(title: String) {
        val chapter = selectedChapter.value ?: return
        renameChapterJob?.cancel()
        renameChapterJob = viewModelScope.launch {
            delay(500)
            repository.renameChapter(chapter, title)
        }
    }

    fun addChapter(action: NextChapterAction) {
        val projectId = selectedProjectId.value ?: return
        val current = selectedChapter.value
        viewModelScope.launch {
            val previous = current?.let { chapter ->
                val pending = pendingChapterContent.remove(chapter.id)
                if (pending == null) chapter else {
                    saveChapterJobs.remove(chapter.id)?.cancel()
                    repository.updateChapter(chapter, pending)
                    chapter.copy(content = pending)
                }
            }
            if (previous == null || previous.content.isBlank()) {
                message.value = "请先完成当前章节正文，再新建下一章"
                return@launch
            }
            when (chapterAdvanceMode(previous.lifecycleStatus)) {
                ChapterAdvanceMode.WAIT_FOR_SUCCESS -> {
                    if (previous.lifecycleStatus == ChapterLifecycleStatus.PASSED) {
                        val nextChapterId = repository.addChapter(projectId)
                        selectedChapterId.value = nextChapterId
                        val nextChapter = repository.chapterById(nextChapterId) ?: return@launch
                        if (action == NextChapterAction.GENERATE_WITH_AI) {
                            startContinuation(project = selectedProject.value ?: return@launch, chapter = nextChapter, direction = "")
                        } else {
                            message.value = "已新建第${nextChapter.number}章"
                        }
                    } else {
                        repository.enqueueChapterLifecycle(previous, action)
                        processLifecycleQueue()
                        message.value = "第${previous.number}章正在闭环；通过后将${if (action == NextChapterAction.GENERATE_WITH_AI) "创建并生成" else "创建"}下一章"
                    }
                }
            }
        }
    }

    fun deleteCurrentChapter() {
        val chapter = selectedChapter.value ?: return
        viewModelScope.launch {
            runCatching { repository.deleteChapter(chapter) }
                .onSuccess {
                    selectedChapterId.value = null
                    message.value = "已删除第${chapter.number}章"
                }
                .onFailure { message.value = it.message ?: "删除章节失败" }
        }
    }

    fun deleteCurrentProject() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.deleteProject(project)
            selectedProjectId.value = null
            selectedChapterId.value = null
            message.value = "已删除《${project.title}》"
        }
    }

    fun saveChapterPlan(outline: String, targetWordCount: Int) {
        val chapter = selectedChapter.value ?: return
        savePlanJob?.cancel()
        savePlanJob = viewModelScope.launch {
            delay(500)
            repository.updateChapterPlan(chapter, outline, targetWordCount)
        }
    }

    fun saveBeatSheet(beatSheet: String) {
        val chapter = selectedChapter.value ?: return
        saveBeatSheetJob?.cancel()
        saveBeatSheetJob = viewModelScope.launch {
            delay(500)
            repository.updateChapterBeatSheet(chapter, beatSheet)
        }
    }

    fun saveStyleGuide(styleGuide: String) {
        val project = selectedProject.value ?: return
        saveStyleJob?.cancel()
        saveStyleJob = viewModelScope.launch {
            delay(500)
            repository.updateProjectStyle(project, styleGuide)
        }
    }

    fun saveProjectProfile(
        title: String,
        genre: String,
        premise: String,
        summary: String,
        tags: String,
        targetAudience: String,
        protagonistName: String,
    ) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.updateProjectProfile(project, title, genre, premise, summary, tags, targetAudience, protagonistName)
            message.value = "作品资料已保存"
        }
    }

    fun saveWritingPolicy(forbiddenContent: String, automationLevel: String, targetChapterWordCountMin: Int, targetChapterWordCountMax: Int) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.updateWritingPolicy(project, forbiddenContent, automationLevel, targetChapterWordCountMin, targetChapterWordCountMax)
            message.value = "创作策略已保存并将用于后续生成"
        }
    }

    fun generateCover() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.COVER) ?: return
        message.value = "正在生成封面..."
        val prompt = buildString {
            append("Create a polished vertical Chinese web novel cover illustration. ")
            append("No text, no typography, no logo, no watermark. ")
            append("Title concept: ${project.title}. ")
            if (project.genre.isNotBlank()) append("Genre: ${project.genre}. ")
            if (project.summary.isNotBlank()) append("Synopsis: ${project.summary.take(600)}. ")
            if (project.tags.isNotBlank()) append("Tags: ${project.tags}. ")
            if (project.protagonistName.isNotBlank()) append("Protagonist: ${project.protagonistName}. ")
            append("Portrait 2:3 composition, a clear focal subject, cinematic lighting, detailed commercial illustration.")
        }
        generationJobs[GenerationTask.COVER] = viewModelScope.launch {
            try {
                modelClient.generateCover(modelConfig.value, prompt, request).fold(
                    onSuccess = { bytes ->
                        val folder = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
                        val file = File(folder, "cover-${project.id}.png")
                        file.writeBytes(bytes)
                        repository.updateCover(project, file.absolutePath)
                        message.value = "封面已保存到本机书架"
                    },
                    onFailure = { message.value = it.message ?: "封面生成失败" },
                )
            } finally {
                finishGeneration(GenerationTask.COVER, request)
            }
        }
    }

    fun importCover(uri: Uri) {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val folder = File(getApplication<Application>().filesDir, "covers").apply { mkdirs() }
                    val temporary = File.createTempFile("cover-${project.id}-", ".upload", folder)
                    try {
                        CoverFileTransfer.copyToTemporaryFile(temporary) { resolver.openInputStream(uri) }
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(temporary.absolutePath, options)
                        require(options.outWidth > 0 && options.outHeight > 0) { "请选择有效的图片文件" }
                        val extension = when (resolver.getType(uri)) {
                            "image/jpeg" -> "jpg"
                            "image/webp" -> "webp"
                            "image/heic", "image/heif" -> "heic"
                            "image/avif" -> "avif"
                            else -> "png"
                        }
                        val destination = File(folder, "cover-${project.id}.$extension")
                        temporary.copyTo(destination, overwrite = true)
                        repository.updateCover(project, destination.absolutePath)
                    } finally {
                        temporary.delete()
                    }
                }
            }.onSuccess {
                message.value = "已使用本地图片作为书架封面"
            }.onFailure {
                message.value = "封面导入失败：${it.message ?: it.javaClass.simpleName}"
            }
        }
    }

    fun testImageModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            message.value = "正在测试封面 AI..."
            modelClient.testImage(config).fold(
                onSuccess = { message.value = it },
                onFailure = { message.value = it.message ?: "封面 AI 测试失败" },
            )
        }
    }

    fun generateProjectProfile() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.PROJECT_PROFILE) ?: return
        message.value = "正在补全作品设定..."
        val context = buildString {
            appendLine("书名：${project.title}")
            if (project.genre.isNotBlank()) appendLine("已有题材：${project.genre}")
            if (project.premise.isNotBlank()) appendLine("已有一句话设定：${project.premise}")
            if (project.summary.isNotBlank()) appendLine("已有简介：${project.summary}")
            if (project.tags.isNotBlank()) appendLine("已有标签：${project.tags}")
            if (project.targetAudience.isNotBlank()) appendLine("已有目标读者：${project.targetAudience}")
            if (project.protagonistName.isNotBlank()) appendLine("已有主角名：${project.protagonistName}")
        }
        generationJobs[GenerationTask.PROJECT_PROFILE] = viewModelScope.launch {
            try {
                modelClient.generateProjectProfile(modelConfig.value, context, request).fold(
                    onSuccess = { raw ->
                        projectProfileSuggestion.value = parseProjectProfile(raw)
                        message.value = "作品设定已生成，请确认后保存"
                    },
                    onFailure = { message.value = it.message ?: "生成作品设定失败" },
                )
            } catch (_: Exception) {
                message.value = "AI 返回的作品设定格式无效，请重试"
            } finally {
                finishGeneration(GenerationTask.PROJECT_PROFILE, request)
            }
        }
    }

    fun saveLongFormBlueprint(blueprint: String) {
        val project = selectedProject.value ?: return
        viewModelScope.launch { repository.updateLongFormBlueprint(project, blueprint) }
    }

    fun savePacing(targetChapters: Int, targetWords: Int, profile: String) {
        val project = selectedProject.value ?: return
        viewModelScope.launch { repository.updatePacing(project, targetChapters, targetWords, profile) }
    }

    fun addResearchNote(title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank() || content.isBlank()) {
            message.value = "请填写调研标题和内容"
            return
        }
        viewModelScope.launch {
            repository.addResearchNote(projectId, title, sourceUrl, tags, content, rightsConfirmed)
            message.value = "调研笔记已保存到本机"
        }
    }

    fun updateResearchNote(note: ResearchNote, title: String, sourceUrl: String, tags: String, content: String, rightsConfirmed: Boolean) {
        if (title.isBlank() || content.isBlank()) {
            message.value = "请填写调研标题和内容"
            return
        }
        viewModelScope.launch { repository.updateResearchNote(note, title, sourceUrl, tags, content, rightsConfirmed) }
    }

    fun deleteResearchNote(note: ResearchNote) {
        viewModelScope.launch { repository.deleteResearchNote(note.id) }
    }

    fun searchOnlineResearch(query: String) {
        if (isOnlineResearching.value) return
        onlineResearchJob?.cancel()
        onlineResearchJob = viewModelScope.launch {
            isOnlineResearching.value = true
            message.value = "正在检索公开资料..."
            runCatching { OnlineResearchClient.search(query) }
                .onSuccess { results ->
                    onlineResearchResults.value = results
                    message.value = if (results.isEmpty()) "没有找到公开资料，请换一个更具体的关键词" else "找到 ${results.size} 条可引用公开资料"
                }
                .onFailure { message.value = it.message ?: "联网调研失败，请检查网络后重试" }
            isOnlineResearching.value = false
        }
    }

    fun clearOnlineResearchResults() {
        onlineResearchResults.value = emptyList()
    }

    fun collectOnlineResearch(result: OnlineResearchResult) {
        val projectId = selectedProjectId.value ?: return
        viewModelScope.launch {
            val content = "公开资料摘要：${result.excerpt}\n\n引用：${result.sourceLabel}\n检索时间：${result.retrievedAt}\n原始链接：${result.sourceUrl}"
            repository.addResearchNote(projectId, result.title, result.sourceUrl, "联网调研, 可追溯引用", content, true)
            message.value = "已收录资料并保留引用链接"
        }
    }

    fun analyzeReferenceStructure(note: ResearchNote) {
        if (!note.rightsConfirmed) {
            message.value = "请先确认笔记为自写摘要或你拥有处理授权，不能分析受保护正文"
            return
        }
        if (note.content.length > 4_000) {
            message.value = "参考分析仅接受不超过 4000 字的自写摘要，不接受作品正文"
            return
        }
        val request = beginGeneration(GenerationTask.REFERENCE_ANALYSIS) ?: return
        message.value = "正在提炼参考结构..."
        val context = "标题：${note.title}\n标签：${note.tags}\n来源：${note.sourceUrl}\n作者摘要：\n${note.content}"
        generationJobs[GenerationTask.REFERENCE_ANALYSIS] = viewModelScope.launch {
            try {
                modelClient.analyzeReferenceStructure(modelConfig.value, context, request).fold(
                    onSuccess = { analysis ->
                        repository.appendResearchAnalysis(note.id, analysis)
                        message.value = "结构提炼已附加到调研笔记"
                    },
                    onFailure = { message.value = it.message ?: "结构提炼失败" },
                )
            } finally {
                finishGeneration(GenerationTask.REFERENCE_ANALYSIS, request)
            }
        }
    }

    fun generateEditorialReview() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (chapter.content.isBlank()) {
            message.value = "本章还没有正文，无法审稿"
            return
        }
        val reviewerValues = listOf(modelConfig.value.reviewerBaseUrl, modelConfig.value.reviewerApiKey, modelConfig.value.reviewerModel)
        if (reviewerValues.any(String::isBlank) && reviewerValues.any(String::isNotBlank)) {
            message.value = "独立审稿模型需同时填写 Base URL、API Key 和模型名称，或全部留空"
            return
        }
        val request = beginGeneration(GenerationTask.EDITORIAL_REVIEW) ?: return
        message.value = "正在进行编辑审稿..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, edges.value, chapterMentions.value, researchNotes.value, ragChunks.value).prompt + "\n完整正文：\n${chapter.content}"
        generationJobs[GenerationTask.EDITORIAL_REVIEW] = viewModelScope.launch {
            try {
                modelClient.editorialReview(reviewModelConfig(), context, request).fold(
                    onSuccess = { review ->
                        repository.addEditorialReview(project.id, chapter.id, review)
                        message.value = "编辑审稿已保存"
                    },
                    onFailure = { message.value = it.message ?: "编辑审稿失败" },
                )
            } finally {
                finishGeneration(GenerationTask.EDITORIAL_REVIEW, request)
            }
        }
    }

    fun runEditorialTeamReview() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (chapter.content.isBlank()) { message.value = "本章还没有正文，无法启动编辑团队"; return }
        val request = beginGeneration(GenerationTask.EDITORIAL_TEAM) ?: return
        message.value = "编辑团队正在依次核查策划、角色与文字..."
        val context = ContextEngine.build(
            project, chapter, chapters.value, storyItems.value, anchors.value, edges.value,
            chapterMentions.value, researchNotes.value, ragChunks.value,
        ).prompt + "\n完整正文：\n${chapter.content}"
        generationJobs[GenerationTask.EDITORIAL_TEAM] = viewModelScope.launch {
            try {
                val reviewer = reviewModelConfig()
                val reports = buildList {
                    modelClient.generateChapterPlan(reviewer, context, request).fold(
                        onSuccess = { add("【总策划】\n$it") },
                        onFailure = { add("【总策划】运行失败：${it.message}") },
                    )
                    modelClient.characterConsistencyReview(reviewer, context, request).fold(
                        onSuccess = { add("【角色校对】\n$it") },
                        onFailure = { add("【角色校对】运行失败：${it.message}") },
                    )
                    modelClient.copyeditReview(reviewer, context + "\n请作为文字编辑给出审校结论。", request).fold(
                        onSuccess = { add("【文字编辑】\n$it") },
                        onFailure = { add("【文字编辑】运行失败：${it.message}") },
                    )
                }
                repository.addEditorialReview(project.id, chapter.id, reports.joinToString("\n\n"))
                message.value = "编辑团队审稿已保存，不会自动修改正文"
            } finally {
                finishGeneration(GenerationTask.EDITORIAL_TEAM, request)
            }
        }
    }

    fun generateBatchEditorialReview(startChapter: Int, endChapter: Int) {
        val project = selectedProject.value ?: return
        val selected = chapters.value.filter { it.number in startChapter..endChapter && it.content.isNotBlank() }
        if (selected.isEmpty()) { message.value = "所选范围没有可审稿的正文"; return }
        val reviewerValues = listOf(modelConfig.value.reviewerBaseUrl, modelConfig.value.reviewerApiKey, modelConfig.value.reviewerModel)
        if (reviewerValues.any(String::isBlank) && reviewerValues.any(String::isNotBlank)) { message.value = "独立审稿模型需完整配置或全部留空"; return }
        val request = beginGeneration(GenerationTask.BATCH_REVIEW) ?: return
        message.value = "正在批量审稿第${selected.first().number}-${selected.last().number}章..."
        generationJobs[GenerationTask.BATCH_REVIEW] = viewModelScope.launch {
            try {
                val body = selected.joinToString("\n\n") { "【第${it.number}章 ${it.title}】\n${it.content.take(8_000)}" }
                val context = "你是网文责编。跨章节审稿后，逐行输出问题，格式必须为：[P0|P1|P2][第N章或全局] 问题摘要。P0为矛盾/泄露，P1为节奏/角色，P2为措辞/可选优化；没有问题也要明确写无。\n\n$body"
                modelClient.editorialReview(reviewModelConfig(), context, request).fold(
                    onSuccess = { report ->
                        val nextRound = (batchReviewRuns.value.maxOfOrNull { it.round } ?: 0) + 1
                        repository.addBatchReview(project.id, selected.first().number, selected.last().number, nextRound, report, parseReviewIssues(report))
                        message.value = "批量审稿已完成，问题已进入审核台账"
                    },
                    onFailure = { message.value = it.message ?: "批量审稿失败" },
                )
            } finally { finishGeneration(GenerationTask.BATCH_REVIEW, request) }
        }
    }

    private fun parseReviewIssues(report: String): List<ReviewIssue> {
        val regex = Regex("\\[(P[012])\\]\\s*\\[(?:第(\\d+)章|全局)\\]\\s*(.+)")
        return report.lineSequence().mapNotNull { line -> regex.find(line.trim())?.let { match ->
            ReviewIssue(severity = match.groupValues[1], chapterNumber = match.groupValues[2].toIntOrNull() ?: 0, summary = match.groupValues[3].take(240))
        } }.toList()
    }

    private fun reviewModelConfig(): ModelConfig = modelConfig.value.let { config ->
        if (config.reviewerBaseUrl.isBlank() || config.reviewerApiKey.isBlank() || config.reviewerModel.isBlank()) config
        else config.copy(baseUrl = config.reviewerBaseUrl, apiKey = config.reviewerApiKey, model = config.reviewerModel, protocol = config.reviewerProtocol.ifBlank { config.protocol })
    }

    fun generateLongFormBlueprint() {
        val project = selectedProject.value ?: return
        val request = beginGeneration(GenerationTask.LONG_FORM_BLUEPRINT) ?: return
        message.value = "正在生成长篇路线图..."
        val context = buildString {
            appendLine("书名：${project.title}")
            appendLine("题材：${project.genre}")
            appendLine("核心设定：${project.premise}")
            if (project.summary.isNotBlank()) appendLine("简介：${project.summary}")
            if (project.tags.isNotBlank()) appendLine("标签：${project.tags}")
            if (project.targetAudience.isNotBlank()) appendLine("读者：${project.targetAudience}")
            if (project.protagonistName.isNotBlank()) appendLine("主角：${project.protagonistName}")
            if (project.longFormBlueprint.isNotBlank()) appendLine("现有路线图：${project.longFormBlueprint}")
        }
        generationJobs[GenerationTask.LONG_FORM_BLUEPRINT] = viewModelScope.launch {
            try {
                modelClient.generateLongFormBlueprint(modelConfig.value, context, request).fold(
                    onSuccess = { blueprint ->
                        repository.updateLongFormBlueprint(project, blueprint)
                        message.value = "长篇路线图已生成，可在作品页继续编辑"
                    },
                    onFailure = { message.value = it.message ?: "生成长篇路线图失败" },
                )
            } finally {
                finishGeneration(GenerationTask.LONG_FORM_BLUEPRINT, request)
            }
        }
    }

    fun addStoryItem(kind: String, name: String, detail: String, status: String) {
        val projectId = selectedProjectId.value ?: return
        if (name.isBlank()) {
            message.value = "请填写资料名称"
            return
        }
        viewModelScope.launch {
            repository.addStoryItem(projectId, kind, name, detail, status)
            message.value = "已保存" + kind
        }
    }

    fun updateStoryItem(item: StoryItem, kind: String, name: String, detail: String, status: String) {
        if (name.isBlank()) {
            message.value = "请填写资料名称"
            return
        }
        viewModelScope.launch {
            repository.updateStoryItem(item, kind, name, detail, status)
            message.value = "已更新${item.name}"
        }
    }

    fun addAnchor(
        startChapter: Int,
        endChapter: Int,
        title: String,
        coreConflict: String,
        allowedPlot: String,
        forbiddenReveals: String,
        mandatoryTension: String,
    ) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank() || coreConflict.isBlank()) {
            message.value = "请填写锚点标题与核心冲突"
            return
        }
        viewModelScope.launch {
            repository.addAnchor(projectId, startChapter, endChapter, title, coreConflict, allowedPlot, forbiddenReveals, mandatoryTension)
            message.value = "大纲锚点已保存"
        }
    }

    fun addEdge(sourceItemId: Long, targetItemId: Long, relation: String, description: String, sinceChapter: Int) {
        val projectId = selectedProjectId.value ?: return
        if (relation.isBlank()) {
            message.value = "请填写关系类型"
            return
        }
        viewModelScope.launch {
            runCatching { repository.addEdge(projectId, sourceItemId, targetItemId, relation, description, sinceChapter) }
                .onSuccess { message.value = "关系已加入知识图谱" }
                .onFailure { message.value = it.message ?: "关系保存失败" }
        }
    }

    private suspend fun applyMemoryExtraction(chapter: Chapter, extraction: MemoryExtraction): String {
        val knownByName = storyItems.value.associateBy { it.name }.toMutableMap()
        val mentionedItemIds = mutableSetOf<Long>()
        var addedItems = 0
        extraction.items.distinctBy { it.name }.forEach { candidate ->
            val existing = knownByName[candidate.name]
            if (existing == null) {
                val id = repository.addStoryItem(chapter.projectId, candidate.kind, candidate.name, candidate.detail, candidate.status)
                knownByName[candidate.name] = StoryItem(
                    id = id,
                    projectId = chapter.projectId,
                    kind = candidate.kind,
                    name = candidate.name,
                    detail = candidate.detail,
                    status = candidate.status,
                )
                addedItems += 1
            } else if (candidate.detail.isNotBlank() && (existing.detail != candidate.detail || existing.status != candidate.status)) {
                repository.updateStoryItem(existing, candidate.kind, candidate.name, candidate.detail, candidate.status)
            }
            knownByName[candidate.name]?.let { mentionedItemIds += it.id }
        }
        val knownEdges = edges.value.map { Triple(it.sourceItemId, it.targetItemId, it.relation) }.toMutableSet()
        var addedEdges = 0
        extraction.edges.forEach { candidate ->
            val source = knownByName[candidate.sourceName] ?: return@forEach
            val target = knownByName[candidate.targetName] ?: return@forEach
            mentionedItemIds += source.id
            mentionedItemIds += target.id
            if (source.id != target.id && knownEdges.add(Triple(source.id, target.id, candidate.relation))) {
                repository.addEdge(chapter.projectId, source.id, target.id, candidate.relation, candidate.description, chapter.number)
                addedEdges += 1
            }
        }
        repository.replaceChapterMentions(chapter, mentionedItemIds)
        return "记忆已更新：新增 $addedItems 条资料、$addedEdges 条关系、${mentionedItemIds.size} 条章节引用"
    }

    private suspend fun runMandatoryGateStages(chapter: Chapter, memoryMessage: String): MandatoryGateResult {
        val project = selectedProject.value?.takeIf { it.id == chapter.projectId }
            ?: return MandatoryGateResult(false, "作品已切换，稍后会重试闭环", emptyList())
        val snapshot = ContextEngine.build(
            project, chapter, chapters.value, storyItems.value, anchors.value, edges.value,
            chapterMentions.value, researchNotes.value, ragChunks.value,
        ).prompt.take(12_000)
        val localIssues = QualityGate.inspect(chapter, storyItems.value, anchors.value, project) +
            PacingPlanner.warnings(project, chapter, pacingEvents.value, eventMatrixRules.value)
        val blockingLocalIssues = localIssues.filter(::isBlockingLocalIssue)
        val localText = when {
            blockingLocalIssues.isNotEmpty() -> "FAIL: [P0] " + blockingLocalIssues.joinToString("；") { "${it.title}: ${it.detail}" }
            localIssues.any { it.severity == QualitySeverity.WARNING } -> "WARN: " + localIssues.joinToString("；") { "${it.title}: ${it.detail}" }
            else -> "PASS: 本地一致性、节奏和 AI 痕迹检查通过"
        }
        val reports = listOf(
            "记忆更新" to memoryMessage,
            "本地一致性与节奏" to localText,
        )
        reports.forEach { (stage, content) ->
            val passed = !blocksChapterLifecycle(stage, content)
            repository.addGateReport(ChapterGateReport(projectId = project.id, chapterId = chapter.id, stage = stage, passed = passed, content = content, contextSnapshot = snapshot))
        }
        val failed = reports.filter { (stage, content) -> blocksChapterLifecycle(stage, content) }
        return MandatoryGateResult(
            passed = failed.isEmpty(),
            summary = if (failed.isEmpty()) "记忆与本地一致性检查通过；文风、审稿和校对可在审核页按需执行" else failed.joinToString("；") { it.first },
            reports = reports,
        )
    }

    private suspend fun runChapterLifecycle(chapter: Chapter, request: GenerationRequest): ChapterLifecycleResult {
        repository.updateChapterLifecycle(
            chapter = chapter,
            lifecycleStatus = ChapterLifecycleStatus.PROCESSING,
            lifecycleDetail = "正在提取本章已发生的角色、事件、伏笔和关系",
        )
        val input = "第${chapter.number}章 ${chapter.title}\n${chapter.content}"
        return modelClient.extractStoryMemory(modelConfig.value, input, request).fold(
            onSuccess = { raw ->
                if (!repository.hasUnchangedContent(chapter)) {
                    repository.updateChapterLifecycle(
                        chapter = chapter,
                        lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                        lifecycleDetail = "正文已在处理期间修改，请重新运行章节闭环",
                    )
                    return@fold ChapterLifecycleResult(false, "正文已修改，请重新运行章节闭环")
                }
                runCatching { MemoryExtractionParser.parse(raw) }.fold(
                    onSuccess = { extraction ->
                        val memoryMessage = applyMemoryExtraction(chapter, extraction)
                        val gate = runMandatoryGateStages(chapter, memoryMessage)
                        repository.recordGateOutcome(chapter, gate.passed, gate.summary, if (gate.passed) "" else gate.summary)
                        ChapterLifecycleResult(gate.passed, gate.summary)
                    },
                    onFailure = {
                        repository.updateChapterLifecycle(
                            chapter = chapter,
                            lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                            lifecycleDetail = "记忆解析失败：${it.message ?: "模型返回格式无效"}",
                        )
                        ChapterLifecycleResult(false, "记忆同步失败，可在审阅页重试")
                    },
                )
            },
            onFailure = {
                repository.updateChapterLifecycle(
                    chapter = chapter,
                    lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                    lifecycleDetail = "记忆同步失败：${it.message ?: "模型请求失败"}",
                )
                ChapterLifecycleResult(false, "记忆同步失败，可在审阅页重试")
            },
        )
    }

    private fun queueChapterLifecycle(chapter: Chapter) {
        viewModelScope.launch {
            repository.enqueueChapterLifecycle(chapter)
            processLifecycleQueue()
        }
    }

    private fun processLifecycleQueue() {
        if (lifecycleQueueRunner?.isActive == true) return
        val projectId = selectedProjectId.value ?: return
        val config = modelConfig.value
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) return
        val request = beginGeneration(GenerationTask.CHAPTER_LIFECYCLE) ?: return
        lifecycleQueueRunner = viewModelScope.launch {
            try {
                while (isActive) {
                    val queued = repository.nextQueuedChapterLifecycle(projectId) ?: break
                    val job = repository.claimChapterLifecycle(queued) ?: continue
                    activeLifecycleJob = job
                    val chapter = repository.chapterById(job.chapterId)
                    if (chapter == null) {
                        repository.finishChapterLifecycle(job, passed = false, detail = "章节已删除")
                        activeLifecycleJob = null
                        continue
                    }
                    if (chapter.contentFingerprint() != job.contentFingerprint) {
                        repository.enqueueChapterLifecycle(chapter)
                        activeLifecycleJob = null
                        continue
                    }
                    val result = runChapterLifecycle(chapter, request)
                    if (result.message == "作品已切换，稍后会重试闭环") {
                        repository.requeueChapterLifecycle(job, result.message)
                        activeLifecycleJob = null
                        break
                    }
                    val finish = repository.finishChapterLifecycle(job, result.passed, result.message)
                    if (finish.saved && !result.passed) {
                        message.value = "第${chapter.number}章已保存；后台闭环未通过：${result.message}。可在审核页重试。"
                    }
                    if (finish.saved && result.passed && finish.nextChapterId != null) {
                        selectedChapterId.value = finish.nextChapterId
                        val nextChapter = repository.chapterById(finish.nextChapterId)
                        if (nextChapter != null && finish.nextChapterAction == NextChapterAction.GENERATE_WITH_AI) {
                            val project = selectedProject.value?.takeIf { it.id == nextChapter.projectId }
                            if (project != null) startContinuation(project, nextChapter, "")
                        } else if (nextChapter != null) {
                            message.value = "第${chapter.number}章闭环通过，已新建第${nextChapter.number}章"
                        }
                    }
                    activeLifecycleJob = null
                }
            } catch (cancelled: CancellationException) {
                activeLifecycleJob?.let { repository.requeueChapterLifecycle(it, "已暂停，可稍后继续") }
                throw cancelled
            } finally {
                activeLifecycleJob = null
                lifecycleQueueRunner = null
                finishGeneration(GenerationTask.CHAPTER_LIFECYCLE, request)
            }
        }
        generationJobs[GenerationTask.CHAPTER_LIFECYCLE] = lifecycleQueueRunner!!
    }

    private suspend fun persistContinuitySnapshot(
        project: NovelProject,
        chapter: Chapter,
        knownChapters: List<Chapter>,
    ): ChapterContinuitySnapshot {
        val predecessor = knownChapters.filter { it.number < chapter.number && it.content.isNotBlank() }.maxByOrNull { it.number }
        val packet = ContextEngine.build(
            project = project,
            current = chapter,
            chapters = knownChapters,
            storyItems = storyItems.value,
            anchors = anchors.value,
            edges = edges.value,
            mentions = chapterMentions.value,
            researchNotes = researchNotes.value,
            ragChunks = ragChunks.value,
        )
        val snapshot = ChapterContinuitySnapshot(
            chapterId = chapter.id,
            projectId = project.id,
            predecessorChapterId = predecessor?.id ?: 0,
            predecessorTail = predecessor?.content?.takeLast(2_200).orEmpty(),
            contextPrompt = packet.prompt,
            confirmationStatus = if (predecessor == null || predecessor.lifecycleStatus == ChapterLifecycleStatus.PASSED) {
                ContinuitySnapshotStatus.CONFIRMED
            } else {
                ContinuitySnapshotStatus.PENDING
            },
        )
        repository.saveContinuitySnapshot(snapshot)
        return snapshot
    }

    private fun Chapter.contentFingerprint(): String = "${content.length}:${content.hashCode()}"

    fun extractMemoryFromCurrentChapter() {
        val chapter = selectedChapter.value ?: return
        val content = pendingChapterContent[chapter.id] ?: chapter.content
        if (content.isBlank()) {
            message.value = "本章还没有正文，无法提取记忆"
            return
        }
        val request = beginGeneration(GenerationTask.MEMORY_EXTRACTION) ?: return
        message.value = "正在从本章提取知识图谱..."
        val extractionInput = "第${chapter.number}章 ${chapter.title}\n$content"
        generationJobs[GenerationTask.MEMORY_EXTRACTION] = viewModelScope.launch {
            try {
                val result = modelClient.extractStoryMemory(modelConfig.value, extractionInput, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { raw ->
                        runCatching { MemoryExtractionParser.parse(raw) }.onSuccess { extraction ->
                            val knownByName = storyItems.value.associateBy { it.name }.toMutableMap()
                            var addedItems = 0
                            extraction.items.distinctBy { it.name }.forEach { candidate ->
                                val existing = knownByName[candidate.name]
                                if (existing == null) {
                                    val id = repository.addStoryItem(
                                        chapter.projectId,
                                        candidate.kind,
                                        candidate.name,
                                        candidate.detail,
                                        candidate.status,
                                    )
                                    knownByName[candidate.name] = StoryItem(
                                        id = id,
                                        projectId = chapter.projectId,
                                        kind = candidate.kind,
                                        name = candidate.name,
                                        detail = candidate.detail,
                                        status = candidate.status,
                                    )
                                    addedItems += 1
                                } else if (candidate.detail.isNotBlank() && (existing.detail != candidate.detail || existing.status != candidate.status)) {
                                    repository.updateStoryItem(existing, candidate.kind, candidate.name, candidate.detail, candidate.status)
                                }
                            }
                            val existingEdges = edges.value.map { Triple(it.sourceItemId, it.targetItemId, it.relation) }.toMutableSet()
                            var addedEdges = 0
                            extraction.edges.forEach { candidate ->
                                val source = knownByName[candidate.sourceName] ?: return@forEach
                                val target = knownByName[candidate.targetName] ?: return@forEach
                                val key = Triple(source.id, target.id, candidate.relation)
                                if (source.id != target.id && existingEdges.add(key)) {
                                    repository.addEdge(chapter.projectId, source.id, target.id, candidate.relation, candidate.description, chapter.number)
                                    addedEdges += 1
                                }
                            }
                            message.value = "记忆已更新：新增 $addedItems 条资料、$addedEdges 条关系"
                        }.onFailure {
                            message.value = "模型返回的图谱格式无效，请重试"
                        }
                    },
                    onFailure = { message.value = it.message ?: "提取记忆失败" },
                )
            } finally {
                finishGeneration(GenerationTask.MEMORY_EXTRACTION, request)
            }
        }
    }

    fun generateRepairPlan() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val issues = QualityGate.inspect(chapter, storyItems.value, anchors.value, project)
        val request = beginGeneration(GenerationTask.REPAIR_PLAN) ?: return
        message.value = "正在生成最短修复计划..."
        val context = buildString {
            append(ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value, ragChunks = ragChunks.value).prompt)
            appendLine("\n门禁问题：")
            issues.forEach { appendLine("- ${it.title}：${it.detail}") }
        }
        generationJobs[GenerationTask.REPAIR_PLAN] = viewModelScope.launch {
            try {
                val result = modelClient.generateRepairPlan(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { plan ->
                        repairPlan.value = plan
                        message.value = "修复计划已生成，正文未自动修改"
                    },
                    onFailure = { message.value = it.message ?: "生成修复计划失败" },
                )
            } finally {
                finishGeneration(GenerationTask.REPAIR_PLAN, request)
            }
        }
    }

    fun generateChapterPlan() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val request = beginGeneration(GenerationTask.CHAPTER_PLAN) ?: return
        message.value = "正在根据本地记忆生成本章计划..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value, ragChunks = ragChunks.value).prompt +
            "\n请为当前章节生成大纲，覆盖冲突、转折和结尾钩子。"
        generationJobs[GenerationTask.CHAPTER_PLAN] = viewModelScope.launch {
            try {
                val result = modelClient.generateChapterPlan(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { outline ->
                        repository.updateChapterPlan(chapter, outline, chapter.targetWordCount)
                        message.value = "本章计划已保存，可继续手动调整"
                    },
                    onFailure = { message.value = it.message ?: "生成大纲失败" },
                )
            } finally {
                finishGeneration(GenerationTask.CHAPTER_PLAN, request)
            }
        }
    }

    fun generateBeatSheet() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val request = beginGeneration(GenerationTask.BEAT_SHEET) ?: return
        message.value = "正在生成本章分镜..."
        val context = ContextEngine.build(project, chapter, chapters.value, storyItems.value, anchors.value, researchNotes = researchNotes.value, ragChunks = ragChunks.value).prompt +
            "\n请把本章计划拆成按顺序执行的场景分镜。"
        generationJobs[GenerationTask.BEAT_SHEET] = viewModelScope.launch {
            try {
                val result = modelClient.generateBeatSheet(modelConfig.value, context, request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { beatSheet ->
                        repository.updateChapterBeatSheet(chapter, beatSheet)
                        message.value = "本章分镜已保存，可继续编辑"
                    },
                    onFailure = { message.value = it.message ?: "生成分镜失败" },
                )
            } finally {
                finishGeneration(GenerationTask.BEAT_SHEET, request)
            }
        }
    }

    fun extractStyleGuideFromCurrentChapter() {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val sample = pendingChapterContent[chapter.id] ?: chapter.content
        if (sample.length < 200) {
            message.value = "样章至少需要 200 字才能提取文风"
            return
        }
        val request = beginGeneration(GenerationTask.STYLE_GUIDE) ?: return
        message.value = "正在提取项目文风..."
        generationJobs[GenerationTask.STYLE_GUIDE] = viewModelScope.launch {
            try {
                val result = modelClient.extractStyleGuide(modelConfig.value, sample.take(8_000), request)
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { styleGuide ->
                        repository.updateProjectStyle(project, styleGuide)
                        message.value = "项目文风档案已保存，可继续调整"
                    },
                    onFailure = { message.value = it.message ?: "提取文风失败" },
                )
            } finally {
                finishGeneration(GenerationTask.STYLE_GUIDE, request)
            }
        }
    }

    fun saveModelConfig(config: ModelConfig) {
        modelPreferences.save(config)
        modelConfig.value = config
        message.value = "模型配置已加密保存到本机"
        processLifecycleQueue()
    }

    fun testModelConfig(config: ModelConfig) {
        viewModelScope.launch {
            message.value = "正在测试连接..."
            modelClient.test(config).fold(
                onSuccess = { message.value = it },
                onFailure = { message.value = it.message ?: "连接失败" },
            )
        }
    }

    fun generateContinuation(direction: String = "") {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        if (hasPendingCascade()) { message.value = "改纲待审项尚未复核，暂不能继续写作"; return }
        val sourceContent = pendingChapterContent[chapter.id] ?: chapter.content
        val sourceChapter = chapter.copy(content = sourceContent)
        startContinuation(project, sourceChapter, direction)
    }

    private fun startContinuation(project: NovelProject, chapter: Chapter, direction: String) {
        if (!modelConfig.value.hasTextGenerationConfiguration()) {
            message.value = "请先在“我的”配置文本创作模型"
            return
        }
        val request = beginGeneration(GenerationTask.CONTINUATION) ?: return
        streamedContinuation.value = ""
        message.value = "正在直接请求你的模型..."
        generationJobs[GenerationTask.CONTINUATION] = viewModelScope.launch {
            try {
                message.value = "正在生成本章计划与分镜..."
                val preparedChapter = prepareChapterForWriting(project, chapter, chapters.value, request).getOrElse {
                    message.value = "章节计划或分镜准备失败：${it.message ?: "模型请求失败"}"
                    return@launch
                }
                if (preparedChapter.outline != chapter.outline) {
                    repository.updateChapterPlan(chapter, preparedChapter.outline, chapter.targetWordCount)
                }
                if (preparedChapter.beatSheet != chapter.beatSheet) {
                    repository.updateChapterBeatSheet(preparedChapter, preparedChapter.beatSheet)
                }
                val continuitySnapshot = persistContinuitySnapshot(project, preparedChapter, chapters.value)
                val writingContext = buildString {
                    append(continuitySnapshot.contextPrompt)
                    appendLine("\n作者本次续写方向：${direction.trim().ifBlank { "根据当前章节未解决冲突自然推进，保留悬念。" }}")
                    if (project.forbiddenContent.isNotBlank()) appendLine("项目核心禁区（必须遵守）：${project.forbiddenContent}")
                    val wordRange = normalizeChapterWordRange(project.targetChapterWordCount, project.targetChapterWordCountMax)
                    appendLine("本章目标字数：${wordRange.min}-${wordRange.max} 字。自动化等级：${project.automationLevel}。")
                    appendLine("本章入口角度（与前章轮换）：${ChapterEntryAngles.forChapter(chapter.number)}")
                }
                message.value = "正在根据计划续写正文..."
                val result = modelClient.continueWriting(modelConfig.value, writingContext, request) { delta ->
                    if (selectedChapter.value?.id == chapter.id) streamedContinuation.value = sanitizeNovelBody(streamedContinuation.value + delta)
                }
                if (!isActive) return@launch
                result.fold(
                    onSuccess = { generated ->
                        // Preserve text typed while the model was generating; AI output only appends.
                        saveChapterJobs.remove(chapter.id)?.cancel()
                        val currentChapter = chapters.value.firstOrNull { it.id == chapter.id }?.copy(
                            outline = preparedChapter.outline,
                            beatSheet = preparedChapter.beatSheet,
                        ) ?: preparedChapter
                        val currentContent = pendingChapterContent.remove(chapter.id) ?: currentChapter.content
                        val body = sanitizeNovelBody(generated)
                        if (body.isBlank()) {
                            message.value = "模型没有返回可用正文，请重试"
                            return@fold
                        }
                        val title = if (shouldGenerateChapterTitle(currentChapter.title, currentContent)) {
                            val fallbackTitle = "第${currentChapter.number}章：新的篇章"
                            modelClient.generateChapterTitle(modelConfig.value, body, request)
                                .getOrDefault(fallbackTitle)
                                .let { chapterTitleOrFallback(it, fallbackTitle) }
                        } else {
                            currentChapter.title
                        }
                        val updatedChapter = currentChapter.copy(
                            title = title,
                            content = appendGeneratedChapterContent(currentContent, body),
                        )
                        repository.updateChapter(updatedChapter, updatedChapter.content)
                        streamedContinuation.value = ""
                        queueChapterLifecycle(updatedChapter)
                        message.value = "已续写到草稿；章节闭环正在后台处理，不会覆盖当前正文"
                    },
                    onFailure = { message.value = it.message ?: "续写失败" },
                )
            } finally {
                streamedContinuation.value = ""
                finishGeneration(GenerationTask.CONTINUATION, request)
            }
        }
    }

    private suspend fun applyAutomaticHumanization(project: NovelProject, chapter: Chapter, request: GenerationRequest): Chapter {
        if (project.automationLevel != "自动推进" || chapter.content.isBlank()) return chapter
        val first = modelClient.humanizeChapter(
            modelConfig.value,
            "自动推进模式第一遍去 AI 化润色。只输出完整正文，不改变剧情事实、人物关系、视角或篇幅。\n\n正文：\n${chapter.content}",
            request,
        ).getOrNull() ?: return chapter
        val second = modelClient.humanizeChapter(
            modelConfig.value,
            "自动推进模式第二遍去 AI 化校对。只处理残留机械重复、模板化衔接和同质句式；只输出完整正文。\n\n正文：\n$first",
            request,
        ).getOrNull() ?: return chapter
        return repository.replaceChapterWithRevision(chapter, second, "自动推进双遍去 AI 润色")
    }

    /** The same plan -> beat -> prose pipeline is used for continuation and unattended batches. */
    private suspend fun prepareChapterForWriting(
        project: NovelProject,
        chapter: Chapter,
        knownChapters: List<Chapter>,
        request: GenerationRequest,
    ): Result<Chapter> = runCatching {
        var prepared = chapter
        if (prepared.outline.isBlank()) {
            val planContext = ContextEngine.build(
                project, prepared, knownChapters, storyItems.value, anchors.value,
                researchNotes = researchNotes.value, ragChunks = ragChunks.value,
            ).prompt + "\n请生成当前章节计划，必须包含冲突、转折、结尾钩子，并遵守所有锚点与禁区。"
            val outline = modelClient.generateChapterPlan(modelConfig.value, planContext, request).getOrThrow()
            prepared = prepared.copy(outline = outline)
        }
        if (prepared.beatSheet.isBlank()) {
            val beatContext = ContextEngine.build(
                project, prepared, knownChapters, storyItems.value, anchors.value,
                researchNotes = researchNotes.value, ragChunks = ragChunks.value,
            ).prompt + "\n请把当前章节计划拆成依序执行的场景分镜。每个分镜必须服务于章节目标，并保留结尾钩子。"
            val beatSheet = modelClient.generateBeatSheet(modelConfig.value, beatContext, request).getOrThrow()
            prepared = prepared.copy(beatSheet = beatSheet)
        }
        prepared
    }

    private fun startAutoWrite(project: NovelProject, run: AutoWriteRun) {
        blockingLifecycleChapter()?.let {
            message.value = "第${it.number}章尚未完成写作闭环，不能开始批量写作"
            return
        }
        if (hasPendingCascade()) {
            message.value = "改纲待审项尚未复核，暂不能批量写作"
            return
        }
        val request = beginGeneration(GenerationTask.AUTO_WRITE) ?: run {
            viewModelScope.launch {
                repository.updateAutoWriteRun(run, run.completedCount, AutoWriteRunStatus.PAUSED, "等待其他 AI 任务结束")
            }
            return
        }
        generationJobs[GenerationTask.AUTO_WRITE] = viewModelScope.launch {
            var currentRun = repository.updateAutoWriteRun(run, run.completedCount, AutoWriteRunStatus.RUNNING, "正在准备第${run.completedCount + 1}章")
            try {
                val workingChapters = chapters.value.toMutableList()
                repeat(currentRun.requestedCount - currentRun.completedCount) {
                    val nextNumber = (workingChapters.maxOfOrNull { it.number } ?: 0) + 1
                    val target = Chapter(projectId = project.id, number = nextNumber, title = "第${nextNumber}章")
                    currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.RUNNING, "正在规划第${nextNumber}章")
                    val preparedTarget = prepareChapterForWriting(project, target, workingChapters, request).getOrElse {
                        currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.PAUSED, "第${nextNumber}章规划失败：${it.message ?: "模型请求失败"}")
                        message.value = "批量写作已暂停：${currentRun.detail}"
                        return@launch
                    }
                    val context = ContextEngine.build(project, preparedTarget, workingChapters, storyItems.value, anchors.value, edges.value, researchNotes = researchNotes.value, ragChunks = ragChunks.value).prompt +
                        "\n这是可恢复批量写作的第${currentRun.completedCount + 1}/${currentRun.requestedCount}章。请完整写出本章。\n本章入口角度（与前章轮换）：${ChapterEntryAngles.forChapter(nextNumber)}"
                    val body = modelClient.writeFullChapter(modelConfig.value, context, request).getOrElse {
                        currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.PAUSED, it.message ?: "模型请求失败")
                        message.value = "批量写作已暂停：${currentRun.detail}"
                        return@launch
                    }
                    val fallbackTitle = "第${nextNumber}章：新的篇章"
                    val title = modelClient.generateChapterTitle(modelConfig.value, body, request)
                        .getOrDefault(fallbackTitle)
                        .let { chapterTitleOrFallback(it, fallbackTitle) }
                    val completed = repository.addGeneratedDraftChapter(
                        projectId = project.id,
                        content = sanitizeNovelBody(body),
                        title = title,
                        autoWriteRunId = currentRun.id,
                        outline = preparedTarget.outline,
                        beatSheet = preparedTarget.beatSheet,
                    )
                    workingChapters += completed
                    selectedChapterId.value = completed.id
                    val lifecycle = runChapterLifecycle(applyAutomaticHumanization(project, completed, request), request)
                    if (!lifecycle.passed) {
                        currentRun = repository.updateAutoWriteRun(currentRun, currentRun.completedCount + 1, AutoWriteRunStatus.PAUSED, "第${completed.number}章：${lifecycle.message}")
                        message.value = "第${completed.number}章待处理，批量写作已暂停"
                        return@launch
                    }
                    currentRun = repository.updateAutoWriteRun(
                        currentRun,
                        currentRun.completedCount + 1,
                        AutoWriteRunStatus.RUNNING,
                        "第${completed.number}章已通过写作闭环",
                    )
                    message.value = "第${completed.number}章已通过写作闭环（${currentRun.completedCount}/${currentRun.requestedCount}）"
                }
                repository.updateAutoWriteRun(currentRun, currentRun.requestedCount, AutoWriteRunStatus.COMPLETED, "全部章节已通过写作闭环")
                message.value = "批量写作完成，共生成 ${currentRun.requestedCount} 章"
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    repository.updateAutoWriteRun(currentRun, currentRun.completedCount, AutoWriteRunStatus.PAUSED, "作者已暂停，可在处理当前章节后继续")
                }
                throw cancelled
            } finally {
                finishGeneration(GenerationTask.AUTO_WRITE, request)
            }
        }
    }

    fun resumeAutoWrite() {
        val project = selectedProject.value ?: return
        val run = resumableAutoWriteRun.value ?: run {
            message.value = "没有可继续的批量写作计划"
            return
        }
        startAutoWrite(project, run)
    }

    fun autoWriteChapters(count: Int) {
        val project = selectedProject.value ?: return
        blockingLifecycleChapter()?.let {
            message.value = "第${it.number}章尚未完成写作闭环，不能开始批量写作"
            return
        }
        if (hasPendingCascade()) {
            message.value = "改纲待审项尚未复核，暂不能批量写作"
            return
        }
        if (autoWritePreparing) {
            message.value = "正在创建批量写作计划"
            return
        }
        val total = count.coerceIn(1, 5)
        autoWritePreparing = true
        viewModelScope.launch {
            try {
                val run = repository.createAutoWriteRun(project.id, total)
                startAutoWrite(project, run)
            } finally {
                autoWritePreparing = false
            }
        }
    }

    fun reviseOutline(fromChapter: Int, description: String) {
        val project = selectedProject.value ?: return
        outlineCascadePending.value = true
        generationTasks.value.toList().forEach(::cancelGeneration)
        val report = OutlineCascadeAnalyzer.analyze(fromChapter, chapters.value, storyItems.value, anchors.value, edges.value, description)
        viewModelScope.launch {
            repository.applyOutlineCascade(project, report, storyItems.value, anchors.value, edges.value)
            message.value = report.summary + " 已标记为待审，请在资料和大纲页逐项确认。"
        }
    }

    fun resolveOutlineCascade() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            repository.resolveOutlineCascade(project, storyItems.value, anchors.value, edges.value)
            outlineCascadePending.value = false
            message.value = "已确认全部改纲待审项，可恢复 AI 写作"
        }
    }

    private fun hasPendingCascade(): Boolean = outlineCascadePending.value || selectedProject.value?.outlineRevisionReport?.isNotBlank() == true || storyItems.value.any { it.cascadePending } || anchors.value.any { it.cascadePending } || edges.value.any { it.cascadePending }

    private fun blockingLifecycleChapter(): Chapter? = chapters.value
        .sortedByDescending { it.number }
        .firstOrNull { ChapterLifecycleStatus.blocksAutomaticWriting(it.lifecycleStatus) }

    fun markCurrentChapterQualityRepaired() {
        retryCurrentChapterLifecycle()
    }

    fun retryCurrentChapterLifecycle() {
        val chapter = selectedChapter.value ?: return
        if (chapter.content.isBlank()) {
            message.value = "本章还没有正文，无法运行写作闭环"
            return
        }
        queueChapterLifecycle(chapter)
        message.value = "已加入章节闭环队列，将在后台重试"
    }

    private fun rewriteCurrentChapter(task: GenerationTask, humanize: Boolean) {
        val project = selectedProject.value ?: return
        val chapter = selectedChapter.value ?: return
        val content = pendingChapterContent[chapter.id] ?: chapter.content
        if (content.isBlank()) {
            message.value = "本章还没有正文，无法改写"
            return
        }
        val request = beginGeneration(task) ?: return
        message.value = if (humanize) "正在润色本章语言..." else "正在按门禁问题改写本章..."
        generationJobs[task] = viewModelScope.launch {
            try {
                saveChapterJobs.remove(chapter.id)?.cancel()
                pendingChapterContent.remove(chapter.id)
                val source = chapter.copy(content = content)
                if (source.content != chapter.content) repository.updateChapter(chapter, source.content)
                val context = buildString {
                    append(ContextEngine.build(project, source, chapters.value, storyItems.value, anchors.value, edges.value, researchNotes = researchNotes.value, ragChunks = ragChunks.value).prompt)
                    appendLine("\n当前完整正文：")
                    appendLine(source.content)
                    if (!humanize) {
                        appendLine("\n需要处理的门禁问题：")
                        QualityGate.inspect(source, storyItems.value, anchors.value, project).forEach { appendLine("- ${it.title}：${it.detail}") }
                    }
                }
                val result = if (humanize) {
                    modelClient.humanizeChapter(modelConfig.value, context, request).fold(
                        onSuccess = { firstPass ->
                            modelClient.humanizeChapter(
                                modelConfig.value,
                                "这是第一遍润色后的正文。执行第二遍去 AI 化校对：只消除残留的机械重复、模板化衔接和同质句式，不改变剧情事实、人物关系、视角或篇幅。\n\n正文：\n$firstPass",
                                request,
                            )
                        },
                        onFailure = { Result.failure(it) },
                    )
                } else modelClient.rewriteChapter(modelConfig.value, context, request)
                result.fold(
                    onSuccess = { replacement ->
                        val updated = repository.replaceChapterWithRevision(
                            source,
                            replacement,
                            if (humanize) "双遍去 AI 润色" else "按门禁问题 AI 改写",
                        )
                        val lifecycle = runChapterLifecycle(updated, request)
                        message.value = if (lifecycle.passed) "AI 改写已复检通过，可在审阅页撤回" else "AI 改写已保存：${lifecycle.message}"
                    },
                    onFailure = { message.value = it.message ?: "AI 改写失败" },
                )
            } finally {
                finishGeneration(task, request)
            }
        }
    }

    fun rewriteCurrentChapterForGate() = rewriteCurrentChapter(GenerationTask.CHAPTER_REWRITE, humanize = false)

    fun humanizeCurrentChapter() = rewriteCurrentChapter(GenerationTask.HUMANIZE, humanize = true)

    fun restoreLatestRevision() {
        val chapter = selectedChapter.value ?: return
        val revision = latestRevision.value ?: return
        viewModelScope.launch {
            repository.restoreRevision(chapter, revision)
            message.value = "已撤回 AI 改写，恢复此前正文"
        }
    }

    fun cancelGeneration(task: GenerationTask) {
        if (task !in generationTasks.value) return
        generationRequests[task]?.cancel()
        generationJobs[task]?.cancel()
        if (task == GenerationTask.CONTINUATION) streamedContinuation.value = ""
        if (task == GenerationTask.CHAPTER_LIFECYCLE) activeLifecycleJob?.let { job ->
            viewModelScope.launch { repository.requeueChapterLifecycle(job, "作者已暂停，可稍后继续") }
        } else selectedChapter.value?.takeIf { it.lifecycleStatus == ChapterLifecycleStatus.PROCESSING }?.let { chapter ->
            viewModelScope.launch {
                repository.updateChapterLifecycle(
                    chapter = chapter,
                    lifecycleStatus = ChapterLifecycleStatus.MEMORY_FAILED,
                    lifecycleDetail = "作者已取消处理，可在审阅页重试",
                )
            }
        }
        finishGeneration(task, generationRequests[task] ?: return)
        message.value = "已取消${task.label}，其他任务继续运行"
    }

    fun clearMessage(expectedMessage: String? = null) {
        if (expectedMessage == null || message.value == expectedMessage) message.value = null
    }
}
